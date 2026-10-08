package com.fintrace.app.ui.dashboard

import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.formatCurrency
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class DashboardPace(
    val summary: String,
    val chartActual: List<Double>,
    val chartIncome: List<Double>,
    val showIncomePace: Boolean
)

data class DashboardMonthUiState(
    val monthYearKey: String,
    val isLoading: Boolean,
    val summary: MonthlyFinancialSummary? = null,
    val budget: MonthlyBudgetAdjustmentEntity? = null,
    val categories: List<CategorySpendSummary> = emptyList(),
    val confirmedTransactions: List<TransactionWithDetails> = emptyList()
)

fun dashboardStateForMonth(state: DashboardMonthUiState, requestedMonth: YearMonth): DashboardMonthUiState? =
    state.takeIf { !it.isLoading && it.monthYearKey == requestedMonth.toString() }

fun dashboardLeftThisMonth(monthlyIncome: Double, totalMyShareSpent: Double): Double =
    monthlyIncome - totalMyShareSpent

fun dashboardSpendProgress(monthlyIncome: Double, totalMyShareSpent: Double): Float =
    if (monthlyIncome > 0.0) (totalMyShareSpent / monthlyIncome).toFloat().coerceIn(0f, 1f) else 0f

fun dashboardSpendPercent(monthlyIncome: Double, totalMyShareSpent: Double): Double? =
    if (monthlyIncome > 0.0) totalMyShareSpent / monthlyIncome * 100.0 else null

fun dashboardCategoryShare(categorySpend: Double, totalPersonalShareSpend: Double): Double =
    if (totalPersonalShareSpend > 0.0) categorySpend / totalPersonalShareSpend * 100.0 else 0.0

fun latestDashboardTransactions(rows: List<TransactionWithDetails>): List<TransactionWithDetails> = rows
    .asSequence()
    .filter { it.transaction.status == TransactionStatus.CONFIRMED }
    .sortedWith(compareByDescending<TransactionWithDetails> { it.transaction.timestamp }.thenByDescending { it.transaction.id })
    .take(5)
    .toList()

fun calculateDashboardPace(
    transactions: List<TransactionWithDetails>,
    selectedMonth: YearMonth,
    monthlyIncome: Double,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): DashboardPace {
    val todayMonth = YearMonth.from(today)
    val currentMonth = selectedMonth == todayMonth
    val futureMonth = selectedMonth.isAfter(todayMonth)
    val elapsedDays = when {
        currentMonth -> today.dayOfMonth
        futureMonth -> 0
        else -> selectedMonth.lengthOfMonth()
    }
    val endDate = when {
        currentMonth -> today
        futureMonth -> selectedMonth.atDay(1).minusDays(1)
        else -> selectedMonth.atEndOfMonth()
    }
    val dailySpend = HashMap<Int, Double>()
    transactions.forEach { row ->
        val transaction = row.transaction
        if (transaction.status != TransactionStatus.CONFIRMED || transaction.type != TransactionType.EXPENSE) return@forEach
        val date = java.time.Instant.ofEpochMilli(transaction.timestamp).atZone(zoneId).toLocalDate()
        if (YearMonth.from(date) != selectedMonth || date.isAfter(endDate)) return@forEach
        dailySpend[date.dayOfMonth] = (dailySpend[date.dayOfMonth] ?: 0.0) + transaction.myShareAmount
    }

    val actual = ArrayList<Double>(elapsedDays)
    var running = 0.0
    for (day in 1..elapsedDays) {
        running += dailySpend[day] ?: 0.0
        actual += running
    }
    val incomePace = if (monthlyIncome > 0.0) {
        (1..elapsedDays).map { day -> monthlyIncome * day / selectedMonth.lengthOfMonth() }
    } else emptyList()
    val dailyAverage = running / elapsedDays.coerceAtLeast(1)
    val projected = if (currentMonth) dailyAverage * selectedMonth.lengthOfMonth() else running
    val summary = buildString {
        if (futureMonth) {
            append("Upcoming month · no spending recorded yet")
        } else {
            append("Average spend ")
            append(formatPaceAmount(dailyAverage))
            append("/day · ")
            if (currentMonth) append("projected ") else append("final ")
            append(formatPaceAmount(projected))
        }
        if (monthlyIncome <= 0.0) append(" · Set income to compare pace")
        else if (monthlyIncome > 0.0 && !futureMonth) append(" · even income pace shown")
    }
    return DashboardPace(summary, actual, incomePace, monthlyIncome > 0.0)
}

private fun formatPaceAmount(value: Double): String =
    formatCurrency(value)
