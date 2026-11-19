package com.streamlink.app.data.local.dao

import androidx.room.*
import com.streamlink.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllMediaItems(userId: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItemById(id: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE userId = :userId AND mediaType = :type ORDER BY createdAt DESC")
    fun getMediaItemsByType(userId: String, type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE telegramMessageId = :messageId AND telegramChatId = :chatId")
    suspend fun getByTelegramMessage(messageId: Long, chatId: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE localPath IS NOT NULL AND userId = :userId")
    fun getDownloadedItems(userId: String): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity)

    @Update
    suspend fun updateMediaItem(item: MediaItemEntity)

    @Delete
    suspend fun deleteMediaItem(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM media_items WHERE userId = :userId AND title LIKE '%' || :query || '%'")
    fun searchMediaItems(userId: String, query: String): Flow<List<MediaItemEntity>>
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllSeries(userId: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getSeriesById(id: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: SeriesEntity)

    @Update
    suspend fun updateSeries(series: SeriesEntity)

    @Delete
    suspend fun deleteSeries(series: SeriesEntity)

    @Query("SELECT * FROM series WHERE userId = :userId AND title LIKE '%' || :query || '%'")
    fun searchSeries(userId: String, query: String): Flow<List<SeriesEntity>>
}

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId ORDER BY seasonNumber ASC")
    fun getSeasonsForSeries(seriesId: String): Flow<List<SeasonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeason(season: SeasonEntity)

    @Delete
    suspend fun deleteSeason(season: SeasonEntity)

    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId AND seasonNumber = :number")
    suspend fun getSeasonByNumber(seriesId: String, number: Int): SeasonEntity?
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE seasonId = :seasonId ORDER BY episodeNumber ASC")
    fun getEpisodesForSeason(seasonId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE mediaItemId = :mediaItemId")
    suspend fun getEpisodeByMediaItem(mediaItemId: String): EpisodeEntity?

    @Query("""
        SELECT e.* FROM episodes e
        INNER JOIN seasons s ON e.seasonId = s.id
        WHERE s.seriesId = :seriesId AND e.episodeNumber = :episodeNumber
        ORDER BY s.seasonNumber ASC LIMIT 1
    """)
    suspend fun getNextEpisodeByNumber(seriesId: String, episodeNumber: Int): EpisodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpisode(episode: EpisodeEntity)

    @Update
    suspend fun updateEpisode(episode: EpisodeEntity)

    @Delete
    suspend fun deleteEpisode(episode: EpisodeEntity)
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE userId = :userId AND mediaItemId = :mediaItemId")
    suspend fun getProgress(userId: String, mediaItemId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllProgress(userId: String): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE userId = :userId AND watched = 0 ORDER BY updatedAt DESC LIMIT 20")
    fun getContinueWatching(userId: String): Flow<List<PlaybackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: PlaybackProgressEntity)

    @Query("UPDATE playback_progress SET positionMs = :position, updatedAt = :timestamp WHERE userId = :userId AND mediaItemId = :mediaItemId")
    suspend fun updatePosition(userId: String, mediaItemId: String, position: Long, timestamp: Long)

    @Query("UPDATE playback_progress SET watched = 1, updatedAt = :timestamp WHERE userId = :userId AND mediaItemId = :mediaItemId")
    suspend fun markAsWatched(userId: String, mediaItemId: String, timestamp: Long)

    @Query("SELECT * FROM playback_progress WHERE synced = 0")
    suspend fun getUnsyncedProgress(): List<PlaybackProgressEntity>
}

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks WHERE mediaItemId = :mediaItemId ORDER BY createdAt DESC LIMIT 1")
    fun getDownloadTaskForItem(mediaItemId: String): Flow<DownloadTaskEntity?>

    @Query("SELECT * FROM download_tasks WHERE status IN ('queued', 'downloading', 'paused') ORDER BY createdAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateDownloadTask(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, progress = :progress, bytesDownloaded = :bytes, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, status: String, progress: Float, bytes: Long, timestamp: Long)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MediaSourceDao {
    @Query("SELECT * FROM media_sources WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllSources(userId: String): Flow<List<MediaSourceEntity>>

    @Query("SELECT * FROM media_sources WHERE id = :id")
    suspend fun getSourceById(id: String): MediaSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: MediaSourceEntity)

    @Update
    suspend fun updateSource(source: MediaSourceEntity)

    @Delete
    suspend fun deleteSource(source: MediaSourceEntity)

    @Query("UPDATE media_sources SET status = :status, lastActivity = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, timestamp: Long)
}
