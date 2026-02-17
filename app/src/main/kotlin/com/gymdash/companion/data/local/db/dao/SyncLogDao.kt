package com.gymdash.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val timestamp: Long,
    val recordsAccepted: Int,
    val recordsRejected: Int,
    val status: String,
    val errorMessage: String? = null
)

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insert(log: SyncLogEntity)

    @Query("SELECT * FROM sync_log ORDER BY timestamp DESC")
    fun getAll(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_log ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): SyncLogEntity?

    @Query("DELETE FROM sync_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
