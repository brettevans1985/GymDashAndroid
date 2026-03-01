package com.gymdash.companion.data.mock

import com.gymdash.companion.data.mapper.MappedHealthData
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
import com.gymdash.companion.data.remote.dto.WaterIntakeSync
import com.gymdash.companion.data.remote.dto.WeightReadingSync
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockHealthDataGenerator @Inject constructor() {

    fun generate(): MappedHealthData {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        return MappedHealthData(
            heartRateReadings = generateHeartRateReadings(now),
            sleepSessions = generateSleepSessions(today),
            dailyActivitySummaries = generateDailyActivity(today),
            spO2Readings = generateSpO2Readings(now),
            hrvReadings = generateHrvReadings(today),
            weightReadings = generateWeightReadings(now),
            respiratoryRateReadings = generateRespiratoryRateReadings(now),
            bloodPressureReadings = generateBloodPressureReadings(now),
            bodyTemperatureReadings = generateBodyTemperatureReadings(now),
            vo2MaxReadings = generateVo2MaxReadings(today),
            bloodGlucoseReadings = generateBloodGlucoseReadings(now),
            waterIntakes = listOf(
                WaterIntakeSync(
                    calendarDate = today.toString(),
                    amountMl = Random.nextInt(500, 2500)
                )
            ),
            heightCm = 175.0
        )
    }

    private fun generateHeartRateReadings(now: Instant): List<HeartRateReadingSync> {
        return (0 until 24).map { hoursAgo ->
            HeartRateReadingSync(
                timestamp = now.minusSeconds(hoursAgo * 3600L).toString(),
                beatsPerMinute = Random.nextInt(60, 101)
            )
        }
    }

    private fun generateSleepSessions(today: LocalDate): List<SleepSessionSync> {
        val sleepDate = today.minusDays(1)
        val durationSeconds = Random.nextInt(6 * 3600, 9 * 3600 + 1)
        val deepPct = 0.2
        val lightPct = 0.4
        val remPct = 0.25
        val awakePct = 0.15
        return listOf(
            SleepSessionSync(
                calendarDate = sleepDate.toString(),
                startTime = sleepDate.atTime(22, 30).atZone(ZoneId.systemDefault()).toInstant().toString(),
                endTime = today.atTime(6, 30).atZone(ZoneId.systemDefault()).toInstant().toString(),
                durationSeconds = durationSeconds,
                deepSleepSeconds = (durationSeconds * deepPct).toInt(),
                lightSleepSeconds = (durationSeconds * lightPct).toInt(),
                remSleepSeconds = (durationSeconds * remPct).toInt(),
                awakeSeconds = (durationSeconds * awakePct).toInt()
            )
        )
    }

    private fun generateDailyActivity(today: LocalDate): List<DailyActivitySummarySync> {
        return listOf(
            DailyActivitySummarySync(
                calendarDate = today.toString(),
                steps = Random.nextInt(5000, 12001),
                distanceMeters = Random.nextDouble(3000.0, 9000.0),
                activeCalories = Random.nextInt(200, 601),
                totalCalories = Random.nextInt(1800, 2800),
                floorsClimbed = Random.nextInt(2, 16)
            )
        )
    }

    private fun generateSpO2Readings(now: Instant): List<SpO2ReadingSync> {
        return listOf(
            SpO2ReadingSync(
                timestamp = now.minusSeconds(3600).toString(),
                spO2Percentage = Random.nextInt(95, 100)
            )
        )
    }

    private fun generateHrvReadings(today: LocalDate): List<HrvReadingSync> {
        return listOf(
            HrvReadingSync(
                calendarDate = today.toString(),
                hrvRmssd = "%.2f".format(Random.nextDouble(20.0, 80.0)).toDouble()
            )
        )
    }

    private fun generateWeightReadings(now: Instant): List<WeightReadingSync> {
        return listOf(
            WeightReadingSync(
                measuredAt = now.minusSeconds(7200).toString(),
                weightKg = Random.nextDouble(60.0, 100.0),
                bodyFatPercent = Random.nextDouble(10.0, 30.0)
            )
        )
    }

    private fun generateRespiratoryRateReadings(now: Instant): List<RespiratoryRateReadingSync> {
        return listOf(
            RespiratoryRateReadingSync(
                timestamp = now.minusSeconds(3600).toString(),
                breathsPerMinute = Random.nextDouble(12.0, 20.0)
            )
        )
    }

    private fun generateBloodPressureReadings(now: Instant): List<BloodPressureReadingSync> {
        return listOf(
            BloodPressureReadingSync(
                timestamp = now.minusSeconds(3600).toString(),
                systolicMmHg = Random.nextInt(110, 140),
                diastolicMmHg = Random.nextInt(70, 90)
            )
        )
    }

    private fun generateBodyTemperatureReadings(now: Instant): List<BodyTemperatureReadingSync> {
        return listOf(
            BodyTemperatureReadingSync(
                timestamp = now.minusSeconds(3600).toString(),
                temperatureCelsius = Random.nextDouble(36.1, 37.2)
            )
        )
    }

    private fun generateVo2MaxReadings(today: LocalDate): List<Vo2MaxReadingSync> {
        return listOf(
            Vo2MaxReadingSync(
                calendarDate = today.toString(),
                vo2MaxMlKgMin = Random.nextDouble(30.0, 55.0)
            )
        )
    }

    private fun generateBloodGlucoseReadings(now: Instant): List<BloodGlucoseReadingSync> {
        return listOf(
            BloodGlucoseReadingSync(
                timestamp = now.minusSeconds(3600).toString(),
                valueMmolL = Random.nextDouble(4.0, 7.0)
            )
        )
    }
}
