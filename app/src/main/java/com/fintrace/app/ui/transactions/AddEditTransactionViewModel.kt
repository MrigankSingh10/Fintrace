package com.fintrace.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SplitParticipantItem(
    val id: Long = 0,
    val name: String,
    val shareAmount: String,
    val isUser: Boolean = false
)

data class AddEditTransactionUiState(
    val transactionId: Long = 0,
    val description: String = "",
    val originalAmount: String = "",
    val myShareAmount: String = "",
    val isSplit: Boolean = false,
    val selectedCategoryId: Long = 0,
    val selectedPaymentModeId: Long = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val smsRawBody: String? = null,
    val smsSender: String? = null,
    val splitParticipants: List<SplitParticipantItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddEditTransactionViewModel(
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

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        if (transactionId <= 0) {
            // New transaction: wait for categories/modes to arrive from DB before setting defaults.
            viewModelScope.launch {
                // Reset to a blank state first so the screen shows cleanly while loading.
                _uiState.value = AddEditTransactionUiState(
                    transactionId = 0L,
                    timestamp = System.currentTimeMillis(),
                    isSaved = false,
                    errorMessage = null
                )
                // Collect first non-empty lists (they come from Room Flow so may need a tick).
                val cats = categories.first { it.isNotEmpty() }
                val modes = paymentModes.first { it.isNotEmpty() }
                _uiState.value = _uiState.value.copy(
                    selectedCategoryId = cats.first().id,
                    selectedPaymentModeId = modes.first().id
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isSaved = false, errorMessage = null)
            val details = repository.getTransactionById(transactionId)
            if (details != null) {
                val t = details.transaction
                val isSplit = details.isSplit
                val participantItems = details.splits
                    .filter { !it.isUser }
                    .map { SplitParticipantItem(id = it.id, name = it.personName, shareAmount = if (it.shareAmount % 1.0 == 0.0) String.format("%.0f", it.shareAmount) else it.shareAmount.toString(), isUser = false) }

                val origStr = if (t.originalAmount % 1.0 == 0.0) String.format("%.0f", t.originalAmount) else t.originalAmount.toString()
                val myShareStr = if (t.myShareAmount % 1.0 == 0.0) String.format("%.0f", t.myShareAmount) else t.myShareAmount.toString()

                var finalDescription = t.description.trim()
                val isGenericOrNumeric = finalDescription.isBlank() ||
                        finalDescription.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)) ||
                        finalDescription.matches(Regex("^[0-9\\.,\\s\\-_]+$")) ||
                        finalDescription.equals("ICICI Bank Credit", ignoreCase = true) ||
                        finalDescription.equals("HDFC Bank Credit", ignoreCase = true) ||
                        finalDescription.equals("SBI Credit", ignoreCase = true) ||
                        finalDescription.equals("Bank / Card Expense", ignoreCase = true) ||
                        finalDescription.equals("Bank Credit / Dividend", ignoreCase = true)

                if (isGenericOrNumeric && !t.smsRawBody.isNullOrBlank()) {
                    val reParsed = com.fintrace.app.data.sms.SmsParser.parse(t.smsRawBody, t.smsSender, t.timestamp)
                    if (reParsed != null && reParsed.merchant.isNotBlank() && !reParsed.merchant.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE))) {
                        finalDescription = reParsed.merchant
                    }
                }

                _uiState.value = AddEditTransactionUiState(
                    transactionId = t.id,
                    description = finalDescription,
                    originalAmount = origStr,
                    myShareAmount = myShareStr,
                    isSplit = isSplit,
                    selectedCategoryId = t.categoryId,
                    selectedPaymentModeId = t.paymentModeId,
                    type = t.type,
                    timestamp = t.timestamp,
                    notes = t.notes ?: "",
                    smsRawBody = t.smsRawBody,
                    smsSender = t.smsSender,
                    splitParticipants = participantItems,
                    isLoading = false,
                    isSaved = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Transaction not found")
            }
        }
    }

    fun resetSaved() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, errorMessage = null)
    }

    fun onOriginalAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        val original = filtered.toDoubleOrNull() ?: 0.0
        val participants = _uiState.value.splitParticipants

        val newMyShare = if (!_uiState.value.isSplit || participants.isEmpty()) {
            filtered
        } else {
            val otherSum = participants.sumOf { it.shareAmount.toDoubleOrNull() ?: 0.0 }
            val remaining = (original - otherSum).coerceAtLeast(0.0)
            if (remaining % 1.0 == 0.0) String.format("%.0f", remaining) else String.format("%.2f", remaining)
        }

        _uiState.value = _uiState.value.copy(
            originalAmount = filtered,
            myShareAmount = newMyShare,
            errorMessage = null
        )
    }

    fun onMyShareAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(myShareAmount = filtered, errorMessage = null)
    }

    fun onSplitToggle(isSplit: Boolean) {
        val original = _uiState.value.originalAmount
        _uiState.value = _uiState.value.copy(
            isSplit = isSplit,
            myShareAmount = if (!isSplit) original else _uiState.value.myShareAmount
        )
    }

    fun onQuickSplit(divisor: Int) {
        val original = _uiState.value.originalAmount.toDoubleOrNull() ?: return
        if (divisor <= 1) return

        val myShare = original / divisor
        val formattedMyShare = if (myShare % 1.0 == 0.0) String.format("%.0f", myShare) else String.format("%.2f", myShare)

        val otherShare = (original - myShare) / (divisor - 1)
        val formattedOtherShare = if (otherShare % 1.0 == 0.0) String.format("%.0f", otherShare) else String.format("%.2f", otherShare)

        val newParticipants = (1 until divisor).map { index ->
            SplitParticipantItem(
                name = "Person $index",
                shareAmount = formattedOtherShare,
                isUser = false
            )
        }

        _uiState.value = _uiState.value.copy(
            isSplit = true,
            myShareAmount = formattedMyShare,
            splitParticipants = newParticipants
        )
    }

    fun onAddParticipant() {
        val current = _uiState.value.splitParticipants.toMutableList()
        current.add(
            SplitParticipantItem(
                name = "Person ${current.size + 1}",
                shareAmount = "",
                isUser = false
            )
        )
        _uiState.value = _uiState.value.copy(
            isSplit = true,
            splitParticipants = current
        )
    }

    fun onUpdateParticipant(index: Int, name: String, share: String) {
        val current = _uiState.value.splitParticipants.toMutableList()
        if (index in current.indices) {
            val filtered = share.filter { it.isDigit() || it == '.' }
            current[index] = current[index].copy(name = name, shareAmount = filtered)

            // Auto-deduct user's own share: Total Original - Sum of other participants
            val original = _uiState.value.originalAmount.toDoubleOrNull() ?: 0.0
            val otherSum = current.sumOf { it.shareAmount.toDoubleOrNull() ?: 0.0 }
            val remainingMyShare = (original - otherSum).coerceAtLeast(0.0)
            val myShareStr = if (remainingMyShare % 1.0 == 0.0) String.format("%.0f", remainingMyShare) else String.format("%.2f", remainingMyShare)

            _uiState.value = _uiState.value.copy(
                splitParticipants = current,
                myShareAmount = myShareStr
            )
        }
    }

    fun onRemoveParticipant(index: Int) {
        val current = _uiState.value.splitParticipants.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val original = _uiState.value.originalAmount.toDoubleOrNull() ?: 0.0
            val otherSum = current.sumOf { it.shareAmount.toDoubleOrNull() ?: 0.0 }
            val remainingMyShare = (original - otherSum).coerceAtLeast(0.0)
            val myShareStr = if (remainingMyShare % 1.0 == 0.0) String.format("%.0f", remainingMyShare) else String.format("%.2f", remainingMyShare)

            _uiState.value = _uiState.value.copy(
                splitParticipants = current,
                myShareAmount = myShareStr
            )
        }
    }

    fun onCategorySelect(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onPaymentModeSelect(paymentModeId: Long) {
        _uiState.value = _uiState.value.copy(selectedPaymentModeId = paymentModeId)
    }

    fun onTypeSelect(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onTimestampChange(timestamp: Long) {
        _uiState.value = _uiState.value.copy(timestamp = timestamp)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveTransaction() {
        val state = _uiState.value
        val desc = state.description.trim()
        val originalAmt = state.originalAmount.toDoubleOrNull()
        val myShareAmt = if (state.isSplit) state.myShareAmount.toDoubleOrNull() else originalAmt

        if (desc.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a description or merchant name")
            return
        }
        if (originalAmt == null || originalAmt <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }
        if (myShareAmt == null || myShareAmt < 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid personal share amount")
            return
        }

        viewModelScope.launch {
            val transaction = TransactionEntity(
                id = state.transactionId,
                description = desc,
                timestamp = state.timestamp,
                originalAmount = originalAmt,
                myShareAmount = myShareAmt,
                categoryId = state.selectedCategoryId,
                paymentModeId = state.selectedPaymentModeId,
                type = state.type,
                smsRawBody = state.smsRawBody,
                smsSender = state.smsSender,
                status = TransactionStatus.CONFIRMED,
                notes = state.notes.ifBlank { null }
            )

            val splits = mutableListOf<TransactionSplitEntity>()
            if (state.isSplit) {
                // Add User's share
                splits.add(
                    TransactionSplitEntity(
                        transactionId = state.transactionId,
                        personName = "Me",
                        shareAmount = myShareAmt,
                        isUser = true
                    )
                )
                // Add other participants
                state.splitParticipants.forEach { p ->
                    val amt = p.shareAmount.toDoubleOrNull() ?: 0.0
                    if (p.name.isNotBlank() && amt > 0.0) {
                        splits.add(
                            TransactionSplitEntity(
                                transactionId = state.transactionId,
                                personName = p.name.trim(),
                                shareAmount = amt,
                                isUser = false
                            )
                        )
                    }
                }
            }

            repository.saveTransaction(transaction, splits)
            _uiState.value = state.copy(isSaved = true)
        }
    }

    fun deleteTransaction() {
        val state = _uiState.value
        if (state.transactionId > 0) {
            viewModelScope.launch {
                val entity = repository.getTransactionById(state.transactionId)?.transaction
                if (entity != null) {
                    repository.deleteTransaction(entity)
                }
                _uiState.value = state.copy(isSaved = true)
            }
        }
    }
}
