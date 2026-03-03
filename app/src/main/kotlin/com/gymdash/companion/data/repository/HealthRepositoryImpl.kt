package com.gymdash.companion.data.repository

import android.os.Build
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import com.gymdash.companion.BuildConfig
import com.gymdash.companion.data.healthconnect.HealthConnectDataSource
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.data.local.db.dao.SyncLogDao
import com.gymdash.companion.data.local.db.dao.SyncLogEntity
import com.gymdash.companion.data.mapper.HealthDataMapper
import com.gymdash.companion.data.mapper.MappedHealthData
import com.gymdash.companion.data.mock.MockHealthDataGenerator
import com.gymdash.companion.data.remote.api.GymDashApi
import com.gymdash.companion.data.remote.dto.HealthSyncRequest
import com.gymdash.companion.domain.model.SyncErrorType
import com.gymdash.companion.domain.model.SyncResult
import com.gymdash.companion.domain.repository.HealthRepository
import com.gymdash.companion.domain.repository.ReadResult
import com.gymdash.companion.domain.repository.TodaySummary
import com.gymdash.companion.domain.repository.TodaySummaryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
        return when (val readResult = readHealthData()) {
            is ReadResult.Success -> sendHealthData(readResult.data)
            is ReadResult.Error -> SyncResult.Error(readResult.message, readResult.type)
        }
    }

    override suspend fun readHealthData(): ReadResult {
        preferences.authToken.first()
            ?: return ReadResult.Error("Not authenticated", SyncErrorType.AUTH_EXPIRED)

        return try {
            val serverUrl = preferences.serverUrl.first()
            val useMock = BuildConfig.DEBUG && serverUrl != BuildConfig.PRODUCTION_SERVER_URL

            val mappedData = if (useMock) {
                mockHealthDataGenerator.generate()
            } else {
                val changesToken = preferences.lastChangesToken.first()
                val records = if (changesToken != null) {
                    try {
                        val result = healthConnectDataSource.getChanges(changesToken)
                        preferences.setLastChangesToken(result.nextToken)
                        result.records
                    } catch (e: SecurityException) {
                        // Stale changes token (e.g. record types changed) — reset and do a full read
                        preferences.setLastChangesToken(null)
                        val newToken = healthConnectDataSource.getChangesToken()
                        preferences.setLastChangesToken(newToken)
                        val now = Instant.now()
                        val sevenDaysAgo = now.minusSeconds(7 * 24 * 60 * 60)
                        HealthConnectDataSource.RECORD_TYPES.flatMap { recordType ->
                            healthConnectDataSource.readRecords(recordType, sevenDaysAgo, now)
                        }
                    }
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

            ReadResult.Success(mappedData)
        } catch (e: Exception) {
            val (errorType, message) = classifyError(e)
            ReadResult.Error(message, errorType)
        }
    }

    override suspend fun sendHealthData(data: MappedHealthData): SyncResult {
        val token = preferences.authToken.first()
            ?: return SyncResult.Error("Not authenticated", SyncErrorType.AUTH_EXPIRED)

        return try {
            if (data.isEmpty) {
                return SyncResult.Success(recordsProcessed = 0, recordsCreated = 0, recordsUpdated = 0)
            }

            val request = HealthSyncRequest(
                deviceName = Build.MODEL,
                heartRateReadings = data.heartRateReadings,
                sleepSessions = data.sleepSessions,
                dailyActivitySummaries = data.dailyActivitySummaries,
                spO2Readings = data.spO2Readings,
                hrvReadings = data.hrvReadings,
                weightReadings = data.weightReadings,
                respiratoryRateReadings = data.respiratoryRateReadings,
                bloodPressureReadings = data.bloodPressureReadings,
                bodyTemperatureReadings = data.bodyTemperatureReadings,
                vo2MaxReadings = data.vo2MaxReadings,
                bloodGlucoseReadings = data.bloodGlucoseReadings,
                waterIntakes = data.waterIntakes,
                heightCm = data.heightCm
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

    override suspend fun readTodaySummary(): TodaySummaryResult {
        return try {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS)

            val steps = healthConnectDataSource.readRecords(StepsRecord::class, startOfDay, now)
                .sumOf { it.count }
                .takeIf { it > 0 }

            val distanceKm = healthConnectDataSource.readRecords(DistanceRecord::class, startOfDay, now)
                .sumOf { it.distance.inKilometers }
                .takeIf { it > 0.0 }

            val activeCalories = healthConnectDataSource.readRecords(ActiveCaloriesBurnedRecord::class, startOfDay, now)
                .sumOf { it.energy.inKilocalories }
                .takeIf { it > 0.0 }

            val floors = healthConnectDataSource.readRecords(FloorsClimbedRecord::class, startOfDay, now)
                .sumOf { it.floors }
                .takeIf { it > 0.0 }

            val weightKg = healthConnectDataSource.readRecords(WeightRecord::class, thirtyDaysAgo, now)
                .maxByOrNull { it.time }
                ?.weight?.inKilograms

            // Last night's sleep: look for sessions ending today
            val yesterdayEvening = startOfDay.minusSeconds(12 * 60 * 60)
            val sleepHours = healthConnectDataSource.readRecords(SleepSessionRecord::class, yesterdayEvening, now)
                .maxByOrNull { it.endTime }
                ?.let { session ->
                    val durationMs = session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
                    durationMs / 3_600_000.0
                }

            // Resting heart rate — latest today
            val restingHr = healthConnectDataSource.readRecords(RestingHeartRateRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.beatsPerMinute?.toInt()

            // Latest heart rate reading today
            val latestHr = healthConnectDataSource.readRecords(HeartRateRecord::class, startOfDay, now)
                .flatMap { it.samples }
                .maxByOrNull { it.time }
                ?.beatsPerMinute?.toInt()

            // SpO2 — latest today
            val spO2 = healthConnectDataSource.readRecords(OxygenSaturationRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.percentage?.value?.toInt()

            // HRV — latest today
            val hrv = healthConnectDataSource.readRecords(HeartRateVariabilityRmssdRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.heartRateVariabilityMillis

            // Respiratory rate — latest today
            val respRate = healthConnectDataSource.readRecords(RespiratoryRateRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.rate

            // Blood pressure — latest today
            val bp = healthConnectDataSource.readRecords(BloodPressureRecord::class, startOfDay, now)
                .maxByOrNull { it.time }

            // Body temperature — latest today
            val bodyTemp = healthConnectDataSource.readRecords(BodyTemperatureRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.temperature?.inCelsius

            // VO2 Max — latest in last 30 days
            val vo2Max = healthConnectDataSource.readRecords(Vo2MaxRecord::class, thirtyDaysAgo, now)
                .maxByOrNull { it.time }
                ?.vo2MillilitersPerMinuteKilogram

            // Blood glucose — latest today
            val glucose = healthConnectDataSource.readRecords(BloodGlucoseRecord::class, startOfDay, now)
                .maxByOrNull { it.time }
                ?.level?.inMillimolesPerLiter

            TodaySummaryResult.Success(
                TodaySummary(
                    steps = steps,
                    distanceKm = distanceKm,
                    activeCalories = activeCalories,
                    floorsClimbed = floors,
                    weightKg = weightKg,
                    sleepHours = sleepHours,
                    restingHeartRate = restingHr,
                    latestHeartRate = latestHr,
                    spO2Percent = spO2,
                    hrvMs = hrv,
                    respiratoryRate = respRate,
                    bloodPressureSystolic = bp?.systolic?.inMillimetersOfMercury?.toInt(),
                    bloodPressureDiastolic = bp?.diastolic?.inMillimetersOfMercury?.toInt(),
                    bodyTempCelsius = bodyTemp,
                    vo2Max = vo2Max,
                    bloodGlucoseMmolL = glucose
                )
            )
        } catch (e: Exception) {
            TodaySummaryResult.Error(e.message ?: "Failed to read today's summary")
        }
    }

    override fun getSyncHistory(): Flow<List<SyncLogEntity>> = syncLogDao.getAll()
}
