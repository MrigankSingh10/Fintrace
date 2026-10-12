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
        val futureDebitReminderPending = transaction(
            id = 7,
            status = TransactionStatus.PENDING,
            body = "ICICI Bank: Your Standing Instruction for Google Play Content Purchase Services of Rs.149.00 is due by 20-Sep-26 and will be debited from your account."
        )
        val validPending = transaction(
            id = 2,
            status = TransactionStatus.PENDING,
            body = "INR 500 debited from A/C XX857 at SWIGGY."
        )
        val completedDebitPending = transaction(
            id = 8,
            status = TransactionStatus.PENDING,
            body = "ICICI Bank: Rs.149.00 has been debited from your account for Google Play Content Purchase Services under your standing instruction."
        )
        val promotionalConfirmed = promotionalPending.copy(id = 3, status = TransactionStatus.CONFIRMED)
        val promotionalDismissed = promotionalPending.copy(id = 4, status = TransactionStatus.DISMISSED)
        val manualPending = promotionalPending.copy(id = 5, smsRawBody = null)
        val blankSmsPending = promotionalPending.copy(id = 6, smsRawBody = "")

        val invalid = SmsPendingCleanup.invalidRows(
            listOf(
                promotionalPending,
                futureDebitReminderPending,
                validPending,
                completedDebitPending,
                promotionalConfirmed,
                promotionalDismissed,
                manualPending,
                blankSmsPending
            )
        )

        assertEquals(listOf(1L, 7L), invalid.map { it.id })
        assertTrue(invalid.all { it.status == TransactionStatus.PENDING && it.smsRawBody != null })
    }

    @Test
    fun futureReminderCleanupIsLimitedToRejectedPendingSmsAndIsIdempotent() {
        val reminder = transaction(
            id = 10,
            status = TransactionStatus.PENDING,
            body = "ICICI Bank: Your Standing Instruction for Google Play Content Purchase Services of Rs.149.00 is due by 20-Sep-26 and will be debited from your account."
        )
        val promotional = transaction(11, TransactionStatus.PENDING, "Get ₹500 cashback on your HDFC card.")
        val completed = transaction(
            12,
            TransactionStatus.PENDING,
            "ICICI Bank: Rs.149.00 has been debited from your account for Google Play Content Purchase Services under your standing instruction."
        )
        val confirmedReminder = reminder.copy(id = 13, status = TransactionStatus.CONFIRMED)
        val dismissedReminder = reminder.copy(id = 14, status = TransactionStatus.DISMISSED)
        val manualReminder = reminder.copy(id = 15, smsRawBody = null)

        val candidates = SmsPendingCleanup.futureDebitReminderRows(
            listOf(reminder, promotional, completed, confirmedReminder, dismissedReminder, manualReminder)
        )

        assertEquals(listOf(10L), candidates.map { it.id })
        assertTrue(SmsPendingCleanup.futureDebitReminderRows(emptyList()).isEmpty())
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
