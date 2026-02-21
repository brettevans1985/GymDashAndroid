package com.gymdash.companion.data.mapper

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import com.gymdash.companion.data.remote.dto.DailyActivitySummarySync
import com.gymdash.companion.data.remote.dto.HeartRateReadingSync
import com.gymdash.companion.data.remote.dto.HrvReadingSync
import com.gymdash.companion.data.remote.dto.SleepSessionSync
import com.gymdash.companion.data.remote.dto.SpO2ReadingSync
import com.gymdash.companion.data.remote.dto.WeightReadingSync
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class MappedHealthData(
    val heartRateReadings: List<HeartRateReadingSync>,
    val sleepSessions: List<SleepSessionSync>,
    val dailyActivitySummaries: List<DailyActivitySummarySync>,
    val spO2Readings: List<SpO2ReadingSync>,
    val hrvReadings: List<HrvReadingSync>,
    val weightReadings: List<WeightReadingSync>
) {
    val isEmpty: Boolean get() = heartRateReadings.isEmpty() &&
            sleepSessions.isEmpty() &&
            dailyActivitySummaries.isEmpty() &&
            spO2Readings.isEmpty() &&
            hrvReadings.isEmpty() &&
            weightReadings.isEmpty()

    val totalRecords: Int get() = heartRateReadings.size +
            sleepSessions.size +
            dailyActivitySummaries.size +
            spO2Readings.size +
            hrvReadings.size +
            weightReadings.size
}

class HealthDataMapper @Inject constructor() {

    fun mapRecords(records: List<Record>): MappedHealthData {
        val heartRateReadings = mutableListOf<HeartRateReadingSync>()
        val sleepSessions = mutableListOf<SleepSessionSync>()
        val spO2Readings = mutableListOf<SpO2ReadingSync>()
        val hrvReadings = mutableListOf<HrvReadingSync>()
        val weightReadings = mutableListOf<WeightReadingSync>()

        // Accumulators for daily activity summaries (grouped by date)
        val dailySteps = mutableMapOf<LocalDate, Int>()
        val dailyDistance = mutableMapOf<LocalDate, Double>()
        val dailyActiveCalories = mutableMapOf<LocalDate, Int>()
        val dailyTotalCalories = mutableMapOf<LocalDate, Int>()
        val dailyFloorsClimbed = mutableMapOf<LocalDate, Int>()

        val zone = ZoneId.systemDefault()

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
                            spO2Percentage = record.percentage.value
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
            weightReadings = weightReadings
        )
    }
}
