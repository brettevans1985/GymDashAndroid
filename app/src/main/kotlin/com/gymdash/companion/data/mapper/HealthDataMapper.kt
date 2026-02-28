package com.gymdash.companion.data.mapper

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
import com.gymdash.companion.domain.model.HealthMetric
import com.gymdash.companion.data.remote.dto.BloodGlucoseReadingSync
import com.gymdash.companion.data.remote.dto.BloodPressureReadingSync
import com.gymdash.companion.data.remote.dto.BodyTemperatureReadingSync
import com.gymdash.companion.data.remote.dto.DailyActivitySummarySync
import com.gymdash.companion.data.remote.dto.HeartRateReadingSync
import com.gymdash.companion.data.remote.dto.HrvReadingSync
import com.gymdash.companion.data.remote.dto.RespiratoryRateReadingSync
import com.gymdash.companion.data.remote.dto.SleepSessionSync
import com.gymdash.companion.data.remote.dto.SpO2ReadingSync
import com.gymdash.companion.data.remote.dto.Vo2MaxReadingSync
import com.gymdash.companion.data.remote.dto.WeightReadingSync
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MappedHealthData(
    val heartRateReadings: List<HeartRateReadingSync>,
    val sleepSessions: List<SleepSessionSync>,
    val dailyActivitySummaries: List<DailyActivitySummarySync>,
    val spO2Readings: List<SpO2ReadingSync>,
    val hrvReadings: List<HrvReadingSync>,
    val weightReadings: List<WeightReadingSync>,
    val respiratoryRateReadings: List<RespiratoryRateReadingSync>,
    val bloodPressureReadings: List<BloodPressureReadingSync>,
    val bodyTemperatureReadings: List<BodyTemperatureReadingSync>,
    val vo2MaxReadings: List<Vo2MaxReadingSync>,
    val bloodGlucoseReadings: List<BloodGlucoseReadingSync>,
    val heightCm: Double? = null
) {
    val isEmpty: Boolean get() = heartRateReadings.isEmpty() &&
            sleepSessions.isEmpty() &&
            dailyActivitySummaries.isEmpty() &&
            spO2Readings.isEmpty() &&
            hrvReadings.isEmpty() &&
            weightReadings.isEmpty() &&
            respiratoryRateReadings.isEmpty() &&
            bloodPressureReadings.isEmpty() &&
            bodyTemperatureReadings.isEmpty() &&
            vo2MaxReadings.isEmpty() &&
            bloodGlucoseReadings.isEmpty()

    val totalRecords: Int get() = heartRateReadings.size +
            sleepSessions.size +
            dailyActivitySummaries.size +
            spO2Readings.size +
            hrvReadings.size +
            weightReadings.size +
            respiratoryRateReadings.size +
            bloodPressureReadings.size +
            bodyTemperatureReadings.size +
            vo2MaxReadings.size +
            bloodGlucoseReadings.size

    fun filterByMetrics(selected: Set<HealthMetric>): MappedHealthData = copy(
        heartRateReadings = if (HealthMetric.HEART_RATE in selected) heartRateReadings else emptyList(),
        sleepSessions = if (HealthMetric.SLEEP in selected) sleepSessions else emptyList(),
        dailyActivitySummaries = if (HealthMetric.DAILY_ACTIVITY in selected) dailyActivitySummaries else emptyList(),
        spO2Readings = if (HealthMetric.SPO2 in selected) spO2Readings else emptyList(),
        hrvReadings = if (HealthMetric.HRV in selected) hrvReadings else emptyList(),
        weightReadings = if (HealthMetric.WEIGHT in selected) weightReadings else emptyList(),
        respiratoryRateReadings = if (HealthMetric.RESPIRATORY_RATE in selected) respiratoryRateReadings else emptyList(),
        bloodPressureReadings = if (HealthMetric.BLOOD_PRESSURE in selected) bloodPressureReadings else emptyList(),
        bodyTemperatureReadings = if (HealthMetric.BODY_TEMPERATURE in selected) bodyTemperatureReadings else emptyList(),
        vo2MaxReadings = if (HealthMetric.VO2_MAX in selected) vo2MaxReadings else emptyList(),
        bloodGlucoseReadings = if (HealthMetric.BLOOD_GLUCOSE in selected) bloodGlucoseReadings else emptyList(),
        heightCm = if (HealthMetric.HEIGHT in selected) heightCm else null
    )
}

class HealthDataMapper @Inject constructor() {

    fun mapRecords(records: List<Record>): MappedHealthData {
        val heartRateReadings = mutableListOf<HeartRateReadingSync>()
        val sleepSessions = mutableListOf<SleepSessionSync>()
        val spO2Readings = mutableListOf<SpO2ReadingSync>()
        val hrvReadings = mutableListOf<HrvReadingSync>()
        val weightReadings = mutableListOf<WeightReadingSync>()
        val respiratoryRateReadings = mutableListOf<RespiratoryRateReadingSync>()
        val bloodPressureReadings = mutableListOf<BloodPressureReadingSync>()
        val bodyTemperatureReadings = mutableListOf<BodyTemperatureReadingSync>()
        val vo2MaxReadings = mutableListOf<Vo2MaxReadingSync>()
        val bloodGlucoseReadings = mutableListOf<BloodGlucoseReadingSync>()

        // Accumulators for daily activity summaries (grouped by date)
        val dailySteps = mutableMapOf<LocalDate, Int>()
        val dailyDistance = mutableMapOf<LocalDate, Double>()
        val dailyActiveCalories = mutableMapOf<LocalDate, Int>()
        val dailyTotalCalories = mutableMapOf<LocalDate, Int>()
        val dailyFloorsClimbed = mutableMapOf<LocalDate, Int>()

        val zone = ZoneId.systemDefault()

        var latestHeightCm: Double? = null
        var latestHeightTime: Instant? = null

        for (record in records) {
            when (record) {
                is HeartRateRecord -> {
                    for (sample in record.samples) {
                        heartRateReadings.add(
                            HeartRateReadingSync(
                                timestamp = sample.time.toString(),
                                beatsPerMinute = sample.beatsPerMinute.toInt()
                            )
                        )
                    }
                }
                is RestingHeartRateRecord -> {
                    heartRateReadings.add(
                        HeartRateReadingSync(
                            timestamp = record.time.toString(),
                            beatsPerMinute = record.beatsPerMinute.toInt(),
                            context = "Resting"
                        )
                    )
                }
                is SleepSessionRecord -> {
                    val duration = Duration.between(record.startTime, record.endTime)
                    val calendarDate = record.startTime.atZone(zone).toLocalDate()

                    var deepSeconds = 0
                    var lightSeconds = 0
                    var remSeconds = 0
                    var awakeSeconds = 0

                    for (stage in record.stages) {
                        val stageDuration = Duration.between(stage.startTime, stage.endTime).seconds.toInt()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deepSeconds += stageDuration
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> lightSeconds += stageDuration
                            SleepSessionRecord.STAGE_TYPE_REM -> remSeconds += stageDuration
                            SleepSessionRecord.STAGE_TYPE_AWAKE -> awakeSeconds += stageDuration
                        }
                    }

                    sleepSessions.add(
                        SleepSessionSync(
                            calendarDate = calendarDate.toString(),
                            startTime = record.startTime.toString(),
                            endTime = record.endTime.toString(),
                            durationSeconds = duration.seconds.toInt(),
                            deepSleepSeconds = deepSeconds,
                            lightSleepSeconds = lightSeconds,
                            remSleepSeconds = remSeconds,
                            awakeSeconds = awakeSeconds
                        )
                    )
                }
                is StepsRecord -> {
                    val date = record.startTime.atZone(zone).toLocalDate()
                    dailySteps[date] = (dailySteps[date] ?: 0) + record.count.toInt()
                }
                is DistanceRecord -> {
                    val date = record.startTime.atZone(zone).toLocalDate()
                    dailyDistance[date] = (dailyDistance[date] ?: 0.0) + record.distance.inMeters
                }
                is ActiveCaloriesBurnedRecord -> {
                    val date = record.startTime.atZone(zone).toLocalDate()
                    dailyActiveCalories[date] = (dailyActiveCalories[date] ?: 0) + record.energy.inKilocalories.toInt()
                }
                is TotalCaloriesBurnedRecord -> {
                    val date = record.startTime.atZone(zone).toLocalDate()
                    dailyTotalCalories[date] = (dailyTotalCalories[date] ?: 0) + record.energy.inKilocalories.toInt()
                }
                is FloorsClimbedRecord -> {
                    val date = record.startTime.atZone(zone).toLocalDate()
                    dailyFloorsClimbed[date] = (dailyFloorsClimbed[date] ?: 0) + record.floors.toInt()
                }
                is OxygenSaturationRecord -> {
                    spO2Readings.add(
                        SpO2ReadingSync(
                            timestamp = record.time.toString(),
                            spO2Percentage = record.percentage.value.toInt()
                        )
                    )
                }
                is HeartRateVariabilityRmssdRecord -> {
                    val date = record.time.atZone(zone).toLocalDate()
                    hrvReadings.add(
                        HrvReadingSync(
                            calendarDate = date.toString(),
                            hrvRmssd = record.heartRateVariabilityMillis
                        )
                    )
                }
                is RespiratoryRateRecord -> {
                    respiratoryRateReadings.add(
                        RespiratoryRateReadingSync(
                            timestamp = record.time.toString(),
                            breathsPerMinute = record.rate
                        )
                    )
                }
                is BloodPressureRecord -> {
                    bloodPressureReadings.add(
                        BloodPressureReadingSync(
                            timestamp = record.time.toString(),
                            systolicMmHg = record.systolic.inMillimetersOfMercury.toInt(),
                            diastolicMmHg = record.diastolic.inMillimetersOfMercury.toInt()
                        )
                    )
                }
                is BodyTemperatureRecord -> {
                    bodyTemperatureReadings.add(
                        BodyTemperatureReadingSync(
                            timestamp = record.time.toString(),
                            temperatureCelsius = record.temperature.inCelsius
                        )
                    )
                }
                is Vo2MaxRecord -> {
                    val date = record.time.atZone(zone).toLocalDate()
                    vo2MaxReadings.add(
                        Vo2MaxReadingSync(
                            calendarDate = date.toString(),
                            vo2MaxMlKgMin = record.vo2MillilitersPerMinuteKilogram
                        )
                    )
                }
                is BloodGlucoseRecord -> {
                    bloodGlucoseReadings.add(
                        BloodGlucoseReadingSync(
                            timestamp = record.time.toString(),
                            valueMmolL = record.level.inMillimolesPerLiter
                        )
                    )
                }
                is WeightRecord -> {
                    weightReadings.add(
                        WeightReadingSync(
                            measuredAt = record.time.toString(),
                            weightKg = record.weight.inKilograms
                        )
                    )
                }
                is BodyFatRecord -> {
                    // Body fat is attached to weight readings if timestamps match;
                    // otherwise send as a standalone weight reading with body fat only
                    val existing = weightReadings.find { it.measuredAt == record.time.toString() }
                    if (existing != null) {
                        val index = weightReadings.indexOf(existing)
                        weightReadings[index] = existing.copy(bodyFatPercent = record.percentage.value)
                    }
                }
                is HeightRecord -> {
                    if (latestHeightTime == null || record.time > latestHeightTime) {
                        latestHeightTime = record.time
                        latestHeightCm = record.height.inMeters * 100.0
                    }
                }
            }
        }

        // Build daily activity summaries from accumulated data
        val allDates = (dailySteps.keys + dailyDistance.keys + dailyActiveCalories.keys + dailyTotalCalories.keys + dailyFloorsClimbed.keys).toSet()
        val dailyActivitySummaries = allDates.map { date ->
            DailyActivitySummarySync(
                calendarDate = date.toString(),
                steps = dailySteps[date] ?: 0,
                distanceMeters = dailyDistance[date] ?: 0.0,
                activeCalories = dailyActiveCalories[date] ?: 0,
                totalCalories = dailyTotalCalories[date] ?: 0,
                floorsClimbed = dailyFloorsClimbed[date] ?: 0
            )
        }

        return MappedHealthData(
            heartRateReadings = heartRateReadings,
            sleepSessions = sleepSessions,
            dailyActivitySummaries = dailyActivitySummaries,
            spO2Readings = spO2Readings,
            hrvReadings = hrvReadings,
            weightReadings = weightReadings,
            respiratoryRateReadings = respiratoryRateReadings,
            bloodPressureReadings = bloodPressureReadings,
            bodyTemperatureReadings = bodyTemperatureReadings,
            vo2MaxReadings = vo2MaxReadings,
            bloodGlucoseReadings = bloodGlucoseReadings,
            heightCm = latestHeightCm
        )
    }
}
