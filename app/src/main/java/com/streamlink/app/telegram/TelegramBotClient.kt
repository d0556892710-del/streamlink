package com.streamlink.app.telegram

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Telegram Bot API client.
 * Uses a "user bot" token approach — the app acts as a bot interacting with movie bots.
 *
 * Flow:
 * 1. User configures their bot token (from @BotFather) OR we use a bridge bot
 * 2. App sends messages to movie bot via sendMessage
 * 3. App polls getUpdates to receive bot responses (inline keyboards, files)
 * 4. App parses results and displays them
 * 5. User picks → app sends callback or message → bot sends file
 * 6. App downloads file via getFile + file download URL
 */
class TelegramBotClient(private val botToken: String) {

    private val baseUrl = "https://api.telegram.org/bot$botToken"
    private val TAG = "TelegramBot"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ---- API calls ----

    suspend fun getMe(): Result<JSONObject> = apiGet("getMe")

    suspend fun sendMessage(chatId: Long, text: String): Result<JSONObject> =
        apiPost("sendMessage", JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
        })

    suspend fun sendMessageToBot(botUsername: String, text: String): Result<JSONObject> {
        // First we need to get the bot's chat_id by starting a chat
        // For now use the bot username directly — Telegram resolves it
        return apiPost("sendMessage", JSONObject().apply {
            put("chat_id", "@${botUsername.removePrefix("@")}")
            put("text", text)
        })
    }

    suspend fun answerCallbackQuery(callbackQueryId: String): Result<JSONObject> =
        apiPost("answerCallbackQuery", JSONObject().apply {
            put("callback_query_id", callbackQueryId)
        })

    suspend fun sendCallbackQuery(chatId: Long, messageId: Long, data: String): Result<JSONObject> =
        apiPost("sendMessage", JSONObject().apply {
            // Simulate a callback by sending the text of the button
            put("chat_id", chatId)
            put("text", data)
        })

    /**
     * Poll for new updates (messages from bots after we send a search query)
     * Returns list of Update objects
     */
    suspend fun getUpdates(offset: Int = 0, timeout: Int = 10): Result<JSONArray> {
        val result = apiGet("getUpdates?offset=$offset&timeout=$timeout&allowed_updates=[\"message\",\"callback_query\"]")
        return result.map { it.getJSONArray("result") }
    }

    /**
     * Wait for bot response after sending a message
     * Polls until we get a message from the bot or timeout
     */
    suspend fun waitForBotResponse(
        botChatId: Long,
        afterUpdateId: Int,
        timeoutMs: Long = 15000
    ): BotMessage? = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var offset = afterUpdateId + 1

        while (System.currentTimeMillis() < deadline) {
            val result = getUpdates(offset = offset, timeout = 3)
            if (result.isSuccess) {
                val updates = result.getOrNull()!!
                for (i in 0 until updates.length()) {
                    val update = updates.getJSONObject(i)
                    offset = update.getInt("update_id") + 1

                    val message = update.optJSONObject("message") ?: continue
                    val from = message.optJSONObject("from") ?: continue
                    val fromId = from.getLong("id")

                    if (fromId == botChatId) {
                        return@withContext parseBotMessage(message)
                    }
                }
            }
            delay(500)
        }
        null
    }

    /**
     * Get file info then download URL
     */
    suspend fun getFileUrl(fileId: String): Result<String> {
        val result = apiGet("getFile?file_id=$fileId")
        return result.map { json ->
            val filePath = json.getJSONObject("result").getString("file_path")
            "https://api.telegram.org/file/bot$botToken/$filePath"
        }
    }

    /**
     * Download a file to local path with progress callback
     */
    suspend fun downloadFile(
        fileId: String,
        destFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val urlResult = getFileUrl(fileId)
            if (urlResult.isFailure) return@withContext Result.failure(urlResult.exceptionOrNull()!!)

            val url = urlResult.getOrNull()!!
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
            val total = body.contentLength()

            destFile.parentFile?.mkdirs()
            var downloaded = 0L

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        onProgress(downloaded, total)
                    }
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    // ---- Helpers ----

    private suspend fun apiGet(method: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = if (method.startsWith("http")) method else "$baseUrl/$method"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JSONObject(body)
            if (json.getBoolean("ok")) {
                Result.success(json)
            } else {
                Result.failure(Exception(json.optString("description", "API error")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "GET $method failed", e)
            Result.failure(e)
        }
    }

    private suspend fun apiPost(method: String, body: JSONObject): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = body.toString()
                    .toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/$method")
                    .post(requestBody)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(responseBody)
                if (json.getBoolean("ok")) {
                    Result.success(json)
                } else {
                    Result.failure(Exception(json.optString("description", "API error")))
                }
            } catch (e: Exception) {
                Log.e(TAG, "POST $method failed", e)
                Result.failure(e)
            }
        }

    private fun parseBotMessage(message: JSONObject): BotMessage {
        val messageId = message.getLong("message_id")
        val text = message.optString("text", "")
        val caption = message.optString("caption", "")

        // Parse inline keyboard
        val replyMarkup = message.optJSONObject("reply_markup")
        val buttons = mutableListOf<List<InlineButton>>()
        if (replyMarkup != null) {
            val keyboard = replyMarkup.optJSONArray("inline_keyboard")
            if (keyboard != null) {
                for (i in 0 until keyboard.length()) {
                    val row = keyboard.getJSONArray(i)
                    val rowButtons = mutableListOf<InlineButton>()
                    for (j in 0 until row.length()) {
                        val btn = row.getJSONObject(j)
                        rowButtons.add(InlineButton(
                            text = btn.getString("text"),
                            callbackData = btn.optString("callback_data"),
                            url = btn.optString("url")
                        ))
                    }
                    buttons.add(rowButtons)
                }
            }
        }

        // Check for document/video file
        val document = message.optJSONObject("document")
        val video = message.optJSONObject("video")
        val fileInfo = (document ?: video)?.let {
            FileInfo(
                fileId = it.getString("file_id"),
                fileName = it.optString("file_name", "movie.mkv"),
                fileSize = it.optLong("file_size", 0),
                mimeType = it.optString("mime_type", "video/x-matroska")
            )
        }

        return BotMessage(
            messageId = messageId,
            text = text.ifEmpty { caption },
            buttons = buttons,
            fileInfo = fileInfo
        )
    }
}

// ---- Data classes ----

data class BotMessage(
    val messageId: Long,
    val text: String,
    val buttons: List<List<InlineButton>> = emptyList(),
    val fileInfo: FileInfo? = null
) {
    val hasButtons get() = buttons.isNotEmpty()
    val hasFile get() = fileInfo != null

    /** Parse search results from bot message text */
    fun parseSearchResults(): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lines = text.lines()
        var index = 1
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("/") && !trimmed.startsWith("@")) {
                results.add(SearchResult(id = index, title = trimmed, year = "", quality = ""))
                index++
            }
        }
        // Also parse from inline keyboard buttons
        buttons.flatten().forEachIndexed { i, btn ->
            if (btn.text.isNotEmpty()) {
                results.add(SearchResult(
                    id = i + 1,
                    title = btn.text,
                    year = "",
                    quality = "",
                    callbackData = btn.callbackData
                ))
            }
        }
        return results
    }
}

data class InlineButton(
    val text: String,
    val callbackData: String = "",
    val url: String = ""
)

data class FileInfo(
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
) {
    val fileSizeMb get() = "%.1f MB".format(fileSize / 1024.0 / 1024.0)
}

data class SearchResult(
    val id: Int,
    val title: String,
    val year: String,
    val quality: String,
    val callbackData: String = ""
)
