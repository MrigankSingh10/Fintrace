package com.fintrace.app

import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatMoneyTest {
    private val india = Locale("en", "IN")

    @Test
    fun formatsIndianGroupingForInr() {
        assertEquals("₹1,23,456.78", formatMoney(123456.78, "INR", india))
        assertEquals("₹12,34,56,789.00", formatMoney(123456789.0, "INR", india))
    }

    @Test
    fun formatsZeroNegativeAndRoundsUsingCurrencyPrecision() {
        assertEquals("₹0.00", formatMoney(0.0, "INR", india))
        assertEquals("-₹1,234.50", formatMoney(-1234.5, "INR", india))
        assertEquals("₹2.00", formatMoney(1.999, "INR", india))
    }

    @Test
    fun formatsOtherCurrenciesWithLocaleRules() {
        assertEquals("$1,234.56", formatMoney(1234.56, "USD", Locale.US))
        val euro = formatMoney(1234.56, "EUR", Locale.GERMANY)
        assertTrue(euro.contains("1.234,56"))
        assertTrue(euro.contains("€"))
    }

    @Test
    fun usesCurrencySpecificFractionDigits() {
        val yen = formatMoney(123.7, "JPY", Locale.JAPAN)
        assertTrue(yen.contains("124"))
        assertTrue(!yen.contains("."))
    }

    @Test
    fun invalidCurrencyCodeFallsBackToGroupedCodeAndNumber() {
        assertEquals("NOT_A_CURRENCY 1,234.50", formatMoney(1234.5, "not_a_currency", Locale.US))
    }

    @Test
    fun compatibilityWrapperRetainsIndianRupeeGrouping() {
        assertEquals("₹1,23,456.78", formatCurrency(123456.78))
    }
}
