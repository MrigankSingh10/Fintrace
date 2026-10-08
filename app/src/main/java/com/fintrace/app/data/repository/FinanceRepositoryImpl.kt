package com.fintrace.app.data.repository

import com.fintrace.app.data.local.dao.CardMappingDao
import com.fintrace.app.data.local.dao.CategoryDao
import com.fintrace.app.data.local.dao.CategoryDeleteResult
import com.fintrace.app.data.local.dao.MonthlyBudgetDao
import com.fintrace.app.data.local.dao.PaymentModeDao
import com.fintrace.app.data.local.dao.TransactionDao
import com.fintrace.app.data.local.entity.CardMappingEntity
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.model.SmsSourceIdentity
import com.fintrace.app.data.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val paymentModeDao: PaymentModeDao,
    private val transactionDao: TransactionDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val cardMappingDao: CardMappingDao
) : FinanceRepository {

    // --- Card Mappings ---
    override fun getAllCardMappings(): Flow<List<CardMappingEntity>> =
        cardMappingDao.getAllCardMappingsFlow()

    override suspend fun addCardMapping(cardMapping: CardMappingEntity): Long =
        cardMappingDao.insertCardMapping(cardMapping)

    override suspend fun updateCardMapping(cardMapping: CardMappingEntity) =
        cardMappingDao.updateCardMapping(cardMapping)

    override suspend fun deleteCardMapping(cardMapping: CardMappingEntity) =
        cardMappingDao.deleteCardMapping(cardMapping)

    override suspend fun getCardMappingByLastFour(lastFour: String): CardMappingEntity? =
        cardMappingDao.getCardMappingByLastFour(lastFour)

    // --- Categories ---
    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategoriesFlow()

    override suspend fun addCategory(category: CategoryEntity): Long =
        categoryDao.insertCategory(category)

    override suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity): CategoryDeleteResult =
        categoryDao.deleteCategoryAndReassign(category.id)

    override suspend fun getCategoryById(id: Long): CategoryEntity? =
        categoryDao.getCategoryById(id)


    // --- Payment Modes ---
    override fun getAllPaymentModes(): Flow<List<PaymentModeEntity>> =
        paymentModeDao.getAllPaymentModesFlow()

    override suspend fun addPaymentMode(mode: PaymentModeEntity): Long =
        paymentModeDao.insertPaymentMode(mode)

    override suspend fun updatePaymentMode(mode: PaymentModeEntity) =
        paymentModeDao.updatePaymentMode(mode)

    override suspend fun deletePaymentMode(mode: PaymentModeEntity) {
        // The seeded DEBIT row (id 1) is required as the ON DELETE SET DEFAULT fallback target.
        if (mode.id == 1L) return
        paymentModeDao.deletePaymentMode(mode)
    }

    override suspend fun getPaymentModeById(id: Long): PaymentModeEntity? =
        paymentModeDao.getPaymentModeById(id)

    /**
     * Guarantees a CREDIT_CARD-typed payment mode always exists. If the user deleted every
     * credit-card mode, a generic "Credit Card (Unmapped)" mode is created on demand so that
     * card transactions never silently fall back to DEBIT and the mapping prompt stays available.
     */
    override suspend fun ensureCreditCardMode(): Long {
        paymentModeDao.getAllPaymentModes().firstOrNull { it.type == com.fintrace.app.data.model.PaymentModeType.CREDIT_CARD }
            ?.let { return it.id }
        return paymentModeDao.insertPaymentMode(
            PaymentModeEntity(
                id = 0,
                name = "Credit Card (Unmapped)",
                type = com.fintrace.app.data.model.PaymentModeType.CREDIT_CARD,
                iconName = "CreditCard",
                isDefault = false
            )
        )
    }


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
    ): Long {
        val previous = transaction.id.takeIf { it > 0L }?.let { transactionDao.getTransactionById(it) }
        val previousSeriesId = previous?.recurringSeriesId

        val normalized = if (transaction.isRecurring) {
            val seriesId = transaction.recurringSeriesId ?: java.util.UUID.randomUUID().toString()
            transaction.copy(
                recurringSeriesId = seriesId,
                recurringMonth = recurringMonthKey(transaction.timestamp),
                recurringDayOfMonth = transaction.recurringDayOfMonth
                    ?: recurringDayOfMonth(transaction.timestamp)
            )
        } else {
            transaction.copy(
                recurringSeriesId = null,
                recurringMonth = null,
                recurringDayOfMonth = null
            )
        }

        val transactionId = transactionDao.saveTransactionWithSplits(normalized, splits)

        if (previousSeriesId != null && previousSeriesId != normalized.recurringSeriesId) {
            transactionDao.clearRecurringSeries(previousSeriesId)
        }

        normalized.recurringSeriesId?.let { seriesId ->
            synchronizeFutureOccurrences(
                seriesId = seriesId,
                fromTimestamp = normalized.timestamp,
                template = normalized.copy(id = transactionId),
                splits = splits
            )
            val transactionMonth = YearMonth.parse(
                normalized.recurringMonth ?: recurringMonthKey(normalized.timestamp)
            )
            val nextCalendarMonth = YearMonth.now().plusMonths(1)
            materializeSeriesThrough(
                template = latestRecurringTransaction(seriesId) ?: normalized.copy(id = transactionId),
                targetMonth = maxOf(transactionMonth.plusMonths(1), nextCalendarMonth)
            )
        }

        return transactionId
    }

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

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        // With no separate scheduler/template row, deleting a recurring occurrence cancels
        // future generation while retaining the other generated transactions as normal rows.
        transaction.recurringSeriesId?.let { transactionDao.clearRecurringSeries(it) }
        transactionDao.deleteTransaction(transaction.copy(
            isRecurring = false,
            recurringSeriesId = null,
            recurringMonth = null,
            recurringDayOfMonth = null
        ))
    }

    override suspend fun isSmsAlreadyProcessed(smsBody: String): Boolean =
        transactionDao.getTransactionBySmsBody(smsBody) != null

    override suspend fun insertPendingSmsIfNew(
        transaction: TransactionEntity,
        receivedTimestamp: Long
    ): Boolean {
        val body = transaction.smsRawBody ?: return false
        val sourceTimestamp = transaction.smsSourceTimestamp ?: return false
        val identity = SmsSourceIdentity(body, transaction.smsSender, sourceTimestamp, receivedTimestamp)
        return transactionDao.insertPendingSmsIfNew(
            transaction,
            identity.body,
            identity.sender,
            identity.sourceTimestamp,
            identity.receivedTimestamp
        )
    }

    override suspend fun materializeMonthlyRecurringTransactions() {
        val targetMonth = YearMonth.now().plusMonths(1)
        transactionDao.getLatestRecurringTransactions().forEach { latest ->
            materializeSeriesThrough(latest, targetMonth)
        }
    }

    private suspend fun synchronizeFutureOccurrences(
        seriesId: String,
        fromTimestamp: Long,
        template: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ) {
        var priorTimestamp = template.timestamp
        val anchorDay = template.recurringDayOfMonth ?: recurringDayOfMonth(template.timestamp)
        transactionDao.getRecurringTransactionsAfter(seriesId, fromTimestamp).forEach { future ->
            val futureMonth = YearMonth.parse(
                future.recurringMonth ?: recurringMonthKey(future.timestamp)
            )
            val adjustedTimestamp = timestampForRecurringMonth(
                previousTimestamp = priorTimestamp,
                month = futureMonth,
                anchorDayOfMonth = anchorDay
            )
            val updated = future.copy(
                description = template.description,
                timestamp = adjustedTimestamp,
                originalAmount = template.originalAmount,
                myShareAmount = template.myShareAmount,
                categoryId = template.categoryId,
                paymentModeId = template.paymentModeId,
                type = template.type,
                notes = template.notes,
                currency = template.currency,
                recurringDayOfMonth = anchorDay
            )
            transactionDao.saveTransactionWithSplits(
                updated,
                splits.map { it.copy(id = 0L, transactionId = future.id) }
            )
            priorTimestamp = adjustedTimestamp
        }
    }

    private suspend fun latestRecurringTransaction(seriesId: String): TransactionEntity? =
        transactionDao.getLatestRecurringTransactions()
            .firstOrNull { it.recurringSeriesId == seriesId }

    private suspend fun materializeSeriesThrough(
        template: TransactionEntity,
        targetMonth: YearMonth
    ) {
        val seriesId = template.recurringSeriesId ?: return
        var latest = template
        var latestMonth = YearMonth.parse(
            latest.recurringMonth ?: recurringMonthKey(latest.timestamp)
        )
        var safetyCounter = 0

        while (latestMonth < targetMonth && safetyCounter++ < MAX_RECURRING_CATCH_UP_MONTHS) {
            val nextMonthValue = latestMonth.plusMonths(1)
            val nextTimestamp = timestampForRecurringMonth(
                previousTimestamp = latest.timestamp,
                month = nextMonthValue,
                anchorDayOfMonth = latest.recurringDayOfMonth
                    ?: recurringDayOfMonth(latest.timestamp)
            )
            val nextMonth = nextMonthValue.toString()
            val existing = transactionDao.getRecurringTransactionForMonth(seriesId, nextMonth)
            if (existing != null) {
                latest = existing
                latestMonth = YearMonth.parse(
                    existing.recurringMonth ?: recurringMonthKey(existing.timestamp)
                )
                continue
            }

            val next = latest.copy(
                id = 0L,
                timestamp = nextTimestamp,
                smsRawBody = null,
                smsSender = null,
                parseConfidence = null,
                cardLastFour = null,
                recurringMonth = nextMonth
            )
            val splits = transactionDao.getSplitsForTransaction(latest.id)
            val insertedId = transactionDao.insertRecurringTransactionWithSplits(next, splits)
            if (insertedId == -1L) break

            latest = next.copy(id = insertedId)
            latestMonth = nextMonthValue
        }
    }

    override suspend fun updateCategoryBudget(categoryId: Long, budgetAmount: Double?) {
        val category = categoryDao.getCategoryById(categoryId) ?: return
        val normalized = budgetAmount?.takeIf { it > 0.0 }
        val updated = category.copy(budgetAmount = normalized)
        categoryDao.updateCategory(updated)
    }

    override suspend fun updateCategoryBudgets(budgets: List<Pair<Long, Double?>>) {
        if (budgets.isEmpty()) return
        val allCategories = categoryDao.getAllCategories()
        val idToCategory = allCategories.associateBy { it.id }
        val toUpdate = mutableListOf<CategoryEntity>()
        for ((categoryId, budgetAmount) in budgets) {
            val category = idToCategory[categoryId] ?: continue
            val normalized = budgetAmount?.takeIf { it > 0.0 }
            if (category.budgetAmount != normalized) {
                toUpdate.add(category.copy(budgetAmount = normalized))
            }
        }
        if (toUpdate.isNotEmpty()) {
            categoryDao.updateCategories(toUpdate)
        }
    }

    // --- Monthly Income & Analytics ---
    override fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudgetAdjustmentEntity?> =
        monthlyBudgetDao.getBudgetForMonthFlow(monthYear)

    override suspend fun setMonthlyIncomeAdjustment(
        monthYear: String,
        manualAdjustment: Double,
        notes: String?,
        mode: SalaryMode
    ) {
        monthlyBudgetDao.upsertBudget(
            MonthlyBudgetAdjustmentEntity(
                monthYear = monthYear,
                salaryAmount = manualAdjustment,
                salaryMode = mode,
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
            val monthlyIncome = resolveMonthlyIncome(confirmedIncome, budget)
            val remaining = if (monthlyIncome > 0.0) monthlyIncome - myShareSpent else 0.0
            val savingsRate = if (monthlyIncome > 0.0) {
                ((monthlyIncome - myShareSpent) / monthlyIncome) * 100.0
            } else 0.0

            MonthlyFinancialSummary(
                monthYear = monthYear,
                monthlyIncome = monthlyIncome,
                totalMyShareSpent = myShareSpent,
                totalOriginalSpent = originalSpent,
                remainingBalance = remaining,
                savingsRatePercentage = savingsRate.coerceAtLeast(0.0),
                isIncomeDerived = isIncomeDerived(confirmedIncome, budget),
                confirmedIncome = confirmedIncome,
                manualAdjustment = budget?.salaryAmount ?: 0.0
            )
        }
    }

    override fun getCategoryBreakdown(
        startTimestamp: Long,
        endTimestamp: Long
    ): Flow<List<CategorySpendSummary>> {
        val aggregatesFlow = transactionDao.getCategoryAggregatesInRangeFlow(startTimestamp, endTimestamp)
        val confirmedIncomeFlow = transactionDao.getTotalConfirmedIncomeInRangeFlow(startTimestamp, endTimestamp)
        val categoriesFlow = categoryDao.getAllCategoriesFlow()

        // Extract monthYear from timestamp to get the month's income for the month
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val monthYear = dateFormat.format(java.util.Date(startTimestamp))
        val budgetFlow = monthlyBudgetDao.getBudgetForMonthFlow(monthYear)

        return combine(aggregatesFlow, categoriesFlow, budgetFlow, confirmedIncomeFlow) { aggregates, categories, budget, confirmedIncome ->
            val monthlyIncome = resolveMonthlyIncome(confirmedIncome, budget)
            val aggregateMap = aggregates.associateBy { it.categoryId }
            val included = mutableSetOf<Long>()
            categories.forEach { cat ->
                val agg = aggregateMap[cat.id]
                val hasSpend = agg?.totalMyShare != null && agg.totalMyShare > 0.0
                val hasBudget = cat.budgetAmount != null && cat.budgetAmount > 0.0
                if (hasSpend || hasBudget) {
                    included.add(cat.id)
                }
            }
            aggregates.forEach { included.add(it.categoryId) }

            val results = mutableListOf<CategorySpendSummary>()
            categories.forEach { cat ->
                if (cat.id !in included) return@forEach
                val agg = aggregateMap[cat.id]
                val spent = agg?.totalMyShare ?: 0.0
                val budgetAmt = cat.budgetAmount?.takeIf { it > 0.0 }
                val percentage = if (monthlyIncome > 0.0) {
                    (spent / monthlyIncome) * 100.0
                } else 0.0
                val budgetRemaining = budgetAmt?.let { it - spent }
                val budgetUtilization = budgetAmt?.let { if (it > 0.0) spent / it else null }

                results.add(
                    CategorySpendSummary(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        colorHex = cat.colorHex,
                        iconName = cat.iconName,
                        totalMyShareSpent = spent,
                        totalOriginalSpent = agg?.totalOriginal ?: 0.0,
                        transactionCount = agg?.count ?: 0,
                        percentageOfTotal = percentage,
                        budgetAmount = budgetAmt,
                        budgetRemaining = budgetRemaining,
                        budgetUtilization = budgetUtilization
                    )
                )
            }
            results.sortedWith(
                compareByDescending<CategorySpendSummary> { it.budgetUtilization ?: -1.0 }
                    .thenByDescending { it.totalMyShareSpent }
            )
        }
    }

    private companion object {
        const val MAX_RECURRING_CATCH_UP_MONTHS = 1200
    }
}
