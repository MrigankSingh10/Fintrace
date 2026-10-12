package com.fintrace.app

import com.fintrace.app.data.model.CategoryBudgetConfiguration
import com.fintrace.app.data.model.CategoryBudgetMode
import com.fintrace.app.data.model.CategoryBudgetRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryBudgetRulesTest {
    @Test
    fun normalizationAcceptsFixedZeroAndRejectsInvalidValues() {
        assertEquals(
            CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, 0.0),
            CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, 0.0))
        )
        assertNull(CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, -1.0)))
        assertNull(CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, Double.NaN)))
        assertNull(CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, Double.POSITIVE_INFINITY)))
    }

    @Test
    fun normalizationAcceptsPercentageEdgesAndRejectsOutOfRange() {
        assertEquals(
            CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 0.0),
            CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 0.0))
        )
        assertEquals(
            CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 100.0),
            CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 100.0))
        )
        assertNull(CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 100.01)))
        assertNull(CategoryBudgetRules.normalize(CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, Double.NaN)))
    }

    @Test
    fun effectivePercentageUsesResolvedIncomeAndClampsNegativeIncome() {
        val budget = CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 20.0)

        assertEquals(16_000.0, CategoryBudgetRules.effectiveAmount(budget, 80_000.0)!!, 0.001)
        assertEquals(0.0, CategoryBudgetRules.effectiveAmount(budget, -1_000.0)!!, 0.001)
        assertEquals(0.0, CategoryBudgetRules.effectiveAmount(budget, Double.NaN)!!, 0.001)
        assertEquals(
            1.0e308,
            CategoryBudgetRules.effectiveAmount(CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, 100.0), 1.0e308)!!,
            1.0e292
        )
    }

    @Test
    fun storedConfigurationPrefersPercentageAndDistinguishesUnbudgeted() {
        assertEquals(CategoryBudgetMode.FIXED, CategoryBudgetRules.fromStored(1_000.0, null).mode)
        assertEquals(CategoryBudgetMode.PERCENTAGE, CategoryBudgetRules.fromStored(1_000.0, 25.0).mode)
        assertEquals(CategoryBudgetMode.NONE, CategoryBudgetRules.fromStored(null, null).mode)
    }
}
