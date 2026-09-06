package com.fintrace.app

import com.fintrace.app.data.local.converter.Converters
import com.fintrace.app.data.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionStatusConverterTest {
    @Test
    fun dismissedStatusRoundTripsAsText() {
        val converters = Converters()
        assertEquals("DISMISSED", converters.fromTransactionStatus(TransactionStatus.DISMISSED))
        assertEquals(TransactionStatus.DISMISSED, converters.toTransactionStatus("DISMISSED"))
    }
}
