package com.fintrace.app.data.repository

import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.dao.CategoryDeleteResult
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.SalaryMode
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>>
    suspend fun addCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity): CategoryDeleteResult
    suspend fun getCategoryById(id: Long): CategoryEntity?

    // Payment Modes
    fun getAllPaymentModes(): Flow<List<PaymentModeEntity>>
    suspend fun addPaymentMode(mode: PaymentModeEntity): Long
    suspend fun updatePaymentMode(mode: PaymentModeEntity)
    suspend fun deletePaymentMode(mode: PaymentModeEntity)
    suspend fun getPaymentModeById(id: Long): PaymentModeEntity?
    suspend fun ensureCreditCardMode(): Long

    // Transactions
    fun getConfirmedTransactions(): Flow<List<TransactionWithDetails>>
    fun getPendingTransactions(): Flow<List<TransactionWithDetails>>
    fun getDismissedTransactions(): Flow<List<TransactionWithDetails>>
    fun getPendingCount(): Flow<Int>
    fun getTransactionsForRange(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithDetails>>
    suspend fun getTransactionById(id: Long): TransactionWithDetails?
    suspend fun saveTransaction(transaction: TransactionEntity, splits: List<TransactionSplitEntity> = emptyList()): Long
    suspend fun confirmPendingTransaction(transaction: TransactionEntity, splits: List<TransactionSplitEntity> = emptyList()): Long
    suspend fun dismissPendingTransaction(transaction: TransactionEntity, splits: List<TransactionSplitEntity> = emptyList()): Long
    suspend fun restoreDismissedTransaction(transaction: TransactionEntity, splits: List<TransactionSplitEntity> = emptyList()): Long
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun isSmsAlreadyProcessed(smsBody: String): Boolean
    suspend fun insertPendingSmsIfNew(transaction: TransactionEntity, receivedTimestamp: Long): Boolean
    suspend fun materializeMonthlyRecurringTransactions()

    // Card Mappings
    fun getAllCardMappings(): Flow<List<com.fintrace.app.data.local.entity.CardMappingEntity>>
    suspend fun addCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity): Long
    suspend fun updateCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity)
    suspend fun deleteCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity)
    suspend fun getCardMappingByLastFour(lastFour: String): com.fintrace.app.data.local.entity.CardMappingEntity?

    // Monthly Income & Analytics
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetAdjustmentEntity?>

    /**
     * Persists the user's manual adjustment to the month's confirmed income. It is not a total:
     * the effective monthly income stays `confirmed income + adjustment`.
     */
    suspend fun setMonthlyIncomeAdjustment(
        monthYear: String,
        manualAdjustment: Double,
        notes: String? = null,
        mode: SalaryMode = SalaryMode.OVERRIDE
    )

    fun getMonthlyFinancialSummary(monthYear: String, startTimestamp: Long, endTimestamp: Long): Flow<MonthlyFinancialSummary>
    fun getCategoryBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpendSummary>>

    // Category Budgets (global)
    suspend fun updateCategoryBudget(categoryId: Long, budgetAmount: Double?)
    suspend fun updateCategoryBudgets(budgets: List<Pair<Long, Double?>>)
}
