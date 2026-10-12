package com.fintrace.app.data.model

/** The selected per-category budget mode and its entered value. */
enum class CategoryBudgetMode {
    NONE,
    FIXED,
    PERCENTAGE
}

data class CategoryBudgetConfiguration(
    val mode: CategoryBudgetMode,
    val value: Double? = null
)

/** Keeps the persisted amount and percentage mutually exclusive and validates user input. */
object CategoryBudgetRules {
    fun normalize(configuration: CategoryBudgetConfiguration): CategoryBudgetConfiguration? {
        val value = configuration.value
        return when (configuration.mode) {
            CategoryBudgetMode.NONE -> CategoryBudgetConfiguration(CategoryBudgetMode.NONE)
            CategoryBudgetMode.FIXED -> value
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?.let { CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, it) }
            CategoryBudgetMode.PERCENTAGE -> value
                ?.takeIf { it.isFinite() && it in 0.0..100.0 }
                ?.let { CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, it) }
        }
    }

    fun fromStored(amount: Double?, percentage: Double?): CategoryBudgetConfiguration = when {
        percentage != null -> CategoryBudgetConfiguration(CategoryBudgetMode.PERCENTAGE, percentage)
        amount != null -> CategoryBudgetConfiguration(CategoryBudgetMode.FIXED, amount)
        else -> CategoryBudgetConfiguration(CategoryBudgetMode.NONE)
    }

    fun effectiveAmount(configuration: CategoryBudgetConfiguration, monthlyIncome: Double): Double? = when (configuration.mode) {
        CategoryBudgetMode.NONE -> null
        CategoryBudgetMode.FIXED -> configuration.value?.takeIf { it.isFinite() && it >= 0.0 }
        CategoryBudgetMode.PERCENTAGE -> configuration.value
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }
            ?.let { (monthlyIncome.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0) * (it / 100.0) }
            ?.takeIf(Double::isFinite)
    }
}
