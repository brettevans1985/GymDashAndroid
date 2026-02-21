package com.gymdash.companion.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HealthConnectDataSource @Inject constructor(
    private val healthConnectClient: HealthConnectClient?
) {
    val isAvailable: Boolean get() = healthConnectClient != null

    companion object {
        val RECORD_TYPES: Set<KClass<out Record>> = setOf(
            StepsRecord::class,
            HeartRateRecord::class,
            SleepSessionRecord::class,
            DistanceRecord::class,
            ActiveCaloriesBurnedRecord::class,
            TotalCaloriesBurnedRecord::class,
            FloorsClimbedRecord::class,
            WeightRecord::class,
            HeightRecord::class,
            BodyFatRecord::class,
            RestingHeartRateRecord::class,
            OxygenSaturationRecord::class,
            HeartRateVariabilityRmssdRecord::class,
            RespiratoryRateRecord::class,
            BloodPressureRecord::class,
            BodyTemperatureRecord::class,
            Vo2MaxRecord::class,
            BloodGlucoseRecord::class,
        )
    }

    private fun requireClient(): HealthConnectClient =
        healthConnectClient ?: throw IllegalStateException("Health Connect is not available")

    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant
    ): List<T> {
        val client = requireClient()
        val request = ReadRecordsRequest(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
    }

    suspend fun getChangesToken(): String {
        val client = requireClient()
        val request = ChangesTokenRequest(RECORD_TYPES)
        return client.getChangesToken(request)
    }

    suspend fun getChanges(token: String): ChangesResult {
        val client = requireClient()
        var nextToken = token
        val changes = mutableListOf<Change>()

        do {
            val response = client.getChanges(nextToken)
            changes.addAll(response.changes)
            nextToken = response.nextChangesToken
        } while (response.hasMore)

        val upsertedRecords = changes
            .filterIsInstance<UpsertionChange>()
            .map { it.record }

        return ChangesResult(
            records = upsertedRecords,
            nextToken = nextToken
        )
    }

    data class ChangesResult(
        val records: List<Record>,
        val nextToken: String
    )
}
