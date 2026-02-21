package com.gymdash.companion.data.mapper

import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class HealthDataMapperTest {

    private lateinit var mapper: HealthDataMapper

    @Before
    fun setup() {
        mapper = HealthDataMapper()
    }

    @Test
    fun `empty records list returns empty MappedHealthData`() {
        val result = mapper.mapRecords(emptyList())

        assertTrue(result.isEmpty)
        assertEquals(0, result.totalRecords)
        assertTrue(result.heartRateReadings.isEmpty())
        assertTrue(result.sleepSessions.isEmpty())
        assertTrue(result.dailyActivitySummaries.isEmpty())
        assertTrue(result.spO2Readings.isEmpty())
        assertTrue(result.hrvReadings.isEmpty())
        assertTrue(result.weightReadings.isEmpty())
        assertNull(result.heightCm)
    }

    @Test
    fun `HeartRateRecord mapping produces correct BPM and timestamp`() {
        val timestamp = Instant.parse("2025-01-15T10:30:00Z")
        val record = mockk<HeartRateRecord> {
            every { samples } returns listOf(
                HeartRateRecord.Sample(time = timestamp, beatsPerMinute = 72)
            )
        }

        val result = mapper.mapRecords(listOf(record))

        assertEquals(1, result.heartRateReadings.size)
        assertEquals(72, result.heartRateReadings[0].beatsPerMinute)
        assertEquals(timestamp.toString(), result.heartRateReadings[0].timestamp)
    }

    @Test
    fun `SleepSessionRecord mapping produces correct duration and stages`() {
        val start = Instant.parse("2025-01-15T22:00:00Z")
        val end = Instant.parse("2025-01-16T06:00:00Z")
        val deepStart = Instant.parse("2025-01-15T23:00:00Z")
        val deepEnd = Instant.parse("2025-01-16T01:00:00Z")

        val record = mockk<SleepSessionRecord> {
            every { startTime } returns start
            every { endTime } returns end
            every { stages } returns listOf(
                SleepSessionRecord.Stage(
                    startTime = deepStart,
                    endTime = deepEnd,
                    stage = SleepSessionRecord.STAGE_TYPE_DEEP
                )
            )
        }

        val result = mapper.mapRecords(listOf(record))

        assertEquals(1, result.sleepSessions.size)
        val session = result.sleepSessions[0]
        assertEquals(28800, session.durationSeconds) // 8 hours
        assertEquals(7200, session.deepSleepSeconds) // 2 hours
        assertEquals(0, session.lightSleepSeconds)
        assertEquals(0, session.remSleepSeconds)
        assertEquals(0, session.awakeSeconds)
    }

    @Test
    fun `multiple StepsRecords on same date are summed`() {
        val date1Start = Instant.parse("2025-01-15T08:00:00Z")
        val date2Start = Instant.parse("2025-01-15T12:00:00Z")

        val records: List<Record> = listOf(
            mockk<StepsRecord> {
                every { startTime } returns date1Start
                every { count } returns 3000
            },
            mockk<StepsRecord> {
                every { startTime } returns date2Start
                every { count } returns 5000
            }
        )

        val result = mapper.mapRecords(records)

        assertEquals(1, result.dailyActivitySummaries.size)
        assertEquals(8000, result.dailyActivitySummaries[0].steps)
    }

    @Test
    fun `WeightRecord and BodyFatRecord merge by timestamp`() {
        val timestamp = Instant.parse("2025-01-15T08:00:00Z")

        val records: List<Record> = listOf(
            mockk<WeightRecord> {
                every { time } returns timestamp
                every { weight } returns Mass.kilograms(80.0)
            },
            mockk<BodyFatRecord> {
                every { time } returns timestamp
                every { percentage } returns Percentage(20.0)
            }
        )

        val result = mapper.mapRecords(records)

        assertEquals(1, result.weightReadings.size)
        assertEquals(80.0, result.weightReadings[0].weightKg, 0.01)
        assertEquals(20.0, result.weightReadings[0].bodyFatPercent!!, 0.01)
    }

    @Test
    fun `HeightRecord picks latest value`() {
        val earlier = Instant.parse("2025-01-10T08:00:00Z")
        val later = Instant.parse("2025-01-15T08:00:00Z")

        val records: List<Record> = listOf(
            mockk<HeightRecord> {
                every { time } returns earlier
                every { height } returns Length.meters(1.70)
            },
            mockk<HeightRecord> {
                every { time } returns later
                every { height } returns Length.meters(1.75)
            }
        )

        val result = mapper.mapRecords(records)

        assertEquals(175.0, result.heightCm!!, 0.01)
    }
}
