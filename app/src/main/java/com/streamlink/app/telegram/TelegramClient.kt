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
            sendRaw(TdApi.SetLogVerbosityLevel(0))
            sendRaw(TdApi.SetTdlibParameters(
                false,                              // use_test_dc
                filesDir.absolutePath,              // database_directory
                filesDir.absolutePath + "/files",   // files_directory
                null,                               // database_encryption_key
                true,                               // use_file_database
                true,                               // use_chat_info_database
                true,                               // use_message_database
                false,                              // use_secret_chats
                BuildConfig.TELEGRAM_API_ID,        // api_id
                BuildConfig.TELEGRAM_API_HASH,      // api_hash
                "he",                               // system_language_code
                "Android",                          // device_model
                "13",                               // system_version
                "1.0.0",                            // application_version
                true,                               // enable_storage_optimizer
                false                               // ignore_file_names
            ))
        }
    }

    // ---- Auth flow ----

    suspend fun sendPhoneNumber(phone: String) {
        sendRaw(TdApi.SetAuthenticationPhoneNumber(
            phone,
            TdApi.PhoneNumberAuthenticationSettings(false, false, false, false, null)
        ))
    }

    suspend fun sendCode(code: String) {
        sendRaw(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun sendPassword(password: String) {
        sendRaw(TdApi.CheckAuthenticationPassword(password))
    }

    // ---- Bot interaction ----

    suspend fun sendMessageToBot(botUsername: String, text: String): Long {
        val chatId = getBotChatId(botUsername)
        val result = sendRequest(TdApi.SendMessage(
            chatId,
            0, 0,
            null, null,
            TdApi.InputMessageText(
                TdApi.FormattedText(text, emptyArray()),
                false, false
            )
        ))
        return if (result is TdApi.Message) result.id else -1L
    }

    suspend fun pressInlineButton(chatId: Long, messageId: Long, callbackData: ByteArray) {
        sendRequest(TdApi.GetCallbackQueryAnswer(
            chatId, messageId,
            TdApi.CallbackQueryPayloadData(callbackData)
        ))
    }

    // ---- File download ----

    suspend fun downloadFile(fileId: Int, priority: Int = 1): String? {
        val result = sendRequest(TdApi.DownloadFile(fileId, priority, 0, 0, true))
        return if (result is TdApi.File && result.local.isDownloadingCompleted) {
            result.local.path
        } else null
    }

    fun cancelDownload(fileId: Int) {
        scope.launch { sendRaw(TdApi.CancelDownloadFile(fileId, false)) }
    }

    // ---- Internal ----

    private suspend fun getBotChatId(username: String): Long {
        botChatIds[username]?.let { return it }
        val result = sendRequest(TdApi.SearchPublicChat(username))
        val chatId = if (result is TdApi.Chat) result.id else error("Bot not found: $username")
        botChatIds[username] = chatId

        // Open chat so we can receive its messages
        sendRaw(TdApi.OpenChat(chatId))
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
                is TdApi.MessageText  -> c.text.text
                is TdApi.MessageDocument -> c.caption.text
                is TdApi.MessageVideo -> c.caption.text
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
        scope.launch { sendRaw(TdApi.Close()) }
    }
}
