package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month_year = :monthYear LIMIT 1")
    fun getBudgetForMonthFlow(monthYear: String): Flow<MonthlyBudgetSalaryEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE month_year = :monthYear LIMIT 1")
    suspend fun getBudgetForMonth(monthYear: String): MonthlyBudgetSalaryEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY month_year DESC")
    fun getAllBudgetsFlow(): Flow<List<MonthlyBudgetSalaryEntity>>

    @Query("SELECT * FROM monthly_budgets ORDER BY month_year DESC")
    suspend fun getAllBudgets(): List<MonthlyBudgetSalaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthlyBudgetSalaryEntity)

    @Delete
    suspend fun deleteBudget(budget: MonthlyBudgetSalaryEntity)
}
