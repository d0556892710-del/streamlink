package com.streamlink.app.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamlink.app.telegram.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    val telegram: TelegramClient,
    val searchEngine: BotSearchEngine,
    val downloadManager: DownloadManager
) : AndroidViewModel(application) {

    val telegramState = telegram.state
    val downloads = downloadManager.downloads

    // Current active bot
    var activeBotUsername: String = "King_Of_Telegram_Beta_bot"

    init {
        telegram.init()
        Log.d("MainViewModel", "Initialized — Telegram state: ${telegramState.value}")
    }

    // ---- Auth ----

    fun sendPhone(phone: String) = viewModelScope.launch {
        try { telegram.sendPhoneNumber(phone) }
        catch (e: Exception) { Log.e("MainViewModel", "sendPhone error: ${e.message}") }
    }

    fun sendCode(code: String) = viewModelScope.launch {
        try { telegram.sendCode(code) }
        catch (e: Exception) { Log.e("MainViewModel", "sendCode error: ${e.message}") }
    }

    fun sendPassword(pw: String) = viewModelScope.launch {
        try { telegram.sendPassword(pw) }
        catch (e: Exception) { Log.e("MainViewModel", "sendPassword error: ${e.message}") }
    }

    // ---- Search ----

    fun search(query: String, onResults: (List<SearchResult>) -> Unit) = viewModelScope.launch {
        try {
            val results = searchEngine.search(activeBotUsername, query)
            onResults(results)
        } catch (e: Exception) {
            Log.e("MainViewModel", "search error: ${e.message}")
            onResults(emptyList())
        }
    }

    fun selectResult(result: SearchResult, onFile: (MediaFile?) -> Unit) = viewModelScope.launch {
        try {
            val file = searchEngine.selectResult(activeBotUsername, result)
            onFile(file)
        } catch (e: Exception) {
            Log.e("MainViewModel", "selectResult error: ${e.message}")
            onFile(null)
        }
    }

    // ---- Downloads ----

    fun startDownload(id: String, title: String, fileId: Int, totalBytes: Long) {
        downloadManager.startDownload(id, title, fileId, totalBytes)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancelDownload(id)
    }

    fun retryDownload(id: String) {
        downloadManager.retryDownload(id)
    }

    fun deleteDownload(id: String) {
        downloadManager.deleteDownload(id)
    }

    /** מחזיר path לנגינה — גם אם ההורדה עדיין בתהליך (streaming) */
    fun getStreamablePath(id: String): String? = downloadManager.getStreamablePath(id)

    /** מחזיר path רק אם ההורדה הושלמה */
    fun getFilePath(id: String): String? = downloadManager.getDownloadedPath(id)

    // ---- Settings ----

    private val prefs by lazy {
        application.getSharedPreferences("streamlink_prefs", Context.MODE_PRIVATE)
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean("wifi_only", enabled).apply()
    }

    fun getWifiOnly(): Boolean = prefs.getBoolean("wifi_only", true)

    fun setDownloadQuality(quality: String) {
        prefs.edit().putString("download_quality", quality).apply()
    }

    fun getDownloadQuality(): String = prefs.getString("download_quality", "הכי גבוהה") ?: "הכי גבוהה"

    override fun onCleared() {
        super.onCleared()
        telegram.close()
    }
}
