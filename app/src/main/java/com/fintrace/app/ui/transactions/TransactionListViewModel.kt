package com.fintrace.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionListFilter(
    val searchQuery: String = "",
    val categoryId: Long? = null,
    val paymentModeId: Long? = null
)

data class TransactionListSummary(
    val totalMyShareSpent: Double = 0.0,
    val totalOriginalCharged: Double = 0.0,
    val transactionCount: Int = 0
)

class TransactionListViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val paymentModes: StateFlow<List<PaymentModeEntity>> = repository.getAllPaymentModes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _filter = MutableStateFlow(TransactionListFilter())
    val filter: StateFlow<TransactionListFilter> = _filter.asStateFlow()

    private val allConfirmedTransactions = repository.getConfirmedTransactions()

    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        allConfirmedTransactions,
        _filter
    ) { transactions, filter ->
        transactions.filter { item ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    item.transaction.description.contains(filter.searchQuery, ignoreCase = true) ||
                    (item.transaction.notes?.contains(filter.searchQuery, ignoreCase = true) == true) ||
                    (item.category?.name?.contains(filter.searchQuery, ignoreCase = true) == true)

            val matchesCategory = filter.categoryId == null || item.transaction.categoryId == filter.categoryId
            val matchesPaymentMode = filter.paymentModeId == null || item.transaction.paymentModeId == filter.paymentModeId

            matchesQuery && matchesCategory && matchesPaymentMode
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val summary: StateFlow<TransactionListSummary> = filteredTransactions.combine(_filter) { list, _ ->
        val expenses = list.filter { it.transaction.type != com.fintrace.app.data.model.TransactionType.INCOME }
        val myShare = expenses.sumOf { it.transaction.myShareAmount }
        val original = expenses.sumOf { it.transaction.originalAmount }
        TransactionListSummary(
            totalMyShareSpent = myShare,
            totalOriginalCharged = original,
            transactionCount = list.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListSummary()
    )

    fun onSearchQueryChanged(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun onCategoryFilterChanged(categoryId: Long?) {
        _filter.value = _filter.value.copy(categoryId = if (_filter.value.categoryId == categoryId) null else categoryId)
    }

    fun onPaymentModeFilterChanged(paymentModeId: Long?) {
        _filter.value = _filter.value.copy(paymentModeId = if (_filter.value.paymentModeId == paymentModeId) null else paymentModeId)
    }

    fun clearFilters() {
        _filter.value = TransactionListFilter()
    }

    fun deleteTransaction(item: TransactionWithDetails) {
        viewModelScope.launch {
            repository.deleteTransaction(item.transaction)
        }
    }
}
