package com.gymdash.companion.di

import android.content.Context
import androidx.room.Room
import com.gymdash.companion.data.local.db.AppDatabase
import com.gymdash.companion.data.local.db.dao.SyncLogDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gymdash_db"
        ).build()

    @Provides
    fun provideSyncLogDao(database: AppDatabase): SyncLogDao = database.syncLogDao()
}
