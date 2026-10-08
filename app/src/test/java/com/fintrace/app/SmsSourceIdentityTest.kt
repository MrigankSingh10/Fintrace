package com.fintrace.app

import com.fintrace.app.data.model.SmsSourceIdentity
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsSourceIdentityTest {
    @Test
    fun identityIncludesBodySenderAndSourceTimestamp() {
        val original = SmsSourceIdentity("INR 500 spent", "BANK", 2000L, 1000L)
        assertEquals(original, SmsSourceIdentity("INR 500 spent", "BANK", 2000L, 1000L))
        assertNotEquals(original, SmsSourceIdentity("INR 500 spent", "BANK", 3000L, 1000L))
        assertNotEquals(original, SmsSourceIdentity("INR 500 spent", "OTHER", 2000L, 1000L))
        assertNotEquals(original, SmsSourceIdentity("INR 600 spent", "BANK", 2000L, 1000L))
    }

    @Test
    fun legacyRowsMatchEitherReceiverSentTimeOrScannerReceivedTime() {
        val identity = SmsSourceIdentity("INR 500 spent", "BANK", 2000L, 1000L)
        assertEquals(true, identity.matchesLegacyTransactionTimestamp(1000L))
        assertEquals(true, identity.matchesLegacyTransactionTimestamp(2000L))
        assertEquals(false, identity.matchesLegacyTransactionTimestamp(3000L))
    }
}
