package com.unrot.app.di

import android.content.Context
import androidx.room.Room
import com.unrot.app.data.local.UnrotDatabase
import com.unrot.app.data.local.dao.AppSettingsDao
import com.unrot.app.data.local.dao.BlockAttemptDao
import com.unrot.app.data.local.dao.LimitedAppDao
import com.unrot.app.data.local.dao.UnlockEventDao
import com.unrot.app.data.local.dao.UsageLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UnrotDatabase =
        Room.databaseBuilder(context, UnrotDatabase::class.java, "unrot.db")
            // No migration is written for the isFavorite column added in version 2 — the app has
            // no external backup/export to preserve, so it's simpler to rebuild the local cache
            // than to hand-write a migration for a single new boolean column during active dev.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLimitedAppDao(db: UnrotDatabase): LimitedAppDao = db.limitedAppDao()

    @Provides
    fun provideUsageLogDao(db: UnrotDatabase): UsageLogDao = db.usageLogDao()

    @Provides
    fun provideUnlockEventDao(db: UnrotDatabase): UnlockEventDao = db.unlockEventDao()

    @Provides
    fun provideAppSettingsDao(db: UnrotDatabase): AppSettingsDao = db.appSettingsDao()

    @Provides
    fun provideBlockAttemptDao(db: UnrotDatabase): BlockAttemptDao = db.blockAttemptDao()
}
