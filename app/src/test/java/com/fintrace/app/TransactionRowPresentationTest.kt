package com.fintrace.app

import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.transactionRowSubtitle
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionRowPresentationTest {
    @Test
    fun incomeRowsUseIncomeSubtitleAndHideCategoryAndPaymentDetail() {
        assertEquals(
            listOf("Income"),
            transactionRowSubtitle(TransactionType.INCOME, "Needs", "DEBIT", null)
        )
        assertEquals(
            listOf("Income", "11 Oct · 9:30 AM"),
            transactionRowSubtitle(TransactionType.INCOME, "Needs", "DEBIT", "11 Oct · 9:30 AM")
        )
    }

    @Test
    fun expenseAndTransferRowsKeepTheirExistingMetadata() {
        assertEquals(
            listOf("Needs", "DEBIT"),
            transactionRowSubtitle(TransactionType.EXPENSE, "Needs", "DEBIT", null)
        )
        assertEquals(
            listOf("Transfer", "Bank"),
            transactionRowSubtitle(TransactionType.TRANSFER, "Transfer", "Bank", null)
        )
    }
}
