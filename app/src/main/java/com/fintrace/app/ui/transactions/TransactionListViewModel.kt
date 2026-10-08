package com.fintrace.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

internal fun YearMonth.transactionTimestampRange(zone: ZoneId = ZoneId.systemDefault()): LongRange {
    val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val nextMonth = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start until nextMonth
}

enum class TransactionSegment { ALL, PENDING, DISMISSED }

data class TransactionListFilter(
    val searchQuery: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val paymentModeIds: Set<Long> = emptySet(),
    val type: TransactionType? = null,
    val hasSplit: Boolean? = null
)

data class CurrencyExpenseSummary(val currencyCode: String, val totalMyShareSpent: Double, val totalOriginalCharged: Double, val transactionCount: Int)
data class TransactionListSummary(val byCurrency: List<CurrencyExpenseSummary> = emptyList())

data class MonthTransactions(val month: YearMonth, val rows: List<TransactionWithDetails>)

class TransactionListViewModel(private val repository: FinanceRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentModes: StateFlow<List<PaymentModeEntity>> = repository.getAllPaymentModes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _filter = MutableStateFlow(TransactionListFilter())
    val filter: StateFlow<TransactionListFilter> = _filter.asStateFlow()
    private val _segment = MutableStateFlow(TransactionSegment.ALL)
    val segment: StateFlow<TransactionSegment> = _segment.asStateFlow()
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthRows: StateFlow<MonthTransactions> = selectedMonth.flatMapLatest { month ->
        val range = month.transactionTimestampRange()
        repository.getTransactionsForRange(range.first, range.last).map { MonthTransactions(month, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthTransactions(YearMonth.now(), emptyList()))

    private val sourceRows = combine(monthRows, repository.getPendingTransactions(), repository.getDismissedTransactions()) { month, pending, dismissed ->
        Triple(month, pending, dismissed)
    }

    val sourceTransactions: StateFlow<List<TransactionWithDetails>> = combine(sourceRows, _segment, selectedMonth) { sources, segment, selected ->
        val (month, pending, dismissed) = sources
        when (segment) {
            TransactionSegment.ALL -> if (month.month == selected) month.rows.filter { it.transaction.status == TransactionStatus.CONFIRMED } else emptyList()
            TransactionSegment.PENDING -> pending
            TransactionSegment.DISMISSED -> dismissed
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(sourceTransactions, _filter) { source, filter ->
        source.filter { item -> matchesTransactionFilter(item, filter) }.sortedForPresentation()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.getPendingCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            categories.collect { values ->
                val validIds = values.mapTo(mutableSetOf()) { it.id }
                _filter.value = _filter.value.copy(categoryIds = _filter.value.categoryIds.intersect(validIds))
            }
        }
    }

    val summary: StateFlow<TransactionListSummary> = filteredTransactions.map(::summarizeExpensesByCurrency)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionListSummary())

    fun onPreviousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun onNextMonth() { _selectedMonth.value = _selectedMonth.value.plusMonths(1) }
    fun onResetToCurrentMonth() { _selectedMonth.value = YearMonth.now() }
    fun onMonthSelected(month: YearMonth) { _selectedMonth.value = month }
    fun selectSegment(value: TransactionSegment) { _segment.value = value }
    fun onSearchQueryChanged(query: String) { _filter.value = _filter.value.copy(searchQuery = query) }
    fun toggleCategory(id: Long) { _filter.value = _filter.value.copy(categoryIds = _filter.value.categoryIds.toggle(id)) }
    fun togglePaymentMode(id: Long) { _filter.value = _filter.value.copy(paymentModeIds = _filter.value.paymentModeIds.toggle(id)) }
    fun setType(value: TransactionType?) { _filter.value = _filter.value.copy(type = value) }
    fun setHasSplit(value: Boolean?) { _filter.value = _filter.value.copy(hasSplit = value) }
    fun removeCategory(id: Long) { _filter.value = _filter.value.copy(categoryIds = _filter.value.categoryIds - id) }
    fun removePaymentMode(id: Long) { _filter.value = _filter.value.copy(paymentModeIds = _filter.value.paymentModeIds - id) }
    fun clearFilters() { _filter.value = TransactionListFilter() }
    fun deleteTransaction(item: TransactionWithDetails) { viewModelScope.launch { repository.deleteTransaction(item.transaction) } }
}

internal fun summarizeExpensesByCurrency(rows: List<TransactionWithDetails>): TransactionListSummary =
    TransactionListSummary(rows.asSequence().filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.transaction.currency.uppercase() }
        .map { (currency, transactions) ->
            CurrencyExpenseSummary(currency, transactions.sumOf { it.transaction.myShareAmount },
                transactions.sumOf { it.transaction.originalAmount }, transactions.size)
        }.sortedBy { it.currencyCode })

internal fun matchesTransactionFilter(item: TransactionWithDetails, filter: TransactionListFilter): Boolean {
    val t = item.transaction
    val income = t.type == TransactionType.INCOME
    val query = filter.searchQuery.trim()
    val matchesQuery = query.isEmpty() || t.description.contains(query, true) ||
        (t.notes?.contains(query, true) == true) || (!income && item.category?.name?.contains(query, true) == true)
    // Income has no expense category, so selecting a category intentionally excludes it.
    val matchesCategory = filter.categoryIds.isEmpty() || (!income && t.categoryId in filter.categoryIds)
    val matchesPayment = filter.paymentModeIds.isEmpty() || t.paymentModeId in filter.paymentModeIds
    val matchesType = filter.type == null || t.type == filter.type
    val matchesSplit = filter.hasSplit == null || (item.isSplit == filter.hasSplit)
    return matchesQuery && matchesCategory && matchesPayment && matchesType && matchesSplit
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id
