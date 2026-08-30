package com.fintrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetSalaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "month_year")
    val monthYear: String, // Format: YYYY-MM (e.g. "2026-08")
    @ColumnInfo(name = "salary_amount")
    val salaryAmount: Double,
    val notes: String? = null
)
