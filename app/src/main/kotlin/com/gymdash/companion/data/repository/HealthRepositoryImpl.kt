package com.gymdash.companion.data.repository

import android.os.Build
import androidx.health.connect.client.records.HeartRateRecord
import com.gymdash.companion.BuildConfig
import com.gymdash.companion.data.healthconnect.HealthConnectDataSource
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.db.dao.SyncLogDao
import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.data.mapper.HealthDataMapper
import com.gymdash.companion.data.mock.MockHealthDataGenerator
import com.gymdash.companion.data.remote.api.GymDashApi
import com.gymdash.companion.data.remote.dto.HeartRateReadingSync
import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.domain.model.SyncErrorType
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HeartRateResult
import com.gymdash.companion.domain.repository.HealthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.Instant
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val gymDashApi: GymDashApi,
    private val mapper: HealthDataMapper,
    private val syncLogDao: SyncLogDao,
    private val preferences: SyncPreferences,
    private val mockHealthDataGenerator: MockHealthDataGenerator
) : HealthRepository {

    override suspend fun syncHealthData(): SyncResult {
        val token = preferences.authToken.first()
            ?: return SyncResult.Error("Not authenticated", SyncErrorType.AUTH_EXPIRED)

        return try {
            val serverUrl = preferences.serverUrl.first()
            val useMock = BuildConfig.DEBUG && serverUrl == BuildConfig.DEFAULT_SERVER_URL

            val mappedData = if (useMock) {
                mockHealthDataGenerator.generate()
            } else {
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
                mapper.mapRecords(records)
            }

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
                weightReadings = mappedData.weightReadings,
                respiratoryRateReadings = mappedData.respiratoryRateReadings,
                bloodPressureReadings = mappedData.bloodPressureReadings,
                bodyTemperatureReadings = mappedData.bodyTemperatureReadings,
                vo2MaxReadings = mappedData.vo2MaxReadings,
                bloodGlucoseReadings = mappedData.bloodGlucoseReadings,
                heightCm = mappedData.heightCm
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

            // Clean up sync logs older than 30 days
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            syncLogDao.deleteOlderThan(thirtyDaysAgo)

            SyncResult.Success(
                recordsProcessed = response.recordsProcessed,
                recordsCreated = response.recordsCreated,
                recordsUpdated = response.recordsUpdated
            )
        } catch (e: Exception) {
            val (errorType, message) = classifyError(e)
            syncLogDao.insert(
                SyncLogEntity(
                    timestamp = System.currentTimeMillis(),
                    recordsProcessed = 0,
                    recordsCreated = 0,
                    recordsUpdated = 0,
                    status = "error",
                    errorMessage = message
                )
            )
            SyncResult.Error(message, errorType)
        }
    }

    private fun classifyError(e: Exception): Pair<SyncErrorType, String> {
        return when {
            e is UnknownHostException || e is ConnectException ->
                SyncErrorType.NETWORK to "No network connection. Please check your internet and try again."
            e is HttpException && e.code() == 401 ->
                SyncErrorType.AUTH_EXPIRED to "Session expired. Please log in again."
            e is HttpException && e.code() in 500..599 ->
                SyncErrorType.SERVER to "Server error. Please try again later."
            e is SecurityException ->
                SyncErrorType.HEALTH_CONNECT to "Health Connect permission denied. Please grant permissions."
            else ->
                SyncErrorType.UNKNOWN to (e.message ?: "Unknown error")
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
