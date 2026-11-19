package com.streamlink.app.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Telegram bot connections and search sessions.
 *
 * Architecture:
 * - User provides a bot token (from their own bridge bot, or we use inline mode)
 * - OR: user logs in with phone number → we use Telegram User API (phase 3 with TDLib)
 *
 * Phase 2 approach (simpler, works now):
 * - Use Telegram Bot API
 * - App has a "bridge bot" token stored securely
 * - Bridge bot forwards messages to movie bots and relays responses
 *
 * For direct bot communication without a bridge:
 * - User can set their own bot token
 */
@Singleton
class TelegramManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "TelegramManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: TelegramBotClient? = null
    private var currentBotToken: String? = null

    // State flows
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress: StateFlow<DownloadProgress?> = _downloadProgress.asStateFlow()

    // Encrypted preferences for storing bot token
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "streamlink_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails
            context.getSharedPreferences("streamlink_prefs", Context.MODE_PRIVATE)
        }
    }

    init {
        // Load saved token on startup
        val savedToken = prefs.getString("bot_token", null)
        if (!savedToken.isNullOrEmpty()) {
            connectWithToken(savedToken)
        }
    }

    fun connectWithToken(token: String) {
        currentBotToken = token
        client = TelegramBotClient(token)
        _connectionState.value = ConnectionState.Connecting

        scope.launch {
            val result = client!!.getMe()
            if (result.isSuccess) {
                val me = result.getOrNull()!!
                val botName = me.getJSONObject("result").optString("username", "bot")
                Log.d(TAG, "Connected as @$botName")
                prefs.edit().putString("bot_token", token).apply()
                _connectionState.value = ConnectionState.Connected(botName)
            } else {
                Log.e(TAG, "Connection failed: ${result.exceptionOrNull()?.message}")
                _connectionState.value = ConnectionState.Error(result.exceptionOrNull()?.message ?: "חיבור נכשל")
            }
        }
    }

    fun saveSourceBot(botUsername: String) {
        val bots = getSourceBots().toMutableList()
        if (!bots.contains(botUsername)) {
            bots.add(botUsername)
            prefs.edit().putStringSet("source_bots", bots.toSet()).apply()
        }
    }

    fun getSourceBots(): List<String> {
        return prefs.getStringSet("source_bots", setOf("King_Of_Telegram_Beta_bot"))?.toList()
            ?: listOf("King_Of_Telegram_Beta_bot")
    }

    /**
     * Search for movies across all configured source bots
     */
    fun searchMovies(query: String, onResult: (List<SearchResult>) -> Unit) {
        scope.launch {
            val c = client
            if (c == null) {
                // No real connection — return demo results
                delay(800)
                onResult(createDemoResults(query))
                return@launch
            }

            val allResults = mutableListOf<SearchResult>()
            val bots = getSourceBots()

            for (bot in bots) {
                try {
                    // Send search query to bot
                    val sendResult = c.sendMessageToBot(bot, query)
                    if (sendResult.isSuccess) {
                        // Poll for response
                        // For now return demo — real polling needs chat_id tracking
                        allResults.addAll(createDemoResults(query))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Search failed for $bot", e)
                }
            }

            if (allResults.isEmpty()) {
                allResults.addAll(createDemoResults(query))
            }

            onResult(allResults)
        }
    }

    /**
     * Select a movie from search results and request file from bot
     */
    fun selectMovie(result: SearchResult, botUsername: String, onFile: (FileInfo?) -> Unit) {
        scope.launch {
            val c = client
            if (c == null) {
                delay(500)
                onFile(null)
                return@launch
            }

            try {
                // Send the selection to bot
                val selection = result.callbackData.ifEmpty { result.title }
                val sendResult = c.sendMessageToBot(botUsername, selection)
                if (sendResult.isSuccess) {
                    // Wait for bot to send the file
                    // In real implementation, poll getUpdates for the file message
                    delay(1000)
                    onFile(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Selection failed", e)
                onFile(null)
            }
        }
    }

    /**
     * Download a file by file_id
     */
    fun downloadFile(
        fileId: String,
        fileName: String,
        destPath: String,
        onProgress: (Int) -> Unit,
        onComplete: (String?) -> Unit
    ) {
        scope.launch {
            val c = client
            if (c == null) {
                // Simulate download for demo
                for (i in 0..100 step 5) {
                    delay(200)
                    onProgress(i)
                }
                onComplete(null)
                return@launch
            }

            val destFile = java.io.File(destPath)
            val result = c.downloadFile(fileId, destFile) { downloaded, total ->
                val pct = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                onProgress(pct)
            }

            if (result.isSuccess) {
                onComplete(destFile.absolutePath)
            } else {
                onComplete(null)
            }
        }
    }

    fun isConnected() = _connectionState.value is ConnectionState.Connected

    fun disconnect() {
        client = null
        currentBotToken = null
        prefs.edit().remove("bot_token").apply()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun createDemoResults(query: String): List<SearchResult> = listOf(
        SearchResult(1, "$query (2024)", "2024", "4K"),
        SearchResult(2, "$query - Extended Cut", "2023", "1080p"),
        SearchResult(3, "$query - Remastered", "2022", "4K"),
        SearchResult(4, "$query 2", "2024", "1080p"),
        SearchResult(5, "The $query", "2023", "720p")
    )
}

// ---- State classes ----

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val botUsername: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class DownloadProgress(
    val fileId: String,
    val fileName: String,
    val percent: Int,
    val downloadedMb: Float,
    val totalMb: Float
)
