package com.fintrace.app.data.repository

import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
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
    suspend fun deleteCategory(category: CategoryEntity)
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

    // Card Mappings
    fun getAllCardMappings(): Flow<List<com.fintrace.app.data.local.entity.CardMappingEntity>>
    suspend fun addCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity): Long
    suspend fun updateCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity)
    suspend fun deleteCardMapping(cardMapping: com.fintrace.app.data.local.entity.CardMappingEntity)
    suspend fun getCardMappingByLastFour(lastFour: String): com.fintrace.app.data.local.entity.CardMappingEntity?

    // Monthly Budget & Analytics
    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetSalaryEntity?>
    suspend fun setMonthlySalary(monthYear: String, salary: Double, notes: String? = null, salaryMode: SalaryMode = SalaryMode.OVERRIDE)
    fun getMonthlyFinancialSummary(monthYear: String, startTimestamp: Long, endTimestamp: Long): Flow<MonthlyFinancialSummary>
    fun getCategoryBreakdown(startTimestamp: Long, endTimestamp: Long): Flow<List<CategorySpendSummary>>
}
