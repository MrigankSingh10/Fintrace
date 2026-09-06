package com.fintrace.app

import com.fintrace.app.ui.transactions.transactionTimestampRange
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMonthRangeTest {
    @Test
    fun leapFebruaryIncludesLastMillisecondAndExcludesMarch() {
        val zone = ZoneId.of("Asia/Kolkata")
        val range = YearMonth.of(2024, 2).transactionTimestampRange(zone)
        assertEquals(29L * 24 * 60 * 60 * 1000, range.last - range.first + 1)
        assertEquals(YearMonth.of(2024, 3).transactionTimestampRange(zone).first, range.last + 1)
    }

    @Test
    fun decemberEndsAtJanuaryBoundary() {
        val zone = ZoneId.of("Asia/Kolkata")
        val range = YearMonth.of(2026, 12).transactionTimestampRange(zone)
        assertEquals(YearMonth.of(2027, 1).transactionTimestampRange(zone).first, range.last + 1)
    }

    @Test
    fun daylightSavingMonthUsesLocalMidnightBoundaries() {
        val range = YearMonth.of(2026, 3).transactionTimestampRange(ZoneId.of("America/New_York"))
        assertEquals((31L * 24 - 1) * 60 * 60 * 1000, range.last - range.first + 1)
    }
}
