package com.fintrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fintrace.app.data.model.SalaryMode

/**
 * One row per month holding the user's persistent manual adjustment to confirmed income.
 *
 * The Kotlin type is named for the adjustment it now stores, but the table and its columns keep
 * their original names: `salaryAmount` maps to `salary_amount` and `salaryMode` maps to
 * `salary_mode`. Renaming either column would require a Room schema migration, so properties that
 * mirror a column deliberately keep the legacy name while everything above this layer uses income
 * terminology.
 */
@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetAdjustmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "month_year")
    val monthYear: String, // Format: YYYY-MM (e.g. "2026-08")
    /**
     * Legacy database column name, now holding the manual adjustment rather than a total.
     *
     * It is NOT the final monthly income and it is NOT necessarily the user's salary.
     *
     * ```
     * effective monthly income = confirmed income + salaryAmount
     * ```
     *
     * Every CONFIRMED income transaction contributes to `confirmed income`, whatever its wording.
     * A negative value is valid: the user lowered the month below its confirmed income.
     */
    @ColumnInfo(name = "salary_amount")
    val salaryAmount: Double,
    @ColumnInfo(name = "salary_mode")
    val salaryMode: SalaryMode = SalaryMode.OVERRIDE,
    val notes: String? = null
)
