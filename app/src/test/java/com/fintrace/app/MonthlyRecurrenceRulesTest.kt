package com.fintrace.app

import com.fintrace.app.data.repository.nextMonthlyTimestamp
import com.fintrace.app.data.repository.recurringMonthKey
import com.fintrace.app.data.repository.timestampForRecurringMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MonthlyRecurrenceRulesTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `next month preserves local day and time`() {
        val start = LocalDateTime.of(2026, 10, 8, 9, 45).atZone(zone).toInstant().toEpochMilli()

        val next = nextMonthlyTimestamp(start, zoneId = zone)

        assertEquals(
            LocalDateTime.of(2026, 11, 8, 9, 45),
            java.time.Instant.ofEpochMilli(next).atZone(zone).toLocalDateTime()
        )
        assertEquals("2026-11", recurringMonthKey(next, zone))
    }

    @Test
    fun `month end clamps to last valid day`() {
        val start = LocalDateTime.of(2027, 1, 31, 18, 0).atZone(zone).toInstant().toEpochMilli()

        val next = nextMonthlyTimestamp(start, zoneId = zone)

        assertEquals(
            LocalDateTime.of(2027, 2, 28, 18, 0),
            java.time.Instant.ofEpochMilli(next).atZone(zone).toLocalDateTime()
        )
    }

    @Test
    fun `month end returns to anchor day after a short month`() {
        val january = LocalDateTime.of(2027, 1, 31, 18, 0).atZone(zone).toInstant().toEpochMilli()
        val february = nextMonthlyTimestamp(january, anchorDayOfMonth = 31, zoneId = zone)

        val march = nextMonthlyTimestamp(february, anchorDayOfMonth = 31, zoneId = zone)

        assertEquals(
            LocalDateTime.of(2027, 3, 31, 18, 0),
            java.time.Instant.ofEpochMilli(march).atZone(zone).toLocalDateTime()
        )
    }

    @Test
    fun `december occurrence rolls into next year`() {
        val start = LocalDateTime.of(2026, 12, 15, 12, 0).atZone(zone).toInstant().toEpochMilli()

        val next = nextMonthlyTimestamp(start, zoneId = zone)

        assertEquals("2027-01", recurringMonthKey(next, zone))
    }

    @Test
    fun `stored target month advances even when timezone changes the previous local month`() {
        val originalZone = ZoneId.of("Asia/Kolkata")
        val newZone = ZoneId.of("UTC")
        val previous = LocalDateTime.of(2026, 11, 1, 0, 30)
            .atZone(originalZone)
            .toInstant()
            .toEpochMilli()

        val december = timestampForRecurringMonth(
            previousTimestamp = previous,
            month = java.time.YearMonth.of(2026, 12),
            anchorDayOfMonth = 1,
            zoneId = newZone
        )

        assertEquals("2026-12", recurringMonthKey(december, newZone))
    }
}
