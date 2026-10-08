package com.fintrace.app.ui.sms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.sms.SmsInboxScanner
import com.fintrace.app.data.sms.SmsParser
import com.fintrace.app.data.sms.SmsScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import com.fintrace.app.data.model.TransactionStatus

data class SmsInboxUiState(
    val isScanning: Boolean = false,
    val scanResult: SmsScanResult? = null,
    val showPermissionRationale: Boolean = false,
    val statusMessage: String? = null,
    val showingDismissed: Boolean = false,
    val actionError: String? = null,
    val pendingLoaded: Boolean = false
)

enum class PendingAction { CONFIRM, DISMISS }
data class UndoablePendingAction(val transactionId: Long, val action: PendingAction, val description: String)

class SmsInboxViewModel(
    private val repository: FinanceRepository,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmsInboxUiState())
    val uiState: StateFlow<SmsInboxUiState> = _uiState.asStateFlow()
    private val pendingRepositoryFlow = repository.getPendingTransactions()
    private val actionCoordinator = PendingActionCoordinator(
        scope = viewModelScope,
        commit = { id, action -> commitStagedAction(id, action) },
        onError = { error -> _uiState.value = _uiState.value.copy(actionError = error.message ?: "Could not save this review action.") }
    )
    private val bulkInFlightIds = mutableSetOf<Long>()
    private val rawPendingTransactions = pendingRepositoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val pendingSourceTransactions: StateFlow<List<TransactionWithDetails>> = rawPendingTransactions

    val pendingTransactions: StateFlow<List<TransactionWithDetails>> = combine(rawPendingTransactions, actionCoordinator.actions) { rows, actions ->
        val staged = actions.keys
        rows.filterNot { it.transaction.id in staged }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val undoableActions: StateFlow<List<UndoablePendingAction>> = combine(rawPendingTransactions, actionCoordinator.actions) { rows, actions ->
        actions.filterValues { it.phase == PendingActionPhase.UNDOABLE }.mapNotNull { (id, record) ->
            rows.firstOrNull { it.transaction.id == id }?.let { UndoablePendingAction(id, record.action, it.transaction.description) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dismissedTransactions: StateFlow<List<TransactionWithDetails>> = repository.getDismissedTransactions()
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

    val cardMappings: StateFlow<List<com.fintrace.app.data.local.entity.CardMappingEntity>> = repository.getAllCardMappings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            pendingRepositoryFlow.collect { _uiState.value = _uiState.value.copy(pendingLoaded = true) }
        }
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
        stagePendingAction(item, PendingAction.CONFIRM)
    }

    fun onConfirmAllPending(snapshotIds: Set<Long>? = null) {
        viewModelScope.launch {
            val candidates = rawPendingTransactions.value.filter { snapshotIds == null || it.transaction.id in snapshotIds }
                .filterNot { it.transaction.id in actionCoordinator.activeIds() || it.transaction.id in bulkInFlightIds }
            val reserved = candidates.filter { bulkInFlightIds.add(it.transaction.id) }
            reserved.forEach { item ->
                val id = item.transaction.id
                try {
                    val latest = repository.getTransactionById(id) ?: return@forEach
                    if (isPendingForDeferredCommit(latest.transaction.status)) {
                        repository.confirmPendingTransaction(resolvedForConfirm(latest), latest.splits)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    _uiState.value = _uiState.value.copy(actionError = error.message ?: "Could not confirm all pending transactions.")
                } finally {
                    bulkInFlightIds.remove(id)
                }
            }
        }
    }

    /**
     * Re-parses the stored SMS while confirming and persists live-parsed fields so confirmed
     * rows and analytics are consistent with the current parser. Card parses are never stored
     * as DEBIT: a mapping wins, otherwise a CREDIT_CARD-typed mode is ensured.
     */
    private suspend fun resolvedForConfirm(item: TransactionWithDetails): TransactionEntity {
        val t = item.transaction
        val body = t.smsRawBody
        if (body.isNullOrBlank()) return t
        val parsed = SmsParser.parse(body, t.smsSender, t.timestamp) ?: return t

        var updated = t.copy(
            description = if (isGenericDescription(t.description)) parsed.merchant else t.description,
            parseConfidence = parsed.parseConfidence,
            cardLastFour = parsed.cardLastFour ?: t.cardLastFour,
            currency = parsed.currencyCode
        )

        if (parsed.paymentModeType == PaymentModeType.CREDIT_CARD) {
            val mapping = parsed.cardLastFour?.let { repository.getCardMappingByLastFour(it) }
            updated = updated.copy(paymentModeId = mapping?.paymentModeId ?: repository.ensureCreditCardMode())
        }
        return updated
    }

    private fun isGenericDescription(desc: String): Boolean {
        val trimmed = desc.trim()
        if (trimmed.isBlank()) return true
        if (trimmed.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE))) return true
        if (trimmed.matches(Regex("^[0-9\\.,\\s\\-_]+$"))) return true
        return trimmed.equals("ICICI Bank Credit", ignoreCase = true) ||
                trimmed.equals("HDFC Bank Credit", ignoreCase = true) ||
                trimmed.equals("SBI Credit", ignoreCase = true) ||
                trimmed.equals("Bank / Card Expense", ignoreCase = true) ||
                trimmed.equals("Bank Credit / Dividend", ignoreCase = true)
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

    fun onCreateCardMapping(cardLastFour: String, paymentModeId: Long, item: TransactionWithDetails? = null) {
        viewModelScope.launch {
            repository.addCardMapping(
                com.fintrace.app.data.local.entity.CardMappingEntity(
                    cardLastFour = cardLastFour,
                    paymentModeId = paymentModeId
                )
            )
            // Update transaction payment mode if provided
            item?.let {
                val updated = it.transaction.copy(paymentModeId = paymentModeId)
                repository.saveTransaction(updated, it.splits)
            }
        }
    }

    fun onDismissTransaction(item: TransactionWithDetails) {
        stagePendingAction(item, PendingAction.DISMISS)
    }

    private fun stagePendingAction(item: TransactionWithDetails, action: PendingAction) {
        val id = item.transaction.id
        if (id in bulkInFlightIds) return
        _uiState.value = _uiState.value.copy(actionError = null)
        actionCoordinator.stage(id, action, item.transaction.description)
    }

    fun undoPendingAction(transactionId: Long) {
        actionCoordinator.undo(transactionId)
    }

    private suspend fun commitStagedAction(id: Long, action: PendingAction) {
        val latest = repository.getTransactionById(id) ?: return
        if (!isPendingForDeferredCommit(latest.transaction.status)) return
        when (action) {
            PendingAction.CONFIRM -> repository.confirmPendingTransaction(resolvedForConfirm(latest), latest.splits)
            PendingAction.DISMISS -> repository.dismissPendingTransaction(latest.transaction, latest.splits)
        }
    }

    fun clearActionError() { _uiState.value = _uiState.value.copy(actionError = null) }

    fun onRestoreTransaction(item: TransactionWithDetails) {
        viewModelScope.launch {
            repository.restoreDismissedTransaction(item.transaction, item.splits)
        }
    }

    fun onDismissedMessagesToggle() {
        _uiState.value = _uiState.value.copy(showingDismissed = !_uiState.value.showingDismissed)
    }

    fun showPendingTransactions() {
        _uiState.value = _uiState.value.copy(showingDismissed = false)
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
