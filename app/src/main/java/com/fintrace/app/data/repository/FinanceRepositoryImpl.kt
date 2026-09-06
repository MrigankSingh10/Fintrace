package com.fintrace.app.data.repository

import com.fintrace.app.data.local.dao.CategoryDao
import com.fintrace.app.data.local.dao.MonthlyBudgetDao
import com.fintrace.app.data.local.dao.PaymentModeDao
import com.fintrace.app.data.local.dao.TransactionDao
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val paymentModeDao: PaymentModeDao,
    private val transactionDao: TransactionDao,
    private val monthlyBudgetDao: MonthlyBudgetDao
) : FinanceRepository {

    // --- Categories ---
    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategoriesFlow()

    override suspend fun addCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) =
        categoryDao.deleteCategory(category)

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)


    // --- Payment Modes ---
    override fun getAllPaymentModes(): Flow<List<PaymentModeEntity>> =
        paymentModeDao.getAllPaymentModesFlow()

    override suspend fun addPaymentMode(mode: PaymentModeEntity): Long =
        paymentModeDao.insertPaymentMode(mode)

    override suspend fun updatePaymentMode(mode: PaymentModeEntity) =
        paymentModeDao.updatePaymentMode(mode)

    override suspend fun deletePaymentMode(mode: PaymentModeEntity) =
        paymentModeDao.deletePaymentMode(mode)

    override suspend fun getPaymentModeById(id: Long): PaymentModeEntity? =
        paymentModeDao.getPaymentModeById(id)


    // --- Transactions ---
    override fun getConfirmedTransactions(): Flow<List<TransactionWithDetails>> =
        transactionDao.getConfirmedTransactionsFlow()

    override fun getPendingTransactions(): Flow<List<TransactionWithDetails>> =
        transactionDao.getPendingTransactionsFlow()

    override fun getDismissedTransactions(): Flow<List<TransactionWithDetails>> =
        transactionDao.getDismissedTransactionsFlow()

    override fun getPendingCount(): Flow<Int> =
        transactionDao.getPendingCountFlow()

    override fun getTransactionsForRange(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<TransactionWithDetails>> =
        transactionDao.getTransactionsInRangeFlow(startTimestamp, endTimestamp)

    override suspend fun getTransactionById(id: Long): TransactionWithDetails? =
        transactionDao.getTransactionWithDetailsById(id)

    override suspend fun saveTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long = transactionDao.saveTransactionWithSplits(transaction, splits)

    override suspend fun confirmPendingTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long {
        val confirmed = transaction.copy(status = TransactionStatus.CONFIRMED)
        return transactionDao.saveTransactionWithSplits(confirmed, splits)
    }

    override suspend fun dismissPendingTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long = transactionDao.saveTransactionWithSplits(
        transaction.copy(status = TransactionStatus.DISMISSED),
        splits
    )

    override suspend fun restoreDismissedTransaction(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long = transactionDao.saveTransactionWithSplits(
        transaction.copy(status = TransactionStatus.PENDING),
        splits
    )

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun isSmsAlreadyProcessed(smsBody: String): Boolean =
        transactionDao.getTransactionBySmsBody(smsBody) != null


    // --- Monthly Budget & Analytics ---
    override fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetSalaryEntity?> =
        monthlyBudgetDao.getBudgetForMonthFlow(monthYear)

    override suspend fun setMonthlySalary(monthYear: String, salary: Double, notes: String?) {
        monthlyBudgetDao.upsertBudget(
            MonthlyBudgetSalaryEntity(
                monthYear = monthYear,
                salaryAmount = salary,
                notes = notes
            )
        )
    }

    override fun getMonthlyFinancialSummary(
        monthYear: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<MonthlyFinancialSummary> {
        val budgetFlow = monthlyBudgetDao.getBudgetForMonthFlow(monthYear)
        val myShareSpentFlow = transactionDao.getTotalMyShareSpentInRangeFlow(startTimestamp, endTimestamp)
        val originalSpentFlow = transactionDao.getTotalOriginalSpentInRangeFlow(startTimestamp, endTimestamp)
        val confirmedIncomeFlow = transactionDao.getTotalConfirmedIncomeInRangeFlow(startTimestamp, endTimestamp)

        return combine(budgetFlow, myShareSpentFlow, originalSpentFlow, confirmedIncomeFlow) { budget, myShareSpent, originalSpent, confirmedIncome ->
            // Confirmed income is the source of truth for the month. A manually entered
            // salary remains a fallback only until the first income is confirmed.
            val isIncomeDerived = confirmedIncome > 0.0
            val salary = if (isIncomeDerived) confirmedIncome else budget?.salaryAmount ?: 0.0
            val remaining = if (salary > 0.0) salary - myShareSpent else 0.0
            val savingsRate = if (salary > 0.0) {
                ((salary - myShareSpent) / salary) * 100.0
            } else 0.0

            MonthlyFinancialSummary(
                monthYear = monthYear,
                salaryAmount = salary,
                totalMyShareSpent = myShareSpent,
                totalOriginalSpent = originalSpent,
                remainingBalance = remaining,
                savingsRatePercentage = savingsRate.coerceAtLeast(0.0),
                isIncomeDerived = isIncomeDerived
            )
        }
    }

    override fun getCategoryBreakdown(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySpendSummary>> {
        val aggregatesFlow = transactionDao.getCategoryAggregatesInRangeFlow(startTimestamp, endTimestamp)
        val totalSpentFlow = transactionDao.getTotalMyShareSpentInRangeFlow(startTimestamp, endTimestamp)
        val confirmedIncomeFlow = transactionDao.getTotalConfirmedIncomeInRangeFlow(startTimestamp, endTimestamp)

        // Extract monthYear from timestamp to get salary for the month
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val monthYear = dateFormat.format(java.util.Date(startTimestamp))
        val budgetFlow = monthlyBudgetDao.getBudgetForMonthFlow(monthYear)

        return combine(aggregatesFlow, totalSpentFlow, budgetFlow, confirmedIncomeFlow) { aggregates, _, budget, confirmedIncome ->
            val salary = if (confirmedIncome > 0.0) confirmedIncome else budget?.salaryAmount ?: 0.0
            aggregates.map { raw ->
                val percentage = if (salary > 0.0) {
                    (raw.totalMyShare / salary) * 100.0
                } else 0.0

                CategorySpendSummary(
                    categoryId = raw.categoryId,
                    categoryName = raw.categoryName,
                    colorHex = raw.colorHex,
                    iconName = raw.iconName,
                    totalMyShareSpent = raw.totalMyShare,
                    totalOriginalSpent = raw.totalOriginal,
                    transactionCount = raw.count,
                    percentageOfTotal = percentage
                )
            }
        }
    }
}
