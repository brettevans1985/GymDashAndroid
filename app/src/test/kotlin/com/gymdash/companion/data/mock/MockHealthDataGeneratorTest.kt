package com.gymdash.companion.data.mock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockHealthDataGeneratorTest {

    private lateinit var generator: MockHealthDataGenerator

    @Before
    fun setup() {
        generator = MockHealthDataGenerator()
    }

    @Test
    fun `generated data is non-empty`() {
        val data = generator.generate()

        assertFalse(data.isEmpty)
        assertTrue(data.totalRecords > 0)
        assertTrue(data.heartRateReadings.isNotEmpty())
        assertTrue(data.sleepSessions.isNotEmpty())
        assertTrue(data.dailyActivitySummaries.isNotEmpty())
        assertTrue(data.spO2Readings.isNotEmpty())
        assertTrue(data.hrvReadings.isNotEmpty())
        assertTrue(data.weightReadings.isNotEmpty())
        assertTrue(data.respiratoryRateReadings.isNotEmpty())
        assertTrue(data.bloodPressureReadings.isNotEmpty())
        assertTrue(data.bodyTemperatureReadings.isNotEmpty())
        assertTrue(data.vo2MaxReadings.isNotEmpty())
        assertTrue(data.bloodGlucoseReadings.isNotEmpty())
        assertNotNull(data.heightCm)
    }

    @Test
    fun `heart rate values are within physiological range`() {
        val data = generator.generate()

        data.heartRateReadings.forEach { reading ->
            assertTrue("BPM ${reading.beatsPerMinute} out of range", reading.beatsPerMinute in 40..200)
        }
    }

    @Test
    fun `SpO2 values are within physiological range`() {
        val data = generator.generate()

        data.spO2Readings.forEach { reading ->
            assertTrue("SpO2 ${reading.spO2Percentage} out of range", reading.spO2Percentage in 90..100)
        }
    }

    @Test
    fun `blood pressure values are within physiological range`() {
        val data = generator.generate()

        data.bloodPressureReadings.forEach { reading ->
            assertTrue("Systolic ${reading.systolicMmHg} out of range", reading.systolicMmHg in 80..200)
            assertTrue("Diastolic ${reading.diastolicMmHg} out of range", reading.diastolicMmHg in 50..120)
        }
    }

    @Test
    fun `body temperature values are within physiological range`() {
        val data = generator.generate()

        data.bodyTemperatureReadings.forEach { reading ->
            assertTrue("Temp ${reading.temperatureCelsius} out of range", reading.temperatureCelsius in 35.0..38.0)
        }
    }

    @Test
    fun `blood glucose values are within physiological range`() {
        val data = generator.generate()

        data.bloodGlucoseReadings.forEach { reading ->
            assertTrue("Glucose ${reading.valueMmolL} out of range", reading.valueMmolL in 3.0..10.0)
        }
    }

    @Test
    fun `weight values are within realistic range`() {
        val data = generator.generate()

        data.weightReadings.forEach { reading ->
            assertTrue("Weight ${reading.weightKg} out of range", reading.weightKg in 30.0..200.0)
        }
    }

    @Test
    fun `generates 24 heart rate readings spanning last 24 hours`() {
        val data = generator.generate()

        assertEquals(24, data.heartRateReadings.size)
    }

    private fun assertEquals(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
