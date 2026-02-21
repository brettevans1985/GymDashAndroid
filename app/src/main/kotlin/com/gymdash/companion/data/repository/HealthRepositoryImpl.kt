package com.gymdash.companion.data.repository

import android.os.Build
import androidx.health.connect.client.records.HeartRateRecord
import com.gymdash.companion.data.healthconnect.HealthConnectDataSource
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.db.dao.SyncLogDao
import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.data.mapper.HealthDataMapper
import com.gymdash.companion.data.remote.api.GymDashApi
import com.gymdash.companion.data.remote.dto.HeartRateReadingSync
import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HeartRateResult
import com.gymdash.companion.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val gymDashApi: GymDashApi,
    private val mapper: HealthDataMapper,
    private val syncLogDao: SyncLogDao,
    private val preferences: SyncPreferences
) : HealthRepository {

    override suspend fun syncHealthData(): SyncResult {
        val token = preferences.authToken.first()
            ?: return SyncResult.Error("Not authenticated")

        return try {
            val changesToken = preferences.lastChangesToken.first()
            val records = if (changesToken != null) {
                val result = healthConnectDataSource.getChanges(changesToken)
                preferences.setLastChangesToken(result.nextToken)
                result.records
            } else {
                val newToken = healthConnectDataSource.getChangesToken()
                preferences.setLastChangesToken(newToken)
                // First sync: read last 7 days
                val now = Instant.now()
                val sevenDaysAgo = now.minusSeconds(7 * 24 * 60 * 60)
                HealthConnectDataSource.RECORD_TYPES.flatMap { recordType ->
                    healthConnectDataSource.readRecords(recordType, sevenDaysAgo, now)
                }
            }

            val mappedData = mapper.mapRecords(records)
            if (mappedData.isEmpty) {
                return SyncResult.Success(recordsProcessed = 0, recordsCreated = 0, recordsUpdated = 0)
            }

            val request = HealthSyncRequest(
                deviceName = Build.MODEL,
                heartRateReadings = mappedData.heartRateReadings,
                sleepSessions = mappedData.sleepSessions,
                dailyActivitySummaries = mappedData.dailyActivitySummaries,
                spO2Readings = mappedData.spO2Readings,
                hrvReadings = mappedData.hrvReadings,
                weightReadings = mappedData.weightReadings
            )

            val response = gymDashApi.syncHealthData(token, request)
            val now = System.currentTimeMillis()

            syncLogDao.insert(
                SyncLogEntity(
                    timestamp = now,
                    recordsProcessed = response.recordsProcessed,
                    recordsCreated = response.recordsCreated,
                    recordsUpdated = response.recordsUpdated,
                    status = "success"
                )
            )
            preferences.setLastSyncTimestamp(now)

            SyncResult.Success(
                recordsProcessed = response.recordsProcessed,
                recordsCreated = response.recordsCreated,
                recordsUpdated = response.recordsUpdated
            )
        } catch (e: Exception) {
            syncLogDao.insert(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    recordsProcessed = 0,
                    recordsCreated = 0,
                    recordsUpdated = 0,
                    status = "error",
                    errorMessage = e.message
                )
            )
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun sendLatestHeartRate(): HeartRateResult {
        val token = preferences.authToken.first()
            ?: return HeartRateResult.Error("Not authenticated")

        return try {
            val now = Instant.now()
            val windowStart = now.minusSeconds(30)

            val records = healthConnectDataSource.readRecords(
                HeartRateRecord::class, windowStart, now
            )

            if (records.isEmpty()) return HeartRateResult.NoData

            val latestSample = records
                .flatMap { it.samples }
                .maxByOrNull { it.time }
                ?: return HeartRateResult.NoData

            val reading = HeartRateReadingSync(
                timestamp = latestSample.time.toString(),
                beatsPerMinute = latestSample.beatsPerMinute.toInt()
            )

            gymDashApi.syncHealthData(
                token,
                HealthSyncRequest(
                    deviceName = Build.MODEL,
                    heartRateReadings = listOf(reading)
                )
            )

            HeartRateResult.Success(
                bpm = reading.beatsPerMinute,
                timestamp = reading.timestamp
            )
        } catch (e: Exception) {
            HeartRateResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun getSyncHistory(): Flow<List<SyncLogEntity>> = syncLogDao.getAll()
}
