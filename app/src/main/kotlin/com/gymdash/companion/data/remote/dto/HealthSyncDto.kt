package com.gymdash.companion.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthSyncRequest(
    val deviceName: String,
    val heartRateReadings: List<HeartRateReadingSync> = emptyList(),
    val sleepSessions: List<SleepSessionSync> = emptyList(),
    val dailyActivitySummaries: List<DailyActivitySummarySync> = emptyList(),
    val spO2Readings: List<SpO2ReadingSync> = emptyList(),
    val hrvReadings: List<HrvReadingSync> = emptyList(),
    val weightReadings: List<WeightReadingSync> = emptyList(),
    val respiratoryRateReadings: List<RespiratoryRateReadingSync> = emptyList(),
    val bloodPressureReadings: List<BloodPressureReadingSync> = emptyList(),
    val bodyTemperatureReadings: List<BodyTemperatureReadingSync> = emptyList(),
    val vo2MaxReadings: List<Vo2MaxReadingSync> = emptyList(),
    val bloodGlucoseReadings: List<BloodGlucoseReadingSync> = emptyList(),
    val waterIntakes: List<WaterIntakeSync> = emptyList(),
    val heightCm: Double? = null
)

@JsonClass(generateAdapter = true)
data class HeartRateReadingSync(
    val timestamp: String,
    val beatsPerMinute: Int,
    val context: String = "Unknown"
)

@JsonClass(generateAdapter = true)
data class SleepSessionSync(
    val calendarDate: String,
    val startTime: String,
    val endTime: String,
    val durationSeconds: Int,
    val deepSleepSeconds: Int = 0,
    val lightSleepSeconds: Int = 0,
    val remSleepSeconds: Int = 0,
    val awakeSeconds: Int = 0,
    val sleepScore: Int? = null
)

@JsonClass(generateAdapter = true)
data class DailyActivitySummarySync(
    val calendarDate: String,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val floorsClimbed: Int = 0,
    val activeTimeSeconds: Int = 0
)

@JsonClass(generateAdapter = true)
data class SpO2ReadingSync(
    val timestamp: String,
    val spO2Percentage: Int
)

@JsonClass(generateAdapter = true)
data class HrvReadingSync(
    val calendarDate: String,
    val hrvRmssd: Double
)

@JsonClass(generateAdapter = true)
data class WeightReadingSync(
    val measuredAt: String,
    val weightKg: Double,
    val bodyFatPercent: Double? = null,
    val bmi: Double? = null
)

@JsonClass(generateAdapter = true)
data class RespiratoryRateReadingSync(
    val timestamp: String,
    val breathsPerMinute: Double
)

@JsonClass(generateAdapter = true)
data class BloodPressureReadingSync(
    val timestamp: String,
    val systolicMmHg: Int,
    val diastolicMmHg: Int
)

@JsonClass(generateAdapter = true)
data class BodyTemperatureReadingSync(
    val timestamp: String,
    val temperatureCelsius: Double
)

@JsonClass(generateAdapter = true)
data class Vo2MaxReadingSync(
    val calendarDate: String,
    val vo2MaxMlKgMin: Double
)

@JsonClass(generateAdapter = true)
data class BloodGlucoseReadingSync(
    val timestamp: String,
    val valueMmolL: Double
)

@JsonClass(generateAdapter = true)
data class WaterIntakeSync(
    val calendarDate: String,
    val amountMl: Int
)

@JsonClass(generateAdapter = true)
data class HealthSyncResponse(
    val recordsProcessed: Int,
    val recordsCreated: Int,
    val recordsUpdated: Int,
    val dataTypesReceived: List<String>,
    val syncedAt: String
)
