package com.fintrace.app.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.sms.SmsInboxScanner
import com.fintrace.app.data.sms.SmsScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmsInboxUiState(
    val isScanning: Boolean = false,
    val scanResult: SmsScanResult? = null,
    val showPermissionRationale: Boolean = false,
    val statusMessage: String? = null
)

class SmsInboxViewModel(
    private val repository: FinanceRepository,
    private val database: AppDatabase
) : ViewModel() {

    val pendingTransactions: StateFlow<List<TransactionWithDetails>> = repository.getPendingTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    private val _uiState = MutableStateFlow(SmsInboxUiState())
    val uiState: StateFlow<SmsInboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            pendingTransactions.collect { list ->
                list.forEach { item ->
                    val t = item.transaction
                    val desc = t.description.trim()
                    val isGenericOrNumeric = desc.isBlank() ||
                            desc.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)) ||
                            desc.matches(Regex("^[0-9\\.,\\s\\-_]+$")) ||
                            desc.equals("ICICI Bank Credit", ignoreCase = true) ||
                            desc.equals("HDFC Bank Credit", ignoreCase = true) ||
                            desc.equals("SBI Credit", ignoreCase = true) ||
                            desc.equals("Bank / Card Expense", ignoreCase = true) ||
                            desc.equals("Bank Credit / Dividend", ignoreCase = true)

                    if (isGenericOrNumeric && !t.smsRawBody.isNullOrBlank()) {
                        val parsed = com.fintrace.app.data.sms.SmsParser.parse(t.smsRawBody, t.smsSender, t.timestamp)
                        if (parsed != null && parsed.merchant.isNotBlank() &&
                            !parsed.merchant.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)) &&
                            parsed.merchant != desc
                        ) {
                            val updated = t.copy(description = parsed.merchant)
                            repository.saveTransaction(updated, item.splits)
                        }
                    }
                }
            }
        }
    }

    fun onConfirmTransaction(item: TransactionWithDetails) {
        viewModelScope.launch {
            repository.confirmPendingTransaction(item.transaction, item.splits)
        }
    }

    fun onConfirmAllPending() {
        viewModelScope.launch {
            pendingTransactions.value.forEach { item ->
                repository.confirmPendingTransaction(item.transaction, item.splits)
            }
        }
    }

    fun onQuickCategoryChange(item: TransactionWithDetails, categoryId: Long) {
        viewModelScope.launch {
            val updated = item.transaction.copy(categoryId = categoryId)
            repository.saveTransaction(updated, item.splits)
        }
    }

    fun onQuickPaymentModeChange(item: TransactionWithDetails, paymentModeId: Long) {
        viewModelScope.launch {
            val updated = item.transaction.copy(paymentModeId = paymentModeId)
            repository.saveTransaction(updated, item.splits)
        }
    }

    fun onDismissTransaction(item: TransactionWithDetails) {
        viewModelScope.launch {
            repository.deleteTransaction(item.transaction)
        }
    }

    fun onShowPermissionRationale() {
        _uiState.value = _uiState.value.copy(showPermissionRationale = true)
    }

    fun onDismissPermissionRationale() {
        _uiState.value = _uiState.value.copy(showPermissionRationale = false)
    }

    fun scanInbox(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = "Scanning SMS inbox for bank alerts...")
            val result = SmsInboxScanner.scanInbox(context, repository, database, lookbackDays = 30)
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                scanResult = result,
                statusMessage = "Scan complete: Found ${result.newlyImported} new transactions (${result.totalProcessed} SMS inspected)."
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null, scanResult = null)
    }
}
