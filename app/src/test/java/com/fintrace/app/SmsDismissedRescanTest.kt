package com.fintrace.app

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.SmsSourceIdentity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmsDismissedRescanTest {

    @Test
    fun dismissedSmsRetainsIdentityAndCanBeExplicitlyRestored() {
        val dismissed = smsTransaction(status = TransactionStatus.DISMISSED)
        val rescannedIdentity = SmsSourceIdentity(
            body = dismissed.smsRawBody!!,
            sender = dismissed.smsSender,
            sourceTimestamp = dismissed.smsSourceTimestamp!!,
            receivedTimestamp = dismissed.timestamp
        )
        val originalIdentity = SmsSourceIdentity(
            body = "Bank debit alert",
            sender = "HDFCBK",
            sourceTimestamp = 1_700_000_000_000L,
            receivedTimestamp = 1_700_000_000_000L
        )

        assertEquals(originalIdentity, rescannedIdentity)
        assertEquals(TransactionStatus.DISMISSED, dismissed.status)
        assertEquals(TransactionStatus.PENDING, dismissed.copy(status = TransactionStatus.PENDING).status)
    }

    @Test
    fun sameSmsBodyAndSenderWithDifferentSourceTimestampIsDifferentIdentity() {
        val original = SmsSourceIdentity(
            body = "Bank debit alert",
            sender = "HDFCBK",
            sourceTimestamp = 1_700_000_000_000L,
            receivedTimestamp = 1_700_000_000_000L
        )
        val laterMessage = original.copy(
            sourceTimestamp = original.sourceTimestamp + 60_000L,
            receivedTimestamp = original.receivedTimestamp + 60_000L
        )

        assertNotEquals(original, laterMessage)
    }

    private fun smsTransaction(status: TransactionStatus) = TransactionEntity(
        id = 9L,
        description = "Bank debit alert",
        timestamp = 1_700_000_000_000L,
        originalAmount = 200.0,
        myShareAmount = 200.0,
        categoryId = 1L,
        paymentModeId = 1L,
        type = TransactionType.EXPENSE,
        status = status,
        smsRawBody = "Bank debit alert",
        smsSender = "HDFCBK",
        smsSourceTimestamp = 1_700_000_000_000L
    )
}
