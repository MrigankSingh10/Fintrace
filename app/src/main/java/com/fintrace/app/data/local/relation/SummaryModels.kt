package com.fintrace.app.data.local.relation

data class CategorySpendSummary(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalMyShareSpent: Double,
    val totalOriginalSpent: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double = 0.0
)

data class MonthlyFinancialSummary(
    val monthYear: String,
    val salaryAmount: Double,
    val totalMyShareSpent: Double,
    val totalOriginalSpent: Double,
    val remainingBalance: Double,
    val savingsRatePercentage: Double = 0.0
)
