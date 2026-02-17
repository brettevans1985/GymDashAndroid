package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    suspend fun syncHealthData(): SyncResult
    fun getSyncHistory(): Flow<List<SyncLogEntity>>
}
