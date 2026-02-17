package com.gymdash.companion.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gymdash.companion.data.local.db.dao.SyncLogDao
import com.gymdash.companion.data.local.db.dao.SyncLogEntity

@Database(
    entities = [SyncLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncLogDao(): SyncLogDao
}
