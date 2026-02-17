package com.gymdash.companion.data.mapper

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
import com.gymdash.companion.data.remote.dto.HealthMetricDto
import javax.inject.Inject

class HealthDataMapper @Inject constructor() {

    fun mapRecords(records: List<Record>): List<HealthMetricDto> =
        records.flatMap { record -> mapRecord(record) }

    private fun mapRecord(record: Record): List<HealthMetricDto> = when (record) {
        is StepsRecord -> listOf(
            HealthMetricDto(
                metricType = "steps",
                value = record.count.toDouble(),
                unit = "count",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is HeartRateRecord -> record.samples.map { sample ->
            HealthMetricDto(
                metricType = "heart_rate",
                value = sample.beatsPerMinute.toDouble(),
                unit = "bpm",
                recordedAt = sample.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        }
        is SleepSessionRecord -> listOf(
            HealthMetricDto(
                metricType = "sleep",
                value = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                unit = "minutes",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName,
                metadata = mapOf(
                    "endTime" to record.endTime.toString()
                )
            )
        )
        is DistanceRecord -> listOf(
            HealthMetricDto(
                metricType = "distance",
                value = record.distance.inMeters,
                unit = "meters",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is ActiveCaloriesBurnedRecord -> listOf(
            HealthMetricDto(
                metricType = "active_calories",
                value = record.energy.inKilocalories,
                unit = "kcal",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is TotalCaloriesBurnedRecord -> listOf(
            HealthMetricDto(
                metricType = "total_calories",
                value = record.energy.inKilocalories,
                unit = "kcal",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is ExerciseSessionRecord -> listOf(
            HealthMetricDto(
                metricType = "exercise",
                value = java.time.Duration.between(record.startTime, record.endTime).toMinutes().toDouble(),
                unit = "minutes",
                recordedAt = record.startTime.toString(),
                source = record.metadata.dataOrigin.packageName,
                metadata = mapOf(
                    "exerciseType" to record.exerciseType.toString(),
                    "endTime" to record.endTime.toString()
                )
            )
        )
        is WeightRecord -> listOf(
            HealthMetricDto(
                metricType = "weight",
                value = record.weight.inKilograms,
                unit = "kg",
                recordedAt = record.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is HeightRecord -> listOf(
            HealthMetricDto(
                metricType = "height",
                value = record.height.inMeters,
                unit = "meters",
                recordedAt = record.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is BodyFatRecord -> listOf(
            HealthMetricDto(
                metricType = "body_fat",
                value = record.percentage.value,
                unit = "percent",
                recordedAt = record.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is RestingHeartRateRecord -> listOf(
            HealthMetricDto(
                metricType = "resting_heart_rate",
                value = record.beatsPerMinute.toDouble(),
                unit = "bpm",
                recordedAt = record.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        is OxygenSaturationRecord -> listOf(
            HealthMetricDto(
                metricType = "oxygen_saturation",
                value = record.percentage.value,
                unit = "percent",
                recordedAt = record.time.toString(),
                source = record.metadata.dataOrigin.packageName
            )
        )
        else -> emptyList()
    }
}
