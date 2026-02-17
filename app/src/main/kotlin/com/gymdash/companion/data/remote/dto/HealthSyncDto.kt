package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthSyncRequest(
    val deviceId: String,
    val syncTimestamp: String,
    val metrics: List<HealthMetricDto>
)

@JsonClass(generateAdapter = true)
data class HealthMetricDto(
    val metricType: String,
    val value: Double,
    val unit: String,
    val recordedAt: String,
    val source: String,
    val metadata: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class HealthSyncResponse(
    val syncId: String,
    val recordsAccepted: Int,
    val recordsRejected: Int,
    val serverTimestamp: String
)

@JsonClass(generateAdapter = true)
data class StepsDto(
    val count: Long,
    val startTime: String,
    val endTime: String
)

@JsonClass(generateAdapter = true)
data class HeartRateDto(
    val bpm: Long,
    val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class SleepSessionDto(
    val startTime: String,
    val endTime: String,
    val stages: List<SleepStageDto>? = null
)

@JsonClass(generateAdapter = true)
data class SleepStageDto(
    val stage: String,
    val startTime: String,
    val endTime: String
)

@JsonClass(generateAdapter = true)
data class ExerciseSessionDto(
    val exerciseType: String,
    val startTime: String,
    val endTime: String,
    val calories: Double? = null,
    val distance: Double? = null
)

@JsonClass(generateAdapter = true)
data class BodyMeasurementDto(
    val measurementType: String,
    val value: Double,
    val unit: String,
    val recordedAt: String
)
