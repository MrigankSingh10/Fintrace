package com.fintrace.app

import com.fintrace.app.data.local.DatabaseSeeder
import com.fintrace.app.data.model.PaymentModeType
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseSeederTest {

    @Test
    fun defaultPaymentModesContainGenericDebitCreditCardAndCash() {
        assertEquals(listOf(1L, 2L, 5L), DatabaseSeeder.defaultPaymentModes.map { it.id })
        assertEquals(listOf("DEBIT", "Credit Card", "CASH"), DatabaseSeeder.defaultPaymentModes.map { it.name })
        assertEquals(
            listOf(PaymentModeType.BANK_DEBIT, PaymentModeType.CREDIT_CARD, PaymentModeType.CASH),
            DatabaseSeeder.defaultPaymentModes.map { it.type }
        )
    }
}
