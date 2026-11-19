package com.streamlink.app.telegram

import android.content.Context
import android.util.Log
import com.streamlink.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

sealed class TelegramState {
    object WaitingForPhone  : TelegramState()
    object WaitingForCode   : TelegramState()
    object WaitingForPassword : TelegramState()
    object Ready            : TelegramState()
    object Closed           : TelegramState()
    data class Error(val message: String) : TelegramState()
}

data class BotMessage(
    val id: Long,
    val text: String?,
    val replyMarkup: TdApi.ReplyMarkup?
)

data class DownloadProgress(
    val fileId: Int,
    val downloaded: Long,
    val total: Long,
    val localPath: String?
)

@Singleton
class TelegramClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TelegramClient"
    }

    private var client: Client? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Public state flows
    private val _state = MutableStateFlow<TelegramState>(TelegramState.Closed)
    val state: StateFlow<TelegramState> = _state.asStateFlow()

    private val _botMessages = MutableSharedFlow<BotMessage>(replay = 0, extraBufferCapacity = 64)
    val botMessages: SharedFlow<BotMessage> = _botMessages.asSharedFlow()

    private val _downloadProgress = MutableSharedFlow<DownloadProgress>(replay = 0, extraBufferCapacity = 64)
    val downloadProgress: SharedFlow<DownloadProgress> = _downloadProgress.asSharedFlow()

    // Pending requests map: requestId -> Continuation
    private val pendingRequests = ConcurrentHashMap<Long, CompletableDeferred<TdApi.Object>>()
    private val requestId = AtomicLong(1)

    // Current bot chat id cache
    private val botChatIds = ConcurrentHashMap<String, Long>()

    // ---- Init ----

    fun init() {
        if (client != null) return
        Log.d(TAG, "Initializing TDLib")

        val filesDir = File(context.filesDir, "tdlib").also { it.mkdirs() }

        client = Client.create(
            { update -> handleUpdate(update) },
            null,
            null
        )

        scope.launch {
            // Use default constructor + field assignment for TDLib 1.8+ compatibility
            val logLevel = TdApi.SetLogVerbosityLevel()
            logLevel.newVerbosityLevel = 0
            sendRaw(logLevel)

            val params = TdApi.SetTdlibParameters()
            params.useTestDc = false
            params.databaseDirectory = filesDir.absolutePath
            params.filesDirectory = filesDir.absolutePath + "/files"
            params.databaseEncryptionKey = ByteArray(0)
            params.useFileDatabase = true
            params.useChatInfoDatabase = true
            params.useMessageDatabase = true
            params.useSecretChats = false
            params.apiId = BuildConfig.TELEGRAM_API_ID
            params.apiHash = BuildConfig.TELEGRAM_API_HASH
            params.systemLanguageCode = "he"
            params.deviceModel = "Android"
            params.systemVersion = "13"
            params.applicationVersion = "1.0.0"
            sendRaw(params)
        }
    }

    // ---- Auth flow ----

    suspend fun sendPhoneNumber(phone: String) {
        val req = TdApi.SetAuthenticationPhoneNumber()
        req.phoneNumber = phone
        req.settings = TdApi.PhoneNumberAuthenticationSettings()
        sendRaw(req)
    }

    suspend fun sendCode(code: String) {
        val req = TdApi.CheckAuthenticationCode()
        req.code = code
        sendRaw(req)
    }

    suspend fun sendPassword(password: String) {
        val req = TdApi.CheckAuthenticationPassword()
        req.password = password
        sendRaw(req)
    }

    // ---- Bot interaction ----

    suspend fun sendMessageToBot(botUsername: String, text: String): Long {
        val chatId = getBotChatId(botUsername)

        val formattedText = TdApi.FormattedText()
        formattedText.text = text
        formattedText.entities = emptyArray()

        val inputText = TdApi.InputMessageText()
        inputText.text = formattedText

        val req = TdApi.SendMessage()
        req.chatId = chatId
        req.inputMessageContent = inputText

        val result = sendRequest(req)
        return if (result is TdApi.Message) result.id else -1L
    }

    suspend fun pressInlineButton(chatId: Long, messageId: Long, callbackData: ByteArray) {
        val payload = TdApi.CallbackQueryPayloadData()
        payload.data = callbackData

        val req = TdApi.GetCallbackQueryAnswer()
        req.chatId = chatId
        req.messageId = messageId
        req.payload = payload

        sendRequest(req)
    }

    // ---- File download ----

    suspend fun downloadFile(fileId: Int, priority: Int = 1): String? {
        val req = TdApi.DownloadFile()
        req.fileId = fileId
        req.priority = priority
        req.offset = 0
        req.limit = 0
        req.synchronous = true

        val result = sendRequest(req)
        return if (result is TdApi.File && result.local.isDownloadingCompleted) {
            result.local.path
        } else null
    }

    fun cancelDownload(fileId: Int) {
        scope.launch {
            val req = TdApi.CancelDownloadFile()
            req.fileId = fileId
            req.onlyIfPending = false
            sendRaw(req)
        }
    }

    // ---- Internal ----

    private suspend fun getBotChatId(username: String): Long {
        botChatIds[username]?.let { return it }

        val req = TdApi.SearchPublicChat()
        req.username = username

        val result = sendRequest(req)
        val chatId = if (result is TdApi.Chat) result.id else error("Bot not found: $username")
        botChatIds[username] = chatId

        // Open chat so we can receive its messages
        val openReq = TdApi.OpenChat()
        openReq.chatId = chatId
        sendRaw(openReq)

        return chatId
    }

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
            is TdApi.UpdateNewMessage         -> handleNewMessage(update.message)
            is TdApi.UpdateFile               -> handleFileUpdate(update.file)
            else -> { /* ignore */ }
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        Log.d(TAG, "Auth state: ${state::class.simpleName}")
        when (state) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> _state.value = TelegramState.WaitingForPhone
            is TdApi.AuthorizationStateWaitCode        -> _state.value = TelegramState.WaitingForCode
            is TdApi.AuthorizationStateWaitPassword    -> _state.value = TelegramState.WaitingForPassword
            is TdApi.AuthorizationStateReady           -> _state.value = TelegramState.Ready
            is TdApi.AuthorizationStateClosed          -> _state.value = TelegramState.Closed
            else -> { /* other intermediate states */ }
        }
    }

    private fun handleNewMessage(msg: TdApi.Message) {
        scope.launch {
            val text = when (val c = msg.content) {
                is TdApi.MessageText     -> c.text.text
                is TdApi.MessageDocument -> c.caption.text
                is TdApi.MessageVideo    -> c.caption.text
                else -> null
            }
            _botMessages.emit(BotMessage(msg.id, text, msg.replyMarkup))
        }
    }

    private fun handleFileUpdate(file: TdApi.File) {
        scope.launch {
            _downloadProgress.emit(DownloadProgress(
                fileId     = file.id,
                downloaded = file.local.downloadedSize,
                total      = file.expectedSize,
                localPath  = if (file.local.isDownloadingCompleted) file.local.path else null
            ))
        }
    }

    // Send with result
    private suspend fun sendRequest(func: TdApi.Function<*>): TdApi.Object {
        val id = requestId.getAndIncrement()
        val deferred = CompletableDeferred<TdApi.Object>()
        pendingRequests[id] = deferred
        client?.send(func) { result ->
            pendingRequests.remove(id)?.complete(result)
        }
        return withTimeout(30_000) { deferred.await() }
    }

    // Send without waiting
    private fun sendRaw(func: TdApi.Function<*>) {
        client?.send(func) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "TDLib error: ${result.message}")
            }
        }
    }

    fun close() {
        val req = TdApi.Close()
        scope.launch { sendRaw(req) }
    }
}
