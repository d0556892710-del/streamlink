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
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMsg: String? = null
) {
    val progressPct: Int get() =
        if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val downloadedMb: Float get() = downloadedBytes / 1_048_576f
    val totalMb: Float get() = totalBytes / 1_048_576f

    /** true כשאפשר להתחיל לנגן גם לפני שההורדה הושלמה */
    val canStream: Boolean get() = localPath != null || status == DownloadStatus.DOWNLOADING
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

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

    // Map from TDLib fileId → download id (for matching progress updates)
    private val fileIdToDownloadId = mutableMapOf<Int, String>()

    init {
        // Track TDLib file progress updates in real time
        scope.launch {
            telegram.downloadProgress.collect { progress ->
                updateProgress(progress)
            }
        }
    }

    fun startDownload(id: String, title: String, fileId: Int, totalBytes: Long) {
        val existing = _downloads.value[id]
        if (existing?.status == DownloadStatus.DOWNLOADING ||
            existing?.status == DownloadStatus.COMPLETED) return

        fileIdToDownloadId[fileId] = id

        val download = Download(
            id = id,
            title = title,
            fileId = fileId,
            totalBytes = totalBytes,
            status = DownloadStatus.DOWNLOADING
        )
        _downloads.value = _downloads.value + (id to download)

        scope.launch {
            Log.d(TAG, "Starting download: $title (fileId=$fileId)")
            try {
                // TDLib downloadFile with synchronous=false → we get progress via UpdateFile
                // then when complete localPath is set
                val path = telegram.downloadFile(fileId, priority = 1)
                val current = _downloads.value[id] ?: return@launch
                _downloads.value = _downloads.value + (id to current.copy(
                    localPath = path,
                    downloadedBytes = current.totalBytes,
                    status = if (path != null) DownloadStatus.COMPLETED else DownloadStatus.FAILED
                ))
                if (path != null) {
                    Log.d(TAG, "Download complete: $path")
                } else {
                    Log.e(TAG, "Download returned null path")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                val current = _downloads.value[id] ?: return@launch
                _downloads.value = _downloads.value + (id to current.copy(
                    status = DownloadStatus.FAILED,
                    errorMsg = e.message
                ))
            }
        }
    }

    fun cancelDownload(id: String) {
        val dl = _downloads.value[id] ?: return
        telegram.cancelDownload(dl.fileId)
        fileIdToDownloadId.remove(dl.fileId)
        _downloads.value = _downloads.value + (id to dl.copy(status = DownloadStatus.CANCELLED))
        Log.d(TAG, "Cancelled: ${dl.title}")
    }

    fun retryDownload(id: String) {
        val dl = _downloads.value[id] ?: return
        if (dl.status == DownloadStatus.FAILED || dl.status == DownloadStatus.CANCELLED) {
            startDownload(id, dl.title, dl.fileId, dl.totalBytes)
        }
    }

    fun getDownloadedPath(id: String): String? {
        return _downloads.value[id]?.localPath
    }

    /** מחזיר path שאפשר להעביר ל-ExoPlayer — גם אם ההורדה עדיין בתהליך */
    fun getStreamablePath(id: String): String? {
        val dl = _downloads.value[id] ?: return null
        return when (dl.status) {
            DownloadStatus.COMPLETED -> dl.localPath
            DownloadStatus.DOWNLOADING -> dl.localPath // TDLib כותב לקובץ בזמן אמת
            else -> null
        }
    }

    fun deleteDownload(id: String) {
        val dl = _downloads.value[id] ?: return
        dl.localPath?.let { File(it).delete() }
        fileIdToDownloadId.remove(dl.fileId)
        _downloads.value = _downloads.value - id
        Log.d(TAG, "Deleted: ${dl.title}")
    }

    private fun updateProgress(progress: DownloadProgress) {
        val id = fileIdToDownloadId[progress.fileId] ?: return
        val current = _downloads.value[id] ?: return

        val newStatus = when {
            progress.localPath != null -> DownloadStatus.COMPLETED
            progress.downloaded > 0   -> DownloadStatus.DOWNLOADING
            else                       -> current.status
        }

        _downloads.value = _downloads.value + (id to current.copy(
            downloadedBytes = progress.downloaded,
            totalBytes      = if (progress.total > 0) progress.total else current.totalBytes,
            localPath       = progress.localPath ?: current.localPath,
            status          = newStatus
        ))
    }
}
