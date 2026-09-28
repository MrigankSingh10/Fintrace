package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month_year = :monthYear LIMIT 1")
    fun getBudgetForMonthFlow(monthYear: String): Flow<MonthlyBudgetAdjustmentEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE month_year = :monthYear LIMIT 1")
    suspend fun getBudgetForMonth(monthYear: String): MonthlyBudgetAdjustmentEntity?

    @Query("SELECT * FROM monthly_budgets ORDER BY month_year DESC")
    fun getAllBudgetsFlow(): Flow<List<MonthlyBudgetAdjustmentEntity>>

    @Query("SELECT * FROM monthly_budgets ORDER BY month_year DESC")
    suspend fun getAllBudgets(): List<MonthlyBudgetAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: MonthlyBudgetAdjustmentEntity)

    @Delete
    suspend fun deleteBudget(budget: MonthlyBudgetAdjustmentEntity)
}
