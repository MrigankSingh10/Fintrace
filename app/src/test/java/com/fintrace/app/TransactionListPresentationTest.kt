package com.fintrace.app

import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.transactions.TransactionListFilter
import com.fintrace.app.ui.transactions.buildTransactionListPresentation
import com.fintrace.app.ui.transactions.groupTransactionsByLocalDay
import com.fintrace.app.ui.transactions.matchesTransactionFilter
import com.fintrace.app.ui.transactions.summarizeExpensesByCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TransactionListPresentationTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun row(id: Long, date: LocalDate, amount: Double, type: TransactionType = TransactionType.EXPENSE,
                    currency: String = "INR", recurring: Boolean = false, category: Long = 1, mode: Long = 1,
                    original: Double = amount) = TransactionWithDetails(
        transaction = TransactionEntity(id = id, description = "Row $id", timestamp = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            originalAmount = original, myShareAmount = amount, categoryId = category, paymentModeId = mode, type = type,
            currency = currency, isRecurring = recurring),
        category = CategoryEntity(id = category, name = "Food", colorHex = "#00AA88"),
        paymentMode = PaymentModeEntity(id = mode, name = "UPI")
    )

    private fun rowAtTimestamp(id: Long, timestamp: Long, recurring: Boolean) = TransactionWithDetails(
        transaction = TransactionEntity(
            id = id,
            description = "Row $id",
            timestamp = timestamp,
            originalAmount = 10.0,
            myShareAmount = 10.0,
            categoryId = 1,
            paymentModeId = 1,
            isRecurring = recurring
        ),
        category = CategoryEntity(id = 1, name = "Food", colorHex = "#00AA88"),
        paymentMode = PaymentModeEntity(id = 1, name = "UPI")
    )

    @Test fun filtersCombineAndIncomeDoesNotMatchExpenseCategory() {
        val split = row(1, LocalDate.of(2026, 1, 1), 50.0, original = 100.0, category = 4, mode = 7)
        val income = row(2, LocalDate.of(2026, 1, 1), 500.0, TransactionType.INCOME, category = 4, mode = 7)
        val filter = TransactionListFilter(categoryIds = setOf(4), paymentModeIds = setOf(7), type = TransactionType.EXPENSE, hasSplit = true)
        assertTrue(matchesTransactionFilter(split, filter))
        assertFalse(matchesTransactionFilter(income, filter))
        assertFalse(matchesTransactionFilter(split, filter.copy(paymentModeIds = setOf(8))))
    }

    @Test fun localDayGroupingHandlesYearBoundaryRecurringOrderAndSignedNets() {
        val dec31 = LocalDate.of(2025, 12, 31)
        val jan1 = LocalDate.of(2026, 1, 1)
        val rows = listOf(
            row(1, dec31, 80.0, recurring = false),
            row(2, dec31, -20.0, recurring = true), // expense refund is a positive contribution
            row(3, dec31, 200.0, TransactionType.INCOME),
            row(4, jan1, 30.0, currency = "USD"),
            row(5, jan1, 10.0, currency = "INR", type = TransactionType.TRANSFER)
        )
        val groups = groupTransactionsByLocalDay(rows, zone)
        assertEquals(listOf(jan1, dec31), groups.map { it.date })
        assertTrue(groups.first().hasMixedCurrencies)
        assertEquals(0.0, groups.first().netByCurrency["INR"]!!, 0.0)
        assertEquals(-30.0, groups.first().netByCurrency["USD"]!!, 0.0)
        assertEquals(140.0, groups.last().netByCurrency["INR"]!!, 0.0)
        assertEquals(listOf(2L, 1L, 3L), groups.last().transactions.map { it.transaction.id })
    }

    @Test fun recurringSectionPrecedesOrdinaryLocalDateGroupsWithoutLosingRows() {
        val dec31 = LocalDate.of(2025, 12, 31)
        val jan1 = LocalDate.of(2026, 1, 1)
        val rows = listOf(
            row(1, dec31, 80.0),
            row(2, dec31, 20.0, recurring = true),
            row(3, jan1, 30.0),
            row(4, jan1, 40.0, recurring = true)
        )

        val presentation = buildTransactionListPresentation(rows, zone)
        val displayedIds = presentation.recurringTransactions.map { it.transaction.id } +
            presentation.dayGroups.flatMap { group -> group.transactions.map { it.transaction.id } }

        assertEquals(listOf(4L, 2L), presentation.recurringTransactions.map { it.transaction.id })
        assertEquals(listOf(jan1, dec31), presentation.dayGroups.map { it.date })
        assertEquals(listOf(listOf(3L), listOf(1L)), presentation.dayGroups.map { group -> group.transactions.map { it.transaction.id } })
        assertEquals(rows.map { it.transaction.id }.sorted(), displayedIds.sorted())
        assertEquals(displayedIds.size, displayedIds.toSet().size)
    }

    @Test fun recurringPresentationHandlesEmptyAndAllRecurringInputs() {
        val empty = buildTransactionListPresentation(emptyList(), zone)
        assertTrue(empty.recurringTransactions.isEmpty())
        assertTrue(empty.dayGroups.isEmpty())

        val allRecurring = buildTransactionListPresentation(listOf(
            row(1, LocalDate.of(2025, 12, 31), 10.0, recurring = true),
            row(2, LocalDate.of(2026, 1, 1), 20.0, recurring = true)
        ), zone)
        assertEquals(listOf(2L, 1L), allRecurring.recurringTransactions.map { it.transaction.id })
        assertTrue(allRecurring.dayGroups.isEmpty())
    }

    @Test fun ordinaryGroupsUseTheRequestedLocalZoneAtYearBoundary() {
        val justAfterUtcNewYear = Instant.parse("2026-01-01T00:30:00Z").toEpochMilli()
        val zone = ZoneId.of("America/Los_Angeles")
        val presentation = buildTransactionListPresentation(listOf(
            rowAtTimestamp(1, justAfterUtcNewYear, recurring = false),
            rowAtTimestamp(2, justAfterUtcNewYear, recurring = true)
        ), zone)

        assertEquals(listOf(2L), presentation.recurringTransactions.map { it.transaction.id })
        assertEquals(listOf(LocalDate.of(2025, 12, 31)), presentation.dayGroups.map { it.date })
    }

    @Test fun expenseSummaryNeverCombinesCurrencies() {
        val rows = listOf(
            row(1, LocalDate.of(2026, 2, 1), 100.0, currency = "INR", original = 150.0),
            row(2, LocalDate.of(2026, 2, 1), 20.0, currency = "USD", original = 25.0),
            row(3, LocalDate.of(2026, 2, 1), 10.0, TransactionType.INCOME, currency = "EUR")
        )
        val totals = summarizeExpensesByCurrency(rows).byCurrency
        assertEquals(listOf("INR", "USD"), totals.map { it.currencyCode })
        assertEquals(100.0, totals[0].totalMyShareSpent, 0.0)
        assertEquals(150.0, totals[0].totalOriginalCharged, 0.0)
        assertEquals(20.0, totals[1].totalMyShareSpent, 0.0)
    }
}
