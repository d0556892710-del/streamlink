package com.streamlink.app.data.local.entities

import androidx.room.*

// ─────────────────────────────────────────────
// MediaSource — Telegram bot / source connection
// ─────────────────────────────────────────────
@Entity(tableName = "media_sources")
data class MediaSourceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sourceType: String,          // "telegram_bot", "manual"
    val displayName: String,
    val username: String,
    val status: String,              // "connected", "disconnected", "error"
    val capabilities: String,        // JSON: { supportsEpisodeRequests, supportsInlineActions, ... }
    val lastActivity: Long?,
    val createdAt: Long
)

// ─────────────────────────────────────────────
// MediaItem — a single video file
// ─────────────────────────────────────────────
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaType: String,           // "movie", "episode", "unknown"
    val title: String,
    val description: String?,
    val posterPath: String?,         // local path
    val localPath: String?,          // local video file path
    val cloudObjectKey: String?,     // S3 key (optional backup)
    val sourceId: String?,
    val telegramMessageId: Long?,
    val telegramChatId: Long?,
    val fileSize: Long?,
    val mimeType: String?,
    val durationMs: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

// ─────────────────────────────────────────────
// Series
// ─────────────────────────────────────────────
@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val artworkPath: String?,        // local poster
    val year: Int?,
    val genre: String?,
    val createdAt: Long,
    val updatedAt: Long
)

// ─────────────────────────────────────────────
// Season
// ─────────────────────────────────────────────
@Entity(
    tableName = "seasons",
    foreignKeys = [ForeignKey(
        entity = SeriesEntity::class,
        parentColumns = ["id"],
        childColumns = ["seriesId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("seriesId")]
)
data class SeasonEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int
)

// ─────────────────────────────────────────────
// Episode
// ─────────────────────────────────────────────
@Entity(
    tableName = "episodes",
    foreignKeys = [ForeignKey(
        entity = SeasonEntity::class,
        parentColumns = ["id"],
        childColumns = ["seasonId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("seasonId"), Index("mediaItemId")]
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String?,
    val durationMs: Long?,
    val mediaItemId: String?,        // null if episode not yet downloaded
    val thumbnailPath: String?,
    val manuallyOrdered: Boolean = false
)

// ─────────────────────────────────────────────
// PlaybackProgress
// ─────────────────────────────────────────────
@Entity(
    tableName = "playback_progress",
    indices = [Index("userId"), Index("mediaItemId")]
)
data class PlaybackProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaItemId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val watched: Boolean = false     // only true when watched >90%
)

// ─────────────────────────────────────────────
// DownloadTask
// ─────────────────────────────────────────────
@Entity(
    tableName = "download_tasks",
    indices = [Index("mediaItemId")]
)
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val mediaItemId: String,
    val status: String,              // "queued", "downloading", "paused", "completed", "failed", "cancelled"
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val workerRequestId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

// ─────────────────────────────────────────────
// SyncRecord — for cloud sync conflict resolution
// ─────────────────────────────────────────────
@Entity(
    tableName = "sync_records",
    indices = [Index("userId"), Index("entityId")]
)
data class SyncRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val entityType: String,         // "media_item", "series", "episode", "progress"
    val entityId: String,
    val version: Int,
    val updatedAt: Long,
    val synced: Boolean = false
)
