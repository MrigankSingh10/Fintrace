package com.fintrace.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.repository.overrideAdjustment
import com.fintrace.app.data.repository.additiveAdjustment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class MonthPeriod(
    val monthYearKey: String,   // e.g. "2026-08"
    val displayName: String,    // e.g. "August 2026"
    val startTimestamp: Long,
    val endTimestamp: Long,
    val daysInMonth: Int,
    val daysElapsed: Int
)

data class DashboardPaceMetrics(
    val dailyAverageSpent: Double = 0.0,
    val projectedMonthEndSpent: Double = 0.0,
    val projectedSavings: Double = 0.0
)

data class CategoryBudgetDialogState(
    val categoryId: Long,
    val categoryName: String,
    val currentBudget: Double?
)

data class BulkBudgetEditState(
    val categories: List<CategoryEntity>
)

data class DashboardIncomeDialogState(
    val monthYearKey: String,
    val displayName: String,
    val currentIncome: Double,
    val monthlyBudget: MonthlyBudgetAdjustmentEntity?
)

class DashboardViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedPeriod: StateFlow<MonthPeriod> = _currentCalendar.map { cal ->
        computeMonthPeriod(cal)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = computeMonthPeriod(Calendar.getInstance())
    )

    private val _incomeDialogState = MutableStateFlow<DashboardIncomeDialogState?>(null)
    val incomeDialogState: StateFlow<DashboardIncomeDialogState?> = _incomeDialogState.asStateFlow()

    private val _categoryBudgetDialogState = MutableStateFlow<CategoryBudgetDialogState?>(null)
    val categoryBudgetDialogState: StateFlow<CategoryBudgetDialogState?> = _categoryBudgetDialogState.asStateFlow()

    private val _bulkBudgetState = MutableStateFlow<BulkBudgetEditState>(BulkBudgetEditState(emptyList()))
    val bulkBudgetState: StateFlow<BulkBudgetEditState> = _bulkBudgetState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthUiState: StateFlow<DashboardMonthUiState> = selectedPeriod.flatMapLatest { period ->
        flow {
            emit(DashboardMonthUiState(monthYearKey = period.monthYearKey, isLoading = true))
            combine(
                repository.getMonthlyFinancialSummary(period.monthYearKey, period.startTimestamp, period.endTimestamp),
                repository.getBudgetForMonth(period.monthYearKey),
                repository.getCategoryBreakdown(period.startTimestamp, period.endTimestamp),
                repository.getTransactionsForRange(period.startTimestamp, period.endTimestamp)
            ) { summary, budget, categories, transactions ->
                DashboardMonthUiState(
                    monthYearKey = period.monthYearKey,
                    isLoading = false,
                    summary = summary,
                    budget = budget,
                    categories = categories,
                    confirmedTransactions = transactions.filter { it.transaction.status == TransactionStatus.CONFIRMED }
                )
            }.collect { emit(it) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMonthUiState(
            monthYearKey = computeMonthPeriod(Calendar.getInstance()).monthYearKey,
            isLoading = true
        )
    )

    fun onPreviousMonth() {
        onMonthSelected(selectedPeriod.value.monthYearKey.toYearMonth().minusMonths(1))
    }

    fun onNextMonth() {
        onMonthSelected(selectedPeriod.value.monthYearKey.toYearMonth().plusMonths(1))
    }

    fun onResetToCurrentMonth() {
        onMonthSelected(YearMonth.now())
    }

    fun onMonthSelected(month: YearMonth) {
        _currentCalendar.value = dashboardCalendarForMonth(month)
    }

    fun onOpenIncomeDialog(month: YearMonth) {
        viewModelScope.launch {
            val period = dashboardMonthPeriod(month)
            val summaryFlow = repository.getMonthlyFinancialSummary(period.monthYearKey, period.startTimestamp, period.endTimestamp)
            val budgetFlow = repository.getBudgetForMonth(period.monthYearKey)
            val (summary, budget) = combine(summaryFlow, budgetFlow) { summary, budget -> summary to budget }.first()
            _incomeDialogState.value = DashboardIncomeDialogState(
                monthYearKey = period.monthYearKey,
                displayName = period.displayName,
                currentIncome = summary.monthlyIncome,
                monthlyBudget = budget
            )
        }
    }

    fun onDismissIncomeDialog() {
        _incomeDialogState.value = null
    }

    fun onEditCategoryBudget(category: CategorySpendSummary) {
        _categoryBudgetDialogState.value = CategoryBudgetDialogState(
            categoryId = category.categoryId,
            categoryName = category.categoryName,
            currentBudget = category.budgetAmount
        )
    }

    fun onDismissCategoryBudgetDialog() {
        _categoryBudgetDialogState.value = null
    }

    fun onSaveCategoryBudget(categoryId: Long, budgetAmount: Double?) {
        viewModelScope.launch {
            repository.updateCategoryBudget(categoryId, budgetAmount)
            onDismissCategoryBudgetDialog()
        }
    }

    fun onOpenBulkBudgets() {
        viewModelScope.launch {
            _bulkBudgetState.value = BulkBudgetEditState(categories = repository.getAllCategories().first())
        }
    }

    fun onDismissBulkBudgets() {
        _bulkBudgetState.value = BulkBudgetEditState(emptyList())
    }

    fun onSaveBulkBudgets(budgets: Map<Long, Double?>) {
        viewModelScope.launch {
            repository.updateCategoryBudgets(budgets.toList())
            onDismissBulkBudgets()
        }
    }

    /**
     * Persists the user's manual adjustment for the selected month.
     *
     * OVERRIDE sets the current total to [amount] without freezing it: the stored adjustment is
     * recalculated against confirmed income, so income confirmed later keeps accumulating.
     * ADD only increments the existing adjustment - it must never store a snapshot of the
     * resolved total.
     */
    fun saveMonthlyIncomeAdjustment(amount: Double, mode: SalaryMode = SalaryMode.OVERRIDE) {
        viewModelScope.launch {
            val dialog = _incomeDialogState.value ?: return@launch
            val period = dashboardMonthPeriod(YearMonth.parse(dialog.monthYearKey))
            val summary = repository.getMonthlyFinancialSummary(
                monthYear = dialog.monthYearKey,
                startTimestamp = period.startTimestamp,
                endTimestamp = period.endTimestamp
            ).first()
            val newAdjustment = when (mode) {
                SalaryMode.OVERRIDE -> overrideAdjustment(
                    targetIncome = amount,
                    confirmedIncome = summary.confirmedIncome
                )

                SalaryMode.ADD_TO_SMS -> additiveAdjustment(
                    existingAdjustment = summary.manualAdjustment,
                    amountToAdd = amount
                )
            }
            repository.setMonthlyIncomeAdjustment(
                monthYear = dialog.monthYearKey,
                manualAdjustment = newAdjustment,
                notes = if (mode == SalaryMode.ADD_TO_SMS) {
                    "Added ${String.format("%.0f", amount)} to the income of ${period.displayName}"
                } else {
                    "Manual income adjustment for ${period.displayName}"
                },
                mode = mode
            )
            onDismissIncomeDialog()
        }
    }

    private fun computeMonthPeriod(cal: Calendar): MonthPeriod {
        val month = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        return dashboardMonthPeriod(month)
    }

    private fun dashboardMonthPeriod(month: YearMonth): MonthPeriod {
        val zone = ZoneId.systemDefault()
        val startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val now = Calendar.getInstance()
        val isCurrentMonth = YearMonth.of(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) == month
        val daysInMonth = month.lengthOfMonth()
        val daysElapsed = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else daysInMonth

        return MonthPeriod(
            monthYearKey = month.toString(),
            displayName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
            startTimestamp = startMillis,
            endTimestamp = endMillis,
            daysInMonth = daysInMonth,
            daysElapsed = daysElapsed.coerceAtLeast(1)
        )
    }

    private fun String.toYearMonth(): YearMonth = YearMonth.parse(this)
}

internal fun dashboardCalendarForMonth(month: YearMonth): Calendar = Calendar.getInstance().apply {
    clear()
    set(month.year, month.monthValue - 1, 1, 0, 0, 0)
}
