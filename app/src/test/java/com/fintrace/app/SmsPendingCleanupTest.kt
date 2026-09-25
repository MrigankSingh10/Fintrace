package com.fintrace.app

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.sms.SmsPendingCleanup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPendingCleanupTest {

    @Test
    fun cleanupSelectsOnlyInvalidPendingSmsImports() {
        val promotionalPending = transaction(
            id = 1,
            status = TransactionStatus.PENDING,
            body = "Get ₹500 cashback on your HDFC card."
        )
        val validPending = transaction(
            id = 2,
            status = TransactionStatus.PENDING,
            body = "INR 500 debited from A/C XX857 at SWIGGY."
        )
        val promotionalConfirmed = promotionalPending.copy(id = 3, status = TransactionStatus.CONFIRMED)
        val promotionalDismissed = promotionalPending.copy(id = 4, status = TransactionStatus.DISMISSED)
        val manualPending = promotionalPending.copy(id = 5, smsRawBody = null)

        val invalid = SmsPendingCleanup.invalidRows(
            listOf(promotionalPending, validPending, promotionalConfirmed, promotionalDismissed, manualPending)
        )

        assertEquals(listOf(1L), invalid.map { it.id })
        assertTrue(invalid.all { it.status == TransactionStatus.PENDING && it.smsRawBody != null })
    }

    private fun transaction(id: Long, status: TransactionStatus, body: String?): TransactionEntity =
        TransactionEntity(
            id = id,
            description = "Test",
            timestamp = 1L,
            originalAmount = 500.0,
            myShareAmount = 500.0,
            categoryId = 1L,
            paymentModeId = 1L,
            type = TransactionType.EXPENSE,
            smsRawBody = body,
            smsSender = "HDFCBK",
            status = status
        )
}
