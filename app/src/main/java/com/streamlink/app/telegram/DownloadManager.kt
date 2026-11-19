package com.streamlink.app.telegram

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class Download(
    val id: String,
    val title: String,
    val fileId: Int,
    val totalBytes: Long,
    val downloadedBytes: Long = 0,
    val localPath: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING
)

enum class DownloadStatus { PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED }

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegram: TelegramClient
) {
    companion object {
        private const val TAG = "DownloadManager"
    }

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads: StateFlow<Map<String, Download>> = _downloads.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Track TDLib file progress updates
        scope.launch {
            telegram.downloadProgress.collect { progress ->
                updateDownloadProgress(progress)
            }
        }
    }

    fun startDownload(id: String, title: String, fileId: Int, totalBytes: Long) {
        val download = Download(id, title, fileId, totalBytes, status = DownloadStatus.DOWNLOADING)
        _downloads.value = _downloads.value + (id to download)

        scope.launch {
            Log.d(TAG, "Starting download: $title (fileId=$fileId)")
            try {
                val path = telegram.downloadFile(fileId)
                val updated = _downloads.value[id]?.copy(
                    localPath = path,
                    status = if (path != null) DownloadStatus.COMPLETED else DownloadStatus.FAILED
                ) ?: return@launch
                _downloads.value = _downloads.value + (id to updated)
                Log.d(TAG, "Download complete: $path")
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                val failed = _downloads.value[id]?.copy(status = DownloadStatus.FAILED) ?: return@launch
                _downloads.value = _downloads.value + (id to failed)
            }
        }
    }

    fun cancelDownload(id: String) {
        val dl = _downloads.value[id] ?: return
        telegram.cancelDownload(dl.fileId)
        val cancelled = dl.copy(status = DownloadStatus.CANCELLED)
        _downloads.value = _downloads.value + (id to cancelled)
    }

    fun getDownloadedPath(id: String): String? {
        return _downloads.value[id]?.localPath
    }

    fun deleteDownload(id: String) {
        val dl = _downloads.value[id] ?: return
        dl.localPath?.let { File(it).delete() }
        _downloads.value = _downloads.value - id
    }

    private fun updateDownloadProgress(progress: DownloadProgress) {
        val entry = _downloads.value.entries.find { it.value.fileId == progress.fileId } ?: return
        val updated = entry.value.copy(
            downloadedBytes = progress.downloaded,
            localPath = progress.localPath,
            status = if (progress.localPath != null) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
        )
        _downloads.value = _downloads.value + (entry.key to updated)
    }
}
