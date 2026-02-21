package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    suspend fun syncHealthData(): SyncResult
    suspend fun sendLatestHeartRate(): HeartRateResult
    fun getSyncHistory(): Flow<List<SyncLogEntity>>
}

sealed class HeartRateResult {
    data class Success(val bpm: Int, val timestamp: String) : HeartRateResult()
    data object NoData : HeartRateResult()
    data class Error(val message: String) : HeartRateResult()
}
