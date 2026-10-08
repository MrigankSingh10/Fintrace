package com.fintrace.app

import com.fintrace.app.ui.dashboard.dashboardCalendarForMonth
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth
import java.util.Calendar

class DashboardMonthSelectionTest {
    @Test
    fun directSelectionSetsExactMonthAndResetsDayBeforeMonthChange() {
        val calendar = dashboardCalendarForMonth(YearMonth.of(2024, 2))
        assertEquals(2024, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, calendar.get(Calendar.MONTH))
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun selectedDecemberCanAdvanceAcrossYearBoundary() {
        val december = YearMonth.of(2026, 12)
        val next = dashboardCalendarForMonth(december.plusMonths(1))
        assertEquals(2027, next.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, next.get(Calendar.MONTH))
    }
}
