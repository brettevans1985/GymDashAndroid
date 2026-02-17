package com.gymdash.companion.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
    private val healthConnectClient: HealthConnectClient
) {
    companion object {
        val RECORD_TYPES: Set<KClass<out Record>> = setOf(
            StepsRecord::class,
            HeartRateRecord::class,
            SleepSessionRecord::class,
            DistanceRecord::class,
            ActiveCaloriesBurnedRecord::class,
            TotalCaloriesBurnedRecord::class,
            ExerciseSessionRecord::class,
            WeightRecord::class,
            HeightRecord::class,
            BodyFatRecord::class,
            RestingHeartRateRecord::class,
            OxygenSaturationRecord::class,
        )
    }

    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant
    ): List<T> {
        val request = ReadRecordsRequest(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return healthConnectClient.readRecords(request).records
    }

    suspend fun getChangesToken(): String {
        val request = ChangesTokenRequest(RECORD_TYPES)
        return healthConnectClient.getChangesToken(request)
    }

    suspend fun getChanges(token: String): ChangesResult {
        var nextToken = token
        val changes = mutableListOf<Change>()

        do {
            val response = healthConnectClient.getChanges(nextToken)
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
