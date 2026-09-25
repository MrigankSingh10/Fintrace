package com.fintrace.app

import com.fintrace.app.data.local.entity.CardMappingEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CardMappingTest {

    @Test
    fun testCardMappingEntityCreation() {
        val mapping = CardMappingEntity(
            id = 1,
            cardLastFour = "4321",
            paymentModeId = 10L,
            label = "My HDFC Card"
        )

        assertEquals(1L, mapping.id)
        assertEquals("4321", mapping.cardLastFour)
        assertEquals(10L, mapping.paymentModeId)
        assertEquals("My HDFC Card", mapping.label)
    }

    @Test
    fun testCardMappingLookupSimulation() {
        val mappings = listOf(
            CardMappingEntity(id = 1, cardLastFour = "4321", paymentModeId = 2L, label = "HDFC Neu"),
            CardMappingEntity(id = 2, cardLastFour = "9876", paymentModeId = 3L, label = "ICICI Coral")
        )

        val map = mappings.associateBy { it.cardLastFour }

        assertEquals(2L, map["4321"]?.paymentModeId)
        assertEquals(3L, map["9876"]?.paymentModeId)
        assertNull(map["0000"])
    }
}
