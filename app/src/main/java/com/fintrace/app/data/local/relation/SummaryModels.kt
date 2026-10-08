package com.fintrace.app.data.local.relation

data class CategorySpendSummary(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalMyShareSpent: Double,
    val totalOriginalSpent: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double = 0.0,
    val budgetAmount: Double? = null,
    val budgetRemaining: Double? = null,
    val budgetUtilization: Double? = null
)

data class MonthlyFinancialSummary(
    val monthYear: String,
    /** Effective monthly income: [confirmedIncome] + [manualAdjustment]. */
    val monthlyIncome: Double,
    val totalMyShareSpent: Double,
    val totalOriginalSpent: Double,
    val remainingBalance: Double,
    val savingsRatePercentage: Double = 0.0,
    /** Descriptive only - never gates whether income can be edited. */
    val isIncomeDerived: Boolean = false,
    /** Sum of every CONFIRMED INCOME transaction in the month. */
    val confirmedIncome: Double = 0.0,
    /** User-controlled adjustment stored in `monthly_budgets.salary_amount`. */
    val manualAdjustment: Double = 0.0
)
