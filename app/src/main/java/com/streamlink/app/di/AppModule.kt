package com.streamlink.app.di

import android.content.Context
import androidx.room.Room
import com.streamlink.app.data.local.StreamLinkDatabase
import com.streamlink.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamLinkDatabase {
        return Room.databaseBuilder(
            context,
            StreamLinkDatabase::class.java,
            "streamlink.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideMediaItemDao(db: StreamLinkDatabase) = db.mediaItemDao()
    @Provides fun provideSeriesDao(db: StreamLinkDatabase) = db.seriesDao()
    @Provides fun provideSeasonDao(db: StreamLinkDatabase) = db.seasonDao()
    @Provides fun provideEpisodeDao(db: StreamLinkDatabase) = db.episodeDao()
    @Provides fun providePlaybackProgressDao(db: StreamLinkDatabase) = db.playbackProgressDao()
    @Provides fun provideDownloadTaskDao(db: StreamLinkDatabase) = db.downloadTaskDao()
    @Provides fun provideMediaSourceDao(db: StreamLinkDatabase) = db.mediaSourceDao()
}
