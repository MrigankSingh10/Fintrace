package com.fintrace.app

import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.formatSignedAdjustment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A manual adjustment is stored as a signed number, so its label has to make the direction
 * explicit. "₹-20,000.00" reads like a rendering bug; "+₹20,000.00" and "−₹20,000.00" do not.
 */
class CurrencyFormattingTest {

    @Test
    fun positiveAdjustmentIsPrefixedWithPlus() {
        assertEquals("+₹20,000.00", formatSignedAdjustment(20000.0))
        assertEquals("+₹1.00", formatSignedAdjustment(1.0))
    }

    @Test
    fun negativeAdjustmentUsesARealMinusSign() {
        val formatted = formatSignedAdjustment(-20000.0)

        assertEquals("−₹20,000.00", formatted)
        assertFalse("a hyphen-minus renders as a broken sign", formatted.contains("-"))
        assertTrue("the minus sign must not be ASCII", formatted.contains("\u2212"))
    }

    @Test
    fun zeroAdjustmentHasNoSign() {
        assertEquals(formatCurrency(0.0), formatSignedAdjustment(0.0))
    }

    @Test
    fun adjustmentAlwaysShowsTheAbsoluteMagnitude() {
        assertTrue(formatSignedAdjustment(-20000.0).endsWith("20,000.00"))
        assertTrue(formatSignedAdjustment(20000.0).endsWith("20,000.00"))
    }

    @Test
    fun otherCurrenciesAreSignedToo() {
        // Non-INR output goes through Locale.getDefault(), so only the sign contract is asserted.
        val positive = formatSignedAdjustment(100.0, "USD")
        val negative = formatSignedAdjustment(-100.0, "USD")

        assertTrue("expected a leading plus, got $positive", positive.startsWith("+"))
        assertTrue("expected a leading minus, got $negative", negative.startsWith("\u2212"))
        assertFalse(positive.contains("-"))
        assertFalse(negative.contains("-"))
        assertEquals(formatCurrency(100.0, "USD"), positive.removePrefix("+"))
        assertEquals(formatCurrency(100.0, "USD"), negative.removePrefix("\u2212"))
    }
}
