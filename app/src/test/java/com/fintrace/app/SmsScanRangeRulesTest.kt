package com.fintrace.app

import com.fintrace.app.data.sms.SmsScanBounds
import com.fintrace.app.data.sms.SmsScanRangeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

class SmsScanRangeRulesTest {
    @Test
    fun incrementalRangeStartsAtCheckpointAndIncludesCapturedUpperBound() {
        assertEquals(
            SmsScanBounds(startInclusive = 1_000L, endInclusive = 5_000L),
            SmsScanRangeRules.incremental(checkpoint = 1_000L, upperBound = 5_000L)
        )
    }

    @Test
    fun firstIncrementalScanFallsBackToLastThirtyDays() {
        val upperBound = 4_000_000_000L
        assertEquals(
            SmsScanBounds(
                startInclusive = upperBound - 30L * 24L * 60L * 60L * 1000L,
                endInclusive = upperBound
            ),
            SmsScanRangeRules.incremental(checkpoint = null, upperBound = upperBound)
        )
    }

    @Test
    fun historicalRescanDoesNotChangeStoredCheckpointAndCheckpointIsMonotonic() {
        val oldCheckpoint = 9_000L
        val historical = SmsScanRangeRules.historical(upperBound = 10_000L)
        assertEquals(10_000L, historical.endInclusive)
        assertEquals(10_000L, SmsScanRangeRules.checkpointAfterSuccessfulIncrementalScan(oldCheckpoint, 10_000L))
    }

    @Test
    fun futureCheckpointFallsBackToThirtyDaysAndRebasesAfterSuccess() {
        val upperBound = 10_000L
        val futureCheckpoint = 20_000L
        val bounds = SmsScanRangeRules.incremental(futureCheckpoint, upperBound)

        assertEquals(upperBound - 30L * 24L * 60L * 60L * 1000L, bounds.startInclusive)
        assertEquals(upperBound, bounds.endInclusive)
        assertEquals(upperBound, SmsScanRangeRules.checkpointAfterSuccessfulIncrementalScan(futureCheckpoint, upperBound))
    }

    @Test
    fun futureDatedSmsIsOutsideCapturedScanRangeUntilALaterUpperBound() {
        val capturedUpperBound = 10_000L
        val futureSmsDate = 10_001L
        val bounds = SmsScanRangeRules.incremental(checkpoint = 9_000L, upperBound = capturedUpperBound)

        assertFalse(futureSmsDate in bounds.startInclusive..bounds.endInclusive)
        val laterBounds = SmsScanRangeRules.incremental(checkpoint = capturedUpperBound, upperBound = futureSmsDate)
        assertEquals(futureSmsDate, laterBounds.endInclusive)
        assertTrue(futureSmsDate in laterBounds.startInclusive..laterBounds.endInclusive)
    }

    @Test
    fun lastYearUsesCalendarYearAcrossLeapDayAndPreservesTimeOfDay() {
        val zone = TimeZone.getTimeZone("UTC")
        val upper = Calendar.getInstance(zone).apply {
            clear()
            set(2024, Calendar.FEBRUARY, 29, 10, 15, 30)
        }.timeInMillis

        val bounds = SmsScanRangeRules.lastYear(upper, zone)
        val expectedStart = Calendar.getInstance(zone).apply {
            clear()
            set(2023, Calendar.FEBRUARY, 28, 10, 15, 30)
        }.timeInMillis
        assertEquals(expectedStart, bounds.startInclusive)
        assertEquals(upper, bounds.endInclusive)
    }

    @Test
    fun customStartOfDayUsesDeviceTimezoneAndRejectsFutureOrInvalidDate() {
        val zone = TimeZone.getTimeZone("Asia/Kolkata")
        val start = SmsScanRangeRules.customStartOfDay(2026, Calendar.OCTOBER, 17, zone)
        val expected = Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.OCTOBER, 17, 0, 0, 0)
        }.timeInMillis
        assertEquals(expected, start)

        val upperBound = Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.OCTOBER, 17, 12, 0, 0)
        }.timeInMillis
        assertFalse(SmsScanRangeRules.isFutureLocalDate(2026, Calendar.OCTOBER, 17, upperBound, zone))
        assertTrue(SmsScanRangeRules.isFutureLocalDate(2026, Calendar.OCTOBER, 18, upperBound, zone))
        assertEquals(null, SmsScanRangeRules.customStartOfDay(2026, Calendar.FEBRUARY, 30, zone))
    }

    @Test
    fun customStartOfDayResolvesMidnightDstGapToFirstValidInstant() {
        val zone = TimeZone.getTimeZone("America/Sao_Paulo")
        val expected = LocalDate.of(2018, 11, 4)
            .atStartOfDay(ZoneId.of("America/Sao_Paulo"))
            .toInstant()
            .toEpochMilli()

        assertEquals(
            expected,
            SmsScanRangeRules.customStartOfDay(2018, Calendar.NOVEMBER, 4, zone)
        )
        assertEquals(
            1,
            Instant.ofEpochMilli(expected).atZone(ZoneId.of("America/Sao_Paulo")).hour
        )
    }

    @Test
    fun historicalRangeLeavesIncrementalCheckpointValueUntouched() {
        val checkpoint = 9_000L
        val historical = SmsScanRangeRules.lastYear(upperBound = 10_000L)
        assertTrue(historical.startInclusive < historical.endInclusive)
        assertFalse(SmsScanRangeRules.shouldAdvanceCheckpoint(isIncrementalScan = false))
        assertTrue(SmsScanRangeRules.shouldAdvanceCheckpoint(isIncrementalScan = true))
        assertEquals(9_000L, checkpoint)
    }
}
