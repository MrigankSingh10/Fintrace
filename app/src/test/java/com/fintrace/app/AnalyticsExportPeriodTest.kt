package com.fintrace.app

import com.fintrace.app.ui.analytics.AnalyticsTimeframe
import com.fintrace.app.ui.analytics.analyticsExportPeriod
import com.fintrace.app.ui.analytics.specificMonthTimestampRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.time.YearMonth
import java.time.ZoneId

class AnalyticsExportPeriodTest {
    @Test
    fun selectedMonthUsesItsYearAcrossJanuaryRollover() {
        withUsUtc {
            val now = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 8, 12, 0, 0) }.timeInMillis
            assertEquals("December_2025" to "December 2025", analyticsExportPeriod(AnalyticsTimeframe.LAST_MONTH, now))
            assertEquals("January_2026" to "January 2026", analyticsExportPeriod(AnalyticsTimeframe.THIS_MONTH, now))
        }
    }

    @Test
    fun multiMonthAndAllTimeHaveExplicitNames() {
        withUsUtc {
            val now = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 10, 12, 0, 0) }.timeInMillis
            assertEquals(
                "Dec_10_2025-Mar_10_2026" to "Dec 10, 2025 – Mar 10, 2026",
                analyticsExportPeriod(AnalyticsTimeframe.LAST_3_MONTHS, now)
            )
            assertEquals("All_Time" to "All Time", analyticsExportPeriod(AnalyticsTimeframe.ALL_TIME, now))
        }
    }

    @Test
    fun specificMonthExportUsesCapturedMonthForNameAndLabel() {
        withUsUtc {
            val now = Calendar.getInstance().apply { set(2027, Calendar.JANUARY, 4, 12, 0, 0) }.timeInMillis
            assertEquals(
                "December_2026" to "December 2026",
                analyticsExportPeriod(AnalyticsTimeframe.SPECIFIC_MONTH, now, YearMonth.of(2026, 12))
            )
        }
    }

    @Test
    fun specificLeapFebruaryRangeIncludesLastMillisecondInLocalZone() {
        val range = specificMonthTimestampRange(YearMonth.of(2024, 2), ZoneId.of("Asia/Kolkata"))
        val marchStart = YearMonth.of(2024, 3).atDay(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
        assertEquals(29L * 24 * 60 * 60 * 1000, range.second - range.first + 1)
        assertEquals(marchStart, range.second + 1)
    }

    private fun withUsUtc(block: () -> Unit) {
        val previousLocale = Locale.getDefault()
        val previousTimezone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            block()
        } finally {
            Locale.setDefault(previousLocale)
            TimeZone.setDefault(previousTimezone)
        }
    }
}
