package com.fintrace.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val projectedMonthEndSpend: Double = 0.0,
    val projectedSavings: Double = 0.0,
    val burnRatePercentage: Double = 0.0
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

    private val _isSalaryDialogOpen = MutableStateFlow(false)
    val isSalaryDialogOpen: StateFlow<Boolean> = _isSalaryDialogOpen.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyBudget: StateFlow<MonthlyBudgetSalaryEntity?> = selectedPeriod.flatMapLatest { period ->
        repository.getBudgetForMonth(period.monthYearKey)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlySummary: StateFlow<MonthlyFinancialSummary> = selectedPeriod.flatMapLatest { period ->
        repository.getMonthlyFinancialSummary(
            monthYear = period.monthYearKey,
            startTimestamp = period.startTimestamp,
            endTimestamp = period.endTimestamp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyFinancialSummary(
            monthYear = "",
            salaryAmount = 0.0,
            totalMyShareSpent = 0.0,
            totalOriginalSpent = 0.0,
            remainingBalance = 0.0
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryBreakdown: StateFlow<List<CategorySpendSummary>> = selectedPeriod.flatMapLatest { period ->
        repository.getCategoryBreakdown(period.startTimestamp, period.endTimestamp)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTransactions: StateFlow<List<TransactionWithDetails>> = selectedPeriod.flatMapLatest { period ->
        repository.getTransactionsForRange(period.startTimestamp, period.endTimestamp)
            .map { it.take(5) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onPreviousMonth() {
        val cal = _currentCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _currentCalendar.value = cal
    }

    fun onNextMonth() {
        val cal = _currentCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _currentCalendar.value = cal
    }

    fun onResetToCurrentMonth() {
        _currentCalendar.value = Calendar.getInstance()
    }

    fun onOpenSalaryDialog() {
        _isSalaryDialogOpen.value = true
    }

    fun onDismissSalaryDialog() {
        _isSalaryDialogOpen.value = false
    }

    fun saveMonthlySalary(amount: Double, salaryMode: SalaryMode = SalaryMode.OVERRIDE) {
        viewModelScope.launch {
            val period = selectedPeriod.value
            val currentSalary = monthlySummary.value.salaryAmount
            val total = when (salaryMode) {
                SalaryMode.OVERRIDE -> amount
                SalaryMode.ADD_TO_SMS -> currentSalary + amount
            }
            repository.setMonthlySalary(
                monthYear = period.monthYearKey,
                salary = total,
                notes = if (salaryMode == SalaryMode.ADD_TO_SMS) {
                    "Added ${String.format("%.0f", amount)} to the salary of ${period.displayName}"
                } else {
                    "Manual salary for ${period.displayName}"
                },
                salaryMode = salaryMode
            )
            onDismissSalaryDialog()
        }
    }

    private fun computeMonthPeriod(cal: Calendar): MonthPeriod {
        val startCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val keyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        val now = Calendar.getInstance()
        val isCurrentMonth = (now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == cal.get(Calendar.MONTH))

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysElapsed = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else daysInMonth

        return MonthPeriod(
            monthYearKey = keyFormat.format(startCal.time),
            displayName = displayFormat.format(startCal.time),
            startTimestamp = startCal.timeInMillis,
            endTimestamp = endCal.timeInMillis,
            daysInMonth = daysInMonth,
            daysElapsed = daysElapsed.coerceAtLeast(1)
        )
    }
}
