package com.fintrace.app.data.repository

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A local-calendar month clipped to the inclusive breakdown range. */
internal data class CategoryBreakdownMonth(
    val monthYear: String,
    val startTimestamp: Long,
    val endTimestamp: Long
)

/**
 * Pure month/range and income calculations for category breakdowns. Timestamps use the
 * app-local timezone and the DAO's inclusive `timestamp >= start AND timestamp <= end` rule.
 */
internal object CategoryBreakdownIncomeRules {
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun monthsIntersecting(
        startTimestamp: Long,
        endTimestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<CategoryBreakdownMonth> {
        if (endTimestamp < startTimestamp) return emptyList()
        val start = Instant.ofEpochMilli(startTimestamp).atZone(zoneId).toLocalDate()
        val end = Instant.ofEpochMilli(endTimestamp).atZone(zoneId).toLocalDate()
        var month = YearMonth.from(start)
        val lastMonth = YearMonth.from(end)
        val result = mutableListOf<CategoryBreakdownMonth>()
        while (month <= lastMonth) {
            val monthStart = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val nextMonthStart = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            result += CategoryBreakdownMonth(
                monthYear = monthFormatter.format(month),
                startTimestamp = maxOf(startTimestamp, monthStart),
                endTimestamp = minOf(endTimestamp, nextMonthStart - 1L)
            )
            month = month.plusMonths(1)
        }
        return result
    }

    /** Sum signed canonical monthly income (clipped confirmed income plus full adjustment). */
    fun canonicalIncomeTotal(
        months: List<CategoryBreakdownMonth>,
        confirmedIncomeByMonth: List<Double>,
        adjustmentByMonth: Map<String, Double>
    ): Double = months.indices.sumOf { index ->
        confirmedIncomeByMonth.getOrElse(index) { 0.0 } + (adjustmentByMonth[months[index].monthYear] ?: 0.0)
    }

    /** Percentage budgets are summed month by month; a nonpositive month contributes zero. */
    fun percentageBudgetIncomeTotal(
        months: List<CategoryBreakdownMonth>,
        confirmedIncomeByMonth: List<Double>,
        adjustmentByMonth: Map<String, Double>
    ): Double = months.indices.sumOf { index ->
        (confirmedIncomeByMonth.getOrElse(index) { 0.0 } + (adjustmentByMonth[months[index].monthYear] ?: 0.0))
            .coerceAtLeast(0.0)
    }
}
