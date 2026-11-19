package com.streamlink.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.streamlink.app.data.local.dao.*
import com.streamlink.app.data.local.entities.*

@Database(
    entities = [
        MediaSourceEntity::class,
        MediaItemEntity::class,
        SeriesEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        PlaybackProgressEntity::class,
        DownloadTaskEntity::class,
        SyncRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class StreamLinkDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun seriesDao(): SeriesDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun mediaSourceDao(): MediaSourceDao
}
