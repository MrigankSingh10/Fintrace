package com.fintrace.app

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.transactionAmountDisplay
import com.fintrace.app.ui.dashboard.DashboardMonthUiState
import com.fintrace.app.ui.dashboard.dashboardStateForMonth
import com.fintrace.app.ui.dashboard.calculateDashboardPace
import com.fintrace.app.ui.dashboard.dashboardCategoryShare
import com.fintrace.app.ui.dashboard.dashboardLeftThisMonth
import com.fintrace.app.ui.dashboard.dashboardSpendProgress
import com.fintrace.app.ui.dashboard.dashboardSpendPercent
import com.fintrace.app.ui.dashboard.latestDashboardTransactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DashboardPresentationTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun leftBalanceAndProgressUseIncomeOnceAndClampDenominatorCases() {
        assertEquals(500.0, dashboardLeftThisMonth(1500.0, 1000.0), 0.0)
        assertEquals(-500.0, dashboardLeftThisMonth(500.0, 1000.0), 0.0)
        assertEquals(1f, dashboardSpendProgress(100.0, 500.0))
        assertEquals(0f, dashboardSpendProgress(0.0, 500.0))
        assertEquals(0f, dashboardSpendProgress(-50.0, 500.0))
        assertEquals(150.0, dashboardSpendPercent(100.0, 150.0)!!, 0.0)
        assertEquals(-10.0, dashboardSpendPercent(100.0, -10.0)!!, 0.0)
        assertEquals(null, dashboardSpendPercent(0.0, 500.0))
        assertEquals(null, dashboardSpendPercent(-50.0, 500.0))
    }

    @Test
    fun currentMonthPaceCountsExpenseMyShareAndExcludesFutureRows() {
        val today = LocalDate.of(2024, 2, 10)
        val rows = listOf(
            row(1, today.minusDays(1), 100.0),
            row(2, today, 200.0),
            row(3, today.plusDays(1), 5000.0),
            row(4, today, 4000.0, type = TransactionType.INCOME),
            row(5, today, 9000.0, status = TransactionStatus.PENDING)
        )
        val pace = calculateDashboardPace(rows, YearMonth.of(2024, 2), 6200.0, today, zone)
        assertEquals(10, pace.chartActual.size)
        assertEquals(300.0, pace.chartActual.last(), 0.0)
        assertEquals(6200.0 * 10.0 / 29.0, pace.chartIncome.last(), 0.0001)
        assertTrue(pace.summary.contains("projected"))
        assertTrue(pace.showIncomePace)
    }

    @Test
    fun historicalPaceUsesCompletedMonthAndLeapYearLength() {
        val month = YearMonth.of(2024, 2)
        val rows = listOf(row(1, LocalDate.of(2024, 2, 29), 290.0))
        val pace = calculateDashboardPace(rows, month, 0.0, LocalDate.of(2024, 3, 1), zone)
        assertEquals(29, pace.chartActual.size)
        assertEquals(290.0, pace.chartActual.last(), 0.0)
        assertTrue(pace.summary.contains("final"))
        assertTrue(pace.summary.contains("Set income"))
        assertFalse(pace.summary.contains("On track"))
    }

    @Test
    fun paceSeparatesYearBoundaryAndRejectsRowsFromAdjacentMonth() {
        val rows = listOf(
            row(1, LocalDate.of(2023, 12, 31), 100.0),
            row(2, LocalDate.of(2024, 1, 1), 200.0)
        )
        val pace = calculateDashboardPace(rows, YearMonth.of(2023, 12), 3100.0, LocalDate.of(2024, 1, 1), zone)
        assertEquals(100.0, pace.chartActual.last(), 0.0)
        assertEquals(3100.0, pace.chartIncome.last(), 0.0)
    }

    @Test
    fun pacePreservesNegativeCumulativeRefunds() {
        val today = LocalDate.of(2024, 2, 5)
        val pace = calculateDashboardPace(
            transactions = listOf(row(1, today, -120.0)),
            selectedMonth = YearMonth.of(2024, 2),
            monthlyIncome = 1000.0,
            today = today,
            zoneId = zone
        )
        assertEquals(-120.0, pace.chartActual.last(), 0.0)
        assertTrue(pace.summary.contains("-₹24"))
    }

    @Test
    fun latestFiveAreTimestampThenIdDescendingAndConfirmedOnly() {
        val rows = (1L..8L).map { id ->
            val date = LocalDate.of(2026, 6, 1).plusDays(id)
            row(id, date, id.toDouble()).copy(
                transaction = row(id, date, id.toDouble()).transaction.copy(
                    timestamp = if (id == 6L || id == 8L) 1000L else id * 100L,
                    status = if (id == 7L) TransactionStatus.PENDING else TransactionStatus.CONFIRMED
                )
            )
        }
        assertEquals(listOf(8L, 6L, 5L, 4L, 3L), latestDashboardTransactions(rows).map { it.transaction.id })
    }

    @Test
    fun categoryShareUsesTotalPersonalShareSpendRatherThanIncome() {
        assertEquals(25.0, dashboardCategoryShare(250.0, 1000.0), 0.0)
        assertEquals(0.0, dashboardCategoryShare(250.0, 0.0), 0.0)
        assertEquals(150.0, dashboardCategoryShare(1500.0, 1000.0), 0.0)
        assertEquals(-20.0, dashboardCategoryShare(-200.0, 1000.0), 0.0)
        assertEquals(0.0, dashboardCategoryShare(200.0, -1000.0), 0.0)
    }

    @Test
    fun incomeAdjustmentSignFollowsAmountAndTransactionType() {
        assertEquals("−", transactionAmountDisplay(TransactionType.EXPENSE, 25.0).sign)
        assertEquals("+", transactionAmountDisplay(TransactionType.EXPENSE, -25.0).sign)
        assertEquals(25.0, transactionAmountDisplay(TransactionType.EXPENSE, -25.0).amount, 0.0)
        assertEquals("+", transactionAmountDisplay(TransactionType.INCOME, 25.0).sign)
        assertEquals("−", transactionAmountDisplay(TransactionType.INCOME, -25.0).sign)
        assertEquals(null, transactionAmountDisplay(TransactionType.TRANSFER, -25.0).sign)
    }

    @Test
    fun delayedMonthDataDoesNotExposePreviousMonthsHomeState() = runBlocking {
        val january = dashboardState("2024-01")
        val februaryLoading = DashboardMonthUiState("2024-02", isLoading = true)
        val februaryReady = dashboardState("2024-02")
        val states = flow {
            emit(january)
            emit(februaryLoading)
            delay(10)
            emit(februaryReady)
        }.toList()

        assertEquals(null, dashboardStateForMonth(states[0], YearMonth.of(2024, 2)))
        assertEquals(null, dashboardStateForMonth(states[1], YearMonth.of(2024, 2)))
        assertEquals(februaryReady, dashboardStateForMonth(states[2], YearMonth.of(2024, 2)))
    }

    private fun row(
        id: Long,
        date: LocalDate,
        myShare: Double,
        type: TransactionType = TransactionType.EXPENSE,
        status: TransactionStatus = TransactionStatus.CONFIRMED
    ): TransactionWithDetails {
        val timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return TransactionWithDetails(
            transaction = TransactionEntity(
                id = id,
                description = "Row $id",
                timestamp = timestamp,
                originalAmount = myShare,
                myShareAmount = myShare,
                categoryId = 1,
                paymentModeId = 1,
                type = type,
                status = status
            ),
            category = null,
            paymentMode = null
        )
    }

    private fun dashboardState(monthKey: String) = DashboardMonthUiState(
        monthYearKey = monthKey,
        isLoading = false,
        summary = com.fintrace.app.data.local.relation.MonthlyFinancialSummary(
            monthYear = monthKey,
            monthlyIncome = 1000.0,
            totalMyShareSpent = 250.0,
            totalOriginalSpent = 250.0,
            remainingBalance = 750.0
        )
    )
}
