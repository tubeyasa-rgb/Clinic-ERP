package com.minecorerp.clinicerp

import com.minecorerp.clinicerp.data.remote.models.AppointmentTime
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class AppointmentTimeTest {
    @Test fun midnightRollsIntoNextDay() {
        val result = AppointmentTime.utcRange("2026-10-27", "23:30", "120", ZoneId.of("UTC"))
        assertEquals("2026-10-27 23:30:00", result.first)
        assertEquals("2026-10-28 01:30:00", result.second)
    }
    @Test fun convertsCairoSummerTimeToUtc() {
        assertEquals("2026-07-01 06:00:00", AppointmentTime.utcRange("2026-07-01", "09:00", "30", ZoneId.of("Africa/Cairo")).first)
    }
    @Test fun convertsCairoWinterTimeToUtc() {
        assertEquals("2026-12-01 07:00:00", AppointmentTime.utcRange("2026-12-01", "09:00", "30", ZoneId.of("Africa/Cairo")).first)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsZeroDuration() { AppointmentTime.utcRange("2026-10-27", "09:00", "0") }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonNumericDuration() { AppointmentTime.utcRange("2026-10-27", "09:00", "abc") }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeDuration() { AppointmentTime.utcRange("2026-10-27", "09:00", "-30") }
    @Test(expected = java.time.format.DateTimeParseException::class) fun rejectsInvalidTime() { AppointmentTime.utcRange("2026-10-27", "25:00", "30") }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonexistentDstTime() { AppointmentTime.utcRange("2026-03-08", "02:30", "30", ZoneId.of("America/New_York")) }
}
