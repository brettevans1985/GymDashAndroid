package com.gymdash.companion.domain.model

import com.gymdash.companion.data.mapper.MappedHealthData

enum class HealthMetric(val displayName: String) {
    HEART_RATE("Heart Rate"),
    SLEEP("Sleep"),
    DAILY_ACTIVITY("Daily Activity"),
    SPO2("SpO2"),
    HRV("Heart Rate Variability"),
    WEIGHT("Weight"),
    RESPIRATORY_RATE("Respiratory Rate"),
    BLOOD_PRESSURE("Blood Pressure"),
    BODY_TEMPERATURE("Body Temperature"),
    VO2_MAX("VO2 Max"),
    BLOOD_GLUCOSE("Blood Glucose"),
    HEIGHT("Height");

    fun countIn(data: MappedHealthData): Int = when (this) {
        HEART_RATE -> data.heartRateReadings.size
        SLEEP -> data.sleepSessions.size
        DAILY_ACTIVITY -> data.dailyActivitySummaries.size
        SPO2 -> data.spO2Readings.size
        HRV -> data.hrvReadings.size
        WEIGHT -> data.weightReadings.size
        RESPIRATORY_RATE -> data.respiratoryRateReadings.size
        BLOOD_PRESSURE -> data.bloodPressureReadings.size
        BODY_TEMPERATURE -> data.bodyTemperatureReadings.size
        VO2_MAX -> data.vo2MaxReadings.size
        BLOOD_GLUCOSE -> data.bloodGlucoseReadings.size
        HEIGHT -> if (data.heightCm != null) 1 else 0
    }
}
