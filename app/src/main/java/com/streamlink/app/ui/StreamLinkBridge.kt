package com.streamlink.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.streamlink.app.telegram.SearchResult
import kotlinx.coroutines.*

class StreamLinkBridge(
    private val context: Context,
    private val webView: WebView,
    private val viewModel: MainViewModel
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    init {
        // Start observing state immediately so auth callbacks reach JS from the start
        observeTelegramState()
        observeDownloads()
    }

    // ---- Telegram Auth ----

    @JavascriptInterface
    fun sendTelegramCode(phoneNumber: String) {
        Log.d("Bridge", "sendTelegramCode: $phoneNumber")
        viewModel.sendPhone(phoneNumber)
    }

    @JavascriptInterface
    fun verifyTelegramCode(code: String) {
        Log.d("Bridge", "verifyTelegramCode: $code")
        viewModel.sendCode(code)
    }

    @JavascriptInterface
    fun verifyTelegramPassword(password: String) {
        viewModel.sendPassword(password)
    }

    @JavascriptInterface
    fun getTelegramState(): String {
        return viewModel.telegramState.value::class.simpleName ?: "Unknown"
    }

    // ---- Bot Search ----

    @JavascriptInterface
    fun searchBot(botUsername: String, query: String) {
        Log.d("Bridge", "searchBot: @$botUsername '$query'")
        if (botUsername.isNotEmpty()) viewModel.activeBotUsername = botUsername

        callJS("window.onSearchStart && window.onSearchStart()")

        viewModel.search(query) { results ->
            val json = gson.toJson(results.map { r -> mapOf(
                "index"   to r.index,
                "title"   to r.title,
                "year"    to (r.year ?: ""),
                "quality" to (r.quality ?: ""),
                "msgId"   to r.messageId,
                "chatId"  to r.chatId
            )})
            callJS("window.onSearchResults && window.onSearchResults($json)")
        }
    }

    @JavascriptInterface
    fun selectSearchResult(index: Int, title: String, msgId: Long, chatId: Long) {
        Log.d("Bridge", "selectSearchResult: #$index $title")
        callJS("window.onSelectingResult && window.onSelectingResult('$title')")

        // Reconstruct result object
        val result = SearchResult(index, title, null, null, msgId, chatId, null)
        viewModel.selectResult(result) { file ->
            if (file != null) {
                val json = gson.toJson(mapOf(
                    "fileId"   to file.fileId,
                    "fileName" to file.fileName,
                    "fileSize" to file.fileSize,
                    "mimeType" to file.mimeType
                ))
                callJS("window.onFileReady && window.onFileReady($json)")
            } else {
                callJS("window.onFileError && window.onFileError('לא נמצא קובץ')")
            }
        }
    }

    // ---- Download ----

    @JavascriptInterface
    fun startDownload(id: String, title: String, fileId: Int, totalBytes: Long) {
        Log.d("Bridge", "startDownload: $title (fileId=$fileId)")
        viewModel.startDownload(id, title, fileId, totalBytes)
        observeDownloads()
    }

    @JavascriptInterface
    fun cancelDownload(id: String) {
        Log.d("Bridge", "cancelDownload: $id")
        viewModel.cancelDownload(id)
    }

    @JavascriptInterface
    fun retryDownload(id: String) {
        Log.d("Bridge", "retryDownload: $id")
        viewModel.retryDownload(id)
    }

    @JavascriptInterface
    fun deleteDownload(id: String) {
        Log.d("Bridge", "deleteDownload: $id")
        viewModel.deleteDownload(id)
    }

    @JavascriptInterface
    fun getDownloadPath(id: String): String {
        return viewModel.getFilePath(id) ?: ""
    }

    @JavascriptInterface
    fun streamFile(id: String, title: String) {
        Log.d("Bridge", "streamFile: $id")
        val path = viewModel.getStreamablePath(id)
        if (path != null) {
            playFile(path, title)
        } else {
            callJS("window.onFileError && window.onFileError('הקובץ לא מוכן לנגינה')")
        }
    }

    // ---- Player ----

    @JavascriptInterface
    fun playFile(filePath: String, title: String) {
        Log.d("Bridge", "playFile: $title @ $filePath")
        // Launch ExoPlayer activity
        scope.launch {
            val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
                putExtra("PATH", filePath)
                putExtra("TITLE", title)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ---- Settings ----

    @JavascriptInterface
    fun setActiveBotUsername(username: String) {
        viewModel.activeBotUsername = username.trimStart('@')
        callJS("window.onBotSet && window.onBotSet('$username')")
    }

    @JavascriptInterface
    fun showToast(message: String) {
        scope.launch {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.d("StreamLink-JS", message)
    }

    // ---- Observers ----

    private var stateObserved = false
    private fun observeTelegramState() {
        if (stateObserved) return
        stateObserved = true
        scope.launch {
            viewModel.telegramState.collect { state ->
                val stateName = state::class.simpleName ?: "Unknown"
                callJS("window.onTelegramState && window.onTelegramState('$stateName')")
            }
        }
    }

    private var downloadsObserved = false
    private fun observeDownloads() {
        if (downloadsObserved) return
        downloadsObserved = true
        scope.launch {
            viewModel.downloads.collect { downloads ->
                val list = downloads.values.map { d -> mapOf(
                    "id"         to d.id,
                    "title"      to d.title,
                    "total"      to d.totalBytes,
                    "downloaded" to d.downloadedBytes,
                    "status"     to d.status.name,
                    "path"       to (d.localPath ?: "")
                )}
                val json = gson.toJson(list)
                callJS("window.onDownloadsUpdate && window.onDownloadsUpdate($json)")
            }
        }
    }

    // ---- Utility / Settings ----

    @JavascriptInterface
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Bridge", "openUrl failed: $url", e)
        }
    }

    @JavascriptInterface
    fun setWifiOnly(enabled: String) {
        val on = enabled == "true"
        Log.d("Bridge", "setWifiOnly: $on")
        viewModel.setWifiOnly(on)
    }

    @JavascriptInterface
    fun setDownloadQuality(quality: String) {
        Log.d("Bridge", "setDownloadQuality: $quality")
        viewModel.setDownloadQuality(quality)
    }

    private fun callJS(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }

    fun destroy() {
        scope.cancel()
    }
}
