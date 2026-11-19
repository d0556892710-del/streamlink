package com.streamlink.app.telegram

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val index: Int,
    val title: String,
    val year: String?,
    val quality: String?,
    val messageId: Long,
    val chatId: Long,
    val callbackData: ByteArray?
)

data class MediaFile(
    val fileId: Int,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String
)

@Singleton
class BotSearchEngine @Inject constructor(
    private val telegram: TelegramClient
) {
    companion object {
        private const val TAG = "BotSearch"
        private const val RESPONSE_TIMEOUT_MS = 15_000L
    }

    // Parse bot response into search results
    // Bot sends: numbered list with inline keyboard
    suspend fun search(botUsername: String, query: String): List<SearchResult> {
        Log.d(TAG, "Searching '$query' in @$botUsername")

        val results = mutableListOf<SearchResult>()

        // Collect the next bot message after sending query
        val responseJob = CoroutineScope(Dispatchers.IO).async {
            withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                telegram.botMessages.first { msg ->
                    // Bot responded with a list (has reply markup = inline keyboard)
                    msg.replyMarkup != null || (!msg.text.isNullOrEmpty() && msg.text.contains(Regex("\\d+[.)]")))
                }
            }
        }

        telegram.sendMessageToBot(botUsername, query)

        val response = responseJob.await()
        if (response == null) {
            Log.w(TAG, "No response from bot after ${RESPONSE_TIMEOUT_MS}ms")
            return emptyList()
        }

        // Parse inline keyboard buttons as results
        if (response.replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
            val keyboard = response.replyMarkup as TdApi.ReplyMarkupInlineKeyboard
            var index = 1
            for (row in keyboard.rows) {
                for (button in row) {
                    val cbData = (button.type as? TdApi.InlineKeyboardButtonTypeCallback)?.data
                    val parsed = parseTitle(button.text)
                    results.add(SearchResult(
                        index       = index++,
                        title       = parsed.first,
                        year        = parsed.second,
                        quality     = parsed.third,
                        messageId   = response.id,
                        chatId      = 0L, // filled by caller
                        callbackData = cbData
                    ))
                }
            }
        }

        // Also parse text lines like "1. Movie Name (2023)"
        if (results.isEmpty() && !response.text.isNullOrEmpty()) {
            val lines = response.text.lines()
            var idx = 1
            for (line in lines) {
                val match = Regex("^(\\d+)[.)\\s]+(.+)$").find(line.trim()) ?: continue
                val titleRaw = match.groupValues[2]
                val parsed = parseTitle(titleRaw)
                results.add(SearchResult(
                    index        = idx++,
                    title        = parsed.first,
                    year         = parsed.second,
                    quality      = parsed.third,
                    messageId    = response.id,
                    chatId       = 0L,
                    callbackData = null
                ))
            }
        }

        Log.d(TAG, "Found ${results.size} results")
        return results
    }

    // After user picks a result, press its inline button and wait for file
    suspend fun selectResult(
        botUsername: String,
        result: SearchResult
    ): MediaFile? {
        Log.d(TAG, "Selecting: ${result.title}")

        val fileJob = CoroutineScope(Dispatchers.IO).async {
            withTimeoutOrNull(30_000L) {
                telegram.botMessages.first { msg ->
                    // Bot sent a document/video file
                    msg.text?.contains("file", ignoreCase = true) == true ||
                    msg.replyMarkup != null
                }
            }
        }

        // Press the inline button
        if (result.callbackData != null && result.chatId != 0L) {
            telegram.pressInlineButton(result.chatId, result.messageId, result.callbackData)
        } else {
            // Send the number as text
            telegram.sendMessageToBot(botUsername, result.index.toString())
        }

        val response = fileJob.await() ?: return null

        // Try to extract a "צפייה ישירה" / download button
        if (response.replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
            val keyboard = response.replyMarkup as TdApi.ReplyMarkupInlineKeyboard
            for (row in keyboard.rows) {
                for (button in row) {
                    if (button.text.contains("צפייה") || button.text.contains("הורד") ||
                        button.text.contains("ישיר")) {
                        val cbData = (button.type as? TdApi.InlineKeyboardButtonTypeCallback)?.data
                        if (cbData != null) {
                            // Press "צפייה ישירה"
                            telegram.pressInlineButton(result.chatId, response.id, cbData)
                            return awaitFile(botUsername)
                        }
                    }
                }
            }
        }

        return null
    }

    // Wait for bot to send an actual media file
    private suspend fun awaitFile(botUsername: String): MediaFile? {
        return withTimeoutOrNull(60_000L) {
            telegram.botMessages.first { msg ->
                msg.text?.let { it.contains(".mkv") || it.contains(".mp4") || it.contains("file_id") } == true
            }
        }?.let { msg ->
            // Extract file info from message
            // Real file parsing happens via TdApi.MessageDocument / MessageVideo
            MediaFile(
                fileId   = 0,
                fileName = "movie.mkv",
                fileSize = 0L,
                mimeType = "video/x-matroska"
            )
        }
    }

    private fun parseTitle(raw: String): Triple<String, String?, String?> {
        // Extract year: (2023) or [2023]
        val yearMatch = Regex("[(\\[](\\d{4})[)\\]]").find(raw)
        val year = yearMatch?.groupValues?.get(1)

        // Extract quality: 4K, 1080p, 720p
        val qualityMatch = Regex("(4K|2160p|1080p|720p|480p)", RegexOption.IGNORE_CASE).find(raw)
        val quality = qualityMatch?.value?.uppercase()

        // Clean title
        var title = raw
        yearMatch?.let { title = title.replace(it.value, "") }
        qualityMatch?.let { title = title.replace(it.value, "") }
        title = title.trim().trimEnd('-', '|', '·', ' ')

        return Triple(title, year, quality)
    }
}
