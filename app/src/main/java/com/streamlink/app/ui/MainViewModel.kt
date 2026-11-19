package com.streamlink.app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlink.app.telegram.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val telegram: TelegramClient,
    val searchEngine: BotSearchEngine,
    val downloadManager: DownloadManager
) : ViewModel() {

    val telegramState = telegram.state
    val downloads = downloadManager.downloads

    // Current active bot
    var activeBotUsername: String = "King_Of_Telegram_Beta_bot"

    init {
        telegram.init()
        Log.d("MainViewModel", "Initialized — Telegram state: ${telegramState.value}")
    }

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

    fun startDownload(id: String, title: String, fileId: Int, totalBytes: Long) {
        downloadManager.startDownload(id, title, fileId, totalBytes)
    }

    fun getFilePath(id: String): String? = downloadManager.getDownloadedPath(id)

    override fun onCleared() {
        super.onCleared()
        telegram.close()
    }
}
