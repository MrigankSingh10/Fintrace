package com.fintrace.app

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.ui.transactions.sortedForPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionPresentationOrderingTest {

    @Test
    fun recurringTransactionsComeFirstAndEachGroupIsNewestFirst() {
        val transactions = listOf(
            item(id = 1, recurring = false, timestamp = 500L),
            item(id = 2, recurring = true, timestamp = 200L),
            item(id = 3, recurring = false, timestamp = 800L),
            item(id = 4, recurring = true, timestamp = 900L)
        )

        assertEquals(listOf(4L, 2L, 3L, 1L), transactions.sortedForPresentation().map { it.transaction.id })
    }

    @Test
    fun orderingPreservesSourceOrderForTimestampTiesAndHandlesUniformOrEmptyLists() {
        val tieItems = listOf(
            item(id = 2, recurring = true, timestamp = 100L),
            item(id = 1, recurring = true, timestamp = 100L)
        )

        assertEquals(listOf(2L, 1L), tieItems.sortedForPresentation().map { it.transaction.id })
        assertEquals(emptyList<Long>(), emptyList<TransactionWithDetails>().sortedForPresentation().map { it.transaction.id })
        assertEquals(listOf(3L, 4L), listOf(
            item(id = 4, recurring = false, timestamp = 10L),
            item(id = 3, recurring = false, timestamp = 20L)
        ).sortedForPresentation().map { it.transaction.id })
    }

    private fun item(id: Long, recurring: Boolean, timestamp: Long) = TransactionWithDetails(
        transaction = TransactionEntity(
            id = id,
            description = "Transaction $id",
            timestamp = timestamp,
            originalAmount = 1.0,
            myShareAmount = 1.0,
            categoryId = 1,
            paymentModeId = 1,
            isRecurring = recurring
        ),
        category = null,
        paymentMode = null
    )
}
