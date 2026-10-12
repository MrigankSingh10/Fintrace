package com.fintrace.app

import com.fintrace.app.data.repository.CategoryBreakdownIncomeRules
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBreakdownIncomeRulesTest {
    private val utc = ZoneId.of("UTC")

    @Test
    fun rangeIsSplitByLocalCalendarMonthsAndKeepsInclusivePartialBoundaries() {
        val start = Instant.parse("2025-12-31T23:30:00Z").toEpochMilli()
        val end = Instant.parse("2026-01-01T00:30:00Z").toEpochMilli()

        val months = CategoryBreakdownIncomeRules.monthsIntersecting(start, end, utc)

        assertEquals(listOf("2025-12", "2026-01"), months.map { it.monthYear })
        assertEquals(start, months.first().startTimestamp)
        assertEquals(Instant.parse("2025-12-31T23:59:59.999Z").toEpochMilli(), months.first().endTimestamp)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), months.last().startTimestamp)
        assertEquals(end, months.last().endTimestamp)
    }

    @Test
    fun canonicalAndPercentageBudgetTotalsApplyEachMonthsAdjustmentSeparately() {
        val months = CategoryBreakdownIncomeRules.monthsIntersecting(
            Instant.parse("2025-12-15T00:00:00Z").toEpochMilli(),
            Instant.parse("2026-01-15T23:59:59Z").toEpochMilli(),
            utc
        )
        val confirmed = listOf(1000.0, 500.0)
        val adjustments = mapOf("2025-12" to -200.0, "2026-01" to 300.0)

        assertEquals(1600.0, CategoryBreakdownIncomeRules.canonicalIncomeTotal(months, confirmed, adjustments), 0.0)
        assertEquals(1600.0, CategoryBreakdownIncomeRules.percentageBudgetIncomeTotal(months, confirmed, adjustments), 0.0)
    }

    @Test
    fun negativeMonthIsExcludedOnlyFromPercentageBudgetAndAdjustmentOnlyIsRetained() {
        val months = CategoryBreakdownIncomeRules.monthsIntersecting(
            Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(),
            Instant.parse("2026-02-28T23:59:59Z").toEpochMilli(),
            utc
        )
        val confirmed = listOf(0.0, -100.0)
        val adjustments = mapOf("2026-01" to 800.0, "2026-02" to -50.0)

        assertEquals(650.0, CategoryBreakdownIncomeRules.canonicalIncomeTotal(months, confirmed, adjustments), 0.0)
        assertEquals(800.0, CategoryBreakdownIncomeRules.percentageBudgetIncomeTotal(months, confirmed, adjustments), 0.0)

        val allNegative = listOf(-300.0, -50.0)
        assertTrue(CategoryBreakdownIncomeRules.canonicalIncomeTotal(months, allNegative, emptyMap()) < 0.0)
        assertEquals(0.0, CategoryBreakdownIncomeRules.percentageBudgetIncomeTotal(months, allNegative, emptyMap()), 0.0)
    }

    @Test
    fun reversedRangeHasNoIntersectingMonths() {
        assertTrue(CategoryBreakdownIncomeRules.monthsIntersecting(10L, 9L, utc).isEmpty())
    }
}
