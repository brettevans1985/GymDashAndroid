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
    suspend fun readTodaySummary(): TodaySummaryResult
    fun getSyncHistory(): Flow<List<SyncLogEntity>>
}

sealed class ReadResult {
    data class Success(val data: MappedHealthData) : ReadResult()
    data class Error(val message: String, val type: SyncErrorType = SyncErrorType.UNKNOWN) : ReadResult()
}

data class TodaySummary(
    val steps: Long? = null,
    val distanceKm: Double? = null,
    val activeCalories: Double? = null,
    val floorsClimbed: Double? = null,
    val weightKg: Double? = null,
    val sleepHours: Double? = null,
    val restingHeartRate: Int? = null,
    val latestHeartRate: Int? = null,
    val spO2Percent: Int? = null,
    val hrvMs: Double? = null,
    val respiratoryRate: Double? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val bodyTempCelsius: Double? = null,
    val vo2Max: Double? = null,
    val bloodGlucoseMmolL: Double? = null
)

sealed class TodaySummaryResult {
    data class Success(val summary: TodaySummary) : TodaySummaryResult()
    data class Error(val message: String) : TodaySummaryResult()
}
