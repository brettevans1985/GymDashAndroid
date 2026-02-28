package com.gymdash.companion.domain.repository

import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.data.mapper.MappedHealthData
import com.gymdash.companion.domain.model.SyncErrorType
import com.gymdash.companion.domain.model.SyncResult
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    suspend fun syncHealthData(): SyncResult
    suspend fun readHealthData(): ReadResult
    suspend fun sendHealthData(data: MappedHealthData): SyncResult
    suspend fun sendLatestHeartRate(): HeartRateResult
    fun getSyncHistory(): Flow<List<SyncLogEntity>>
}

sealed class ReadResult {
    data class Success(val data: MappedHealthData) : ReadResult()
    data class Error(val message: String, val type: SyncErrorType = SyncErrorType.UNKNOWN) : ReadResult()
}

sealed class HeartRateResult {
    data class Success(val bpm: Int, val timestamp: String) : HeartRateResult()
    data object NoData : HeartRateResult()
    data class Error(val message: String) : HeartRateResult()
}
