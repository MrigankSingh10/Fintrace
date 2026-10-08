package com.fintrace.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.fallbackCategoryId
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.sms.SmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import java.util.UUID
import kotlin.math.round

private fun String.toFiniteDoubleOrNull(): Double? = toDoubleOrNull()?.takeIf(Double::isFinite)

data class SplitParticipantItem(
    val id: Long = 0,
    val name: String,
    val shareAmount: String,
    val isUser: Boolean = false
)

/** All values which are editable in the transaction sheet. */
data class TransactionDraft(
    val description: String,
    val originalAmount: String,
    val myShareAmount: String,
    val isSplit: Boolean,
    val selectedCategoryId: Long,
    val selectedPaymentModeId: Long,
    val type: TransactionType,
    val timestamp: Long,
    val notes: String,
    val splitParticipants: List<SplitParticipantItem>,
    val isRecurring: Boolean
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
    val splitParticipants: List<SplitParticipantItem> = emptyList(),
    val currency: String = "INR",
    val isRecurring: Boolean = false,
    val recurringSeriesId: String? = null,
    val recurringDayOfMonth: Int? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isSaved: Boolean = false,
    val isDirty: Boolean = false,
    val isInitialized: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val shareError: String? = null,
    val participantErrors: Map<Int, String> = emptyMap(),
    val participantTotalAmount: Double = 0.0,
    val participantTotalIsFinite: Boolean = true,
    val personalShareValue: Double = 0.0
) {
    val isBusy: Boolean get() = isLoading || isSaving || isDeleting
    val canEdit: Boolean get() = isInitialized && !isBusy && !isSaved
    val canSave: Boolean get() {
        val bill = originalAmount.toFiniteDoubleOrNull() ?: return false
        if (!isInitialized || isBusy || isSaved || description.isBlank() || selectedCategoryId <= 0L || selectedPaymentModeId <= 0L || bill <= 0.0) return false
        if (!isSplit) return true
        val own = myShareAmount.toFiniteDoubleOrNull() ?: return false
        if (own < 0.0 || own > bill) return false
        var others = 0.0
        for (row in splitParticipants) {
            val share = row.shareAmount.toFiniteDoubleOrNull() ?: return false
            if (row.name.isBlank() || share <= 0.0 || share > bill) return false
            others += share
        }
        return others.isFinite() && others <= bill
    }
}

class AddEditTransactionViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val paymentModes: StateFlow<List<PaymentModeEntity>> = repository.getAllPaymentModes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private var loadedTransactionId: Long? = null
    private var originalEntity: TransactionEntity? = null
    private var baseline: TransactionDraft? = null
    private var categoryTouched = false
    private var paymentModeTouched = false
    private var personalShareExplicitlyEdited = false

    init {
        viewModelScope.launch {
            categories.collect { values ->
                reconcileCategorySelection(values)
            }
        }
        viewModelScope.launch {
            paymentModes.collect { values ->
                if (values.isNotEmpty() && !paymentModeTouched && _uiState.value.selectedPaymentModeId == 0L) {
                    update { state -> state.copy(selectedPaymentModeId = values.first().id).also {
                        baseline = baseline?.copy(selectedPaymentModeId = values.first().id)
                    } }
                }
            }
        }
    }

    /** Safe to call from recomposition; a loaded or edited draft is never reinitialized. */
    fun loadTransaction(transactionId: Long) {
        if (loadedTransactionId == transactionId) return
        loadedTransactionId = transactionId
        categoryTouched = false
        paymentModeTouched = false
        personalShareExplicitlyEdited = false
        baseline = null
        originalEntity = null
        if (transactionId <= 0L) {
            val initialCategoryId = fallbackCategoryId(categories.value) ?: 0L
            val initialPaymentModeId = paymentModes.value.firstOrNull()?.id ?: 0L
            val state = AddEditTransactionUiState(
                transactionId = 0L,
                timestamp = System.currentTimeMillis(),
                selectedCategoryId = initialCategoryId,
                selectedPaymentModeId = initialPaymentModeId,
                isInitialized = true
            )
            _uiState.value = state.withDerivedValues()
            baseline = state.toDraft()
            return
        }

        _uiState.value = AddEditTransactionUiState(
            transactionId = transactionId,
            isLoading = true
        )
        viewModelScope.launch {
            runCatching { repository.getTransactionById(transactionId) }
                .onSuccess { details ->
                    if (loadedTransactionId != transactionId) return@onSuccess
                    if (details == null) {
                        loadedTransactionId = null
                        update { it.copy(isLoading = false, errorMessage = "Transaction not found") }
                    } else {
                        val entity = details.transaction
                        originalEntity = entity
                        val rows = details.splits.filterNot { it.isUser }.map {
                            SplitParticipantItem(
                                id = it.id,
                                name = it.personName,
                                shareAmount = it.shareAmount.toEditorText()
                            )
                        }
                        val loaded = AddEditTransactionUiState(
                            transactionId = entity.id,
                            description = descriptionForEditing(entity),
                            originalAmount = entity.originalAmount.toEditorText(),
                            myShareAmount = entity.myShareAmount.toEditorText(),
                            isSplit = details.isSplit,
                            selectedCategoryId = entity.categoryId,
                            selectedPaymentModeId = entity.paymentModeId,
                            type = entity.type,
                            timestamp = entity.timestamp,
                            notes = entity.notes.orEmpty(),
                            splitParticipants = rows,
                            currency = entity.currency,
                            isRecurring = entity.isRecurring,
                            recurringSeriesId = entity.recurringSeriesId,
                            recurringDayOfMonth = entity.recurringDayOfMonth,
                            isInitialized = true
                        )
                        _uiState.value = loaded.withDerivedValues()
                        baseline = loaded.toDraft()
                        categoryTouched = false
                        paymentModeTouched = false
                        reconcileCategorySelection(categories.value)
                    }
                }
                .onFailure {
                    if (loadedTransactionId == transactionId) {
                        loadedTransactionId = null
                        update { it.copy(isLoading = false, errorMessage = "Could not load this transaction. Try again.") }
                    }
                }
        }
    }

    fun resetSaved() { update { it.copy(isSaved = false) } }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onOriginalAmountChange(value: String) = edit { state ->
        val nextMyShare = when {
            !state.isSplit -> value
            personalShareExplicitlyEdited -> state.myShareAmount
            else -> automaticRemainder(value, state.splitParticipants) ?: state.myShareAmount
        }
        state.copy(originalAmount = value, myShareAmount = nextMyShare)
    }
    fun onMyShareAmountChange(value: String) {
        edit {
            personalShareExplicitlyEdited = true
            it.copy(myShareAmount = value)
        }
    }
    fun onSplitToggle(isSplit: Boolean) {
        edit {
            if (!isSplit) personalShareExplicitlyEdited = false
            it.copy(isSplit = isSplit, myShareAmount = if (!isSplit) it.originalAmount else it.myShareAmount)
        }
    }

    fun onQuickSplit(divisor: Int) = edit { state ->
        val bill = state.originalAmount.toFiniteDoubleOrNull() ?: return@edit state
        if (bill <= 0.0 || divisor <= 1 || bill >= Long.MAX_VALUE / 100.0) return@edit state
        val myShare = round((bill / divisor) * 100.0) / 100.0
        var remainingCents = (round(bill * 100.0) - round(myShare * 100.0)).toLong()
        val otherCount = divisor - 1
        val rows = (1..otherCount).map { index ->
            val cents = if (index == otherCount) remainingCents else (remainingCents / (otherCount - index + 1))
            remainingCents -= cents
            SplitParticipantItem(name = "Person $index", shareAmount = (cents / 100.0).toEditorText())
        }
        personalShareExplicitlyEdited = false
        state.copy(isSplit = true, myShareAmount = myShare.toEditorText(), splitParticipants = rows)
    }

    fun onAddParticipant() = edit { state ->
        val rows = state.splitParticipants + SplitParticipantItem(name = "", shareAmount = "")
        state.copy(
            isSplit = true,
            splitParticipants = rows,
            myShareAmount = if (personalShareExplicitlyEdited) state.myShareAmount else automaticRemainder(state.originalAmount, rows) ?: state.myShareAmount
        )
    }

    fun onUpdateParticipant(index: Int, name: String, share: String) = edit { state ->
        if (index !in state.splitParticipants.indices) return@edit state
        val rows = state.splitParticipants.toMutableList()
        rows[index] = rows[index].copy(name = name, shareAmount = share)
        state.copy(
            splitParticipants = rows,
            myShareAmount = if (personalShareExplicitlyEdited) state.myShareAmount else automaticRemainder(state.originalAmount, rows) ?: state.myShareAmount
        )
    }

    fun onRemoveParticipant(index: Int) = edit { state ->
        if (index !in state.splitParticipants.indices) return@edit state
        val rows = state.splitParticipants.toMutableList().also { it.removeAt(index) }
        state.copy(
            splitParticipants = rows,
            myShareAmount = if (personalShareExplicitlyEdited) state.myShareAmount else automaticRemainder(state.originalAmount, rows) ?: state.myShareAmount
        )
    }

    fun onCategorySelect(id: Long) { categoryTouched = true; edit { it.copy(selectedCategoryId = id) } }
    fun onPaymentModeSelect(id: Long) { paymentModeTouched = true; edit { it.copy(selectedPaymentModeId = id) } }
    fun onTypeSelect(type: TransactionType) = edit { it.copy(type = type) }
    fun onTimestampChange(timestamp: Long) = edit { it.copy(timestamp = timestamp) }
    fun onNotesChange(value: String) = edit { it.copy(notes = value) }
    fun onRecurringToggle(value: Boolean) = edit { it.copy(isRecurring = value) }

    fun saveTransaction() {
        val state = _uiState.value
        if (state.isBusy || state.isSaved || !state.isInitialized || state.errorMessage?.startsWith("Could not load") == true) return
        val validation = validate(state)
        if (validation != null) {
            _uiState.value = state.copy(
                amountError = validation.amountError,
                shareError = validation.shareError,
                participantErrors = validation.participantErrors,
                errorMessage = validation.generalError
            )
            return
        }
        val bill = state.originalAmount.toFiniteDoubleOrNull()!!
        val myShare = if (state.isSplit) state.myShareAmount.toFiniteDoubleOrNull()!! else bill
        val loaded = originalEntity
        val entity = (loaded ?: TransactionEntity(
            description = "",
            timestamp = state.timestamp,
            originalAmount = bill,
            myShareAmount = myShare,
            categoryId = state.selectedCategoryId,
            paymentModeId = state.selectedPaymentModeId
        )).copy(
            id = state.transactionId,
            description = state.description.trim(),
            timestamp = state.timestamp,
            originalAmount = bill,
            myShareAmount = myShare,
            categoryId = state.selectedCategoryId,
            paymentModeId = state.selectedPaymentModeId,
            type = state.type,
            status = TransactionStatus.CONFIRMED,
            notes = state.notes.trim().ifBlank { null },
            isRecurring = state.isRecurring,
            recurringSeriesId = if (state.isRecurring) state.recurringSeriesId ?: UUID.randomUUID().toString() else null,
            recurringDayOfMonth = if (state.isRecurring) state.recurringDayOfMonth ?: Calendar.getInstance().apply { timeInMillis = state.timestamp }.get(Calendar.DAY_OF_MONTH) else null
        )
        val splits = if (state.isSplit) buildList {
            add(TransactionSplitEntity(transactionId = state.transactionId, personName = "Me", shareAmount = myShare, isUser = true))
            state.splitParticipants.forEach { row ->
                add(TransactionSplitEntity(
                    transactionId = state.transactionId,
                    personName = row.name.trim(),
                    shareAmount = row.shareAmount.toFiniteDoubleOrNull()!!,
                    isUser = false
                ))
            }
        } else emptyList()

        update { it.copy(isSaving = true, amountError = null, shareError = null, participantErrors = emptyMap(), errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.saveTransaction(entity, splits) }
                .onSuccess {
                    baseline = _uiState.value.toDraft()
                    originalEntity = entity
                    update { it.copy(isSaving = false, isSaved = true, isDirty = false) }
                }
                .onFailure {
                    update { it.copy(isSaving = false, errorMessage = "Could not save this transaction. Your changes are still here.") }
                }
        }
    }

    fun deleteTransaction() {
        val state = _uiState.value
        val entity = originalEntity
        if (state.isBusy || state.isSaved || state.transactionId <= 0L || entity == null) return
        update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.deleteTransaction(entity) }
                .onSuccess {
                    baseline = _uiState.value.toDraft()
                    update { it.copy(isDeleting = false, isSaved = true, isDirty = false) }
                }
                .onFailure { update { it.copy(isDeleting = false, errorMessage = "Could not delete this transaction. Try again.") } }
        }
    }

    private data class Validation(
        val amountError: String? = null,
        val shareError: String? = null,
        val participantErrors: Map<Int, String> = emptyMap(),
        val generalError: String? = null
    )

    private fun validate(state: AddEditTransactionUiState): Validation? {
        if (state.description.isBlank()) return Validation(generalError = "Enter a merchant or description.")
        val bill = state.originalAmount.toFiniteDoubleOrNull()
            ?: return Validation(amountError = "Enter a valid amount.")
        if (bill <= 0.0) return Validation(amountError = "Full bill must be greater than zero.")
        if (!state.isSplit) return null
        val own = state.myShareAmount.toFiniteDoubleOrNull()
            ?: return Validation(shareError = "Enter a valid personal share.")
        if (own < 0.0 || own > bill) return Validation(shareError = "Your share must be between zero and the full bill.")
        var sum = 0.0
        val errors = mutableMapOf<Int, String>()
        state.splitParticipants.forEachIndexed { index, row ->
            val amount = row.shareAmount.toFiniteDoubleOrNull()
            when {
                row.name.isBlank() -> errors[index] = "Enter a participant name."
                amount == null || amount <= 0.0 -> errors[index] = "Enter a share greater than zero."
                amount > bill -> errors[index] = "A share cannot exceed the full bill."
                else -> sum += amount
            }
        }
        if (sum > bill) return Validation(shareError = "Participant shares cannot exceed the full bill.", participantErrors = errors)
        if (errors.isNotEmpty()) return Validation(participantErrors = errors)
        return null
    }

    private fun edit(transform: (AddEditTransactionUiState) -> AddEditTransactionUiState) {
        if (_uiState.value.isBusy || !_uiState.value.isInitialized) return
        update { state -> transform(state).copy(errorMessage = null, amountError = null, shareError = null, participantErrors = emptyMap()) }
    }

    private fun update(transform: (AddEditTransactionUiState) -> AddEditTransactionUiState) {
        _uiState.value = transform(_uiState.value).withDerivedValues().let { state ->
            val savedBaseline = baseline
            if (savedBaseline == null || !state.isInitialized) state else state.copy(isDirty = state.toDraft() != savedBaseline)
        }
    }

    private fun reconcileCategorySelection(values: List<CategoryEntity>) {
        val state = _uiState.value
        if (!state.isInitialized || values.isEmpty() || values.any { it.id == state.selectedCategoryId }) return
        val fallbackId = fallbackCategoryId(values) ?: return
        _uiState.value = state.copy(selectedCategoryId = fallbackId).withDerivedValues().let { next ->
            val savedBaseline = baseline
            if (savedBaseline == null) next else next.copy(isDirty = next.toDraft() != savedBaseline.copy(selectedCategoryId = fallbackId))
        }
        baseline = baseline?.copy(selectedCategoryId = fallbackId)
    }

    private fun AddEditTransactionUiState.toDraft() = TransactionDraft(
        description, originalAmount, myShareAmount, isSplit, selectedCategoryId,
        selectedPaymentModeId, type, timestamp, notes, splitParticipants.toList(), isRecurring
    )

    private fun Double.toEditorText(): String =
        if (isFinite()) BigDecimal.valueOf(this).stripTrailingZeros().toPlainString() else toString()

    private fun automaticRemainder(
        billText: String,
        participants: List<SplitParticipantItem>
    ): String? {
        val bill = billText.toFiniteDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
        var others = 0.0
        for (participant in participants) {
            val share = if (participant.shareAmount.isBlank()) 0.0 else participant.shareAmount.toFiniteDoubleOrNull() ?: return null
            others += share
            if (!others.isFinite()) return null
        }
        val remainder = (bill - others).coerceAtLeast(0.0)
        if (!remainder.isFinite()) return null
        return remainder.toEditorText()
    }

    private fun descriptionForEditing(transaction: TransactionEntity): String {
        var description = transaction.description.trim()
        val isGenericOrNumeric = description.isBlank() ||
            description.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE)) ||
            description.matches(Regex("^[0-9\\.,\\s\\-_]+$")) ||
            description.equals("ICICI Bank Credit", ignoreCase = true) ||
            description.equals("HDFC Bank Credit", ignoreCase = true) ||
            description.equals("SBI Credit", ignoreCase = true) ||
            description.equals("Bank / Card Expense", ignoreCase = true) ||
            description.equals("Bank Credit / Dividend", ignoreCase = true)
        val rawBody = transaction.smsRawBody
        if (isGenericOrNumeric && !rawBody.isNullOrBlank()) {
            val parsed = SmsParser.parse(rawBody, transaction.smsSender, transaction.timestamp)
            if (parsed != null && parsed.merchant.isNotBlank() &&
                !parsed.merchant.matches(Regex("^(?:INR|RS\\.?|₹)?\\s*[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?\\s*$", RegexOption.IGNORE_CASE))
            ) {
                description = parsed.merchant
            }
        }
        return description
    }

    private fun AddEditTransactionUiState.withDerivedValues(): AddEditTransactionUiState {
        val bill = originalAmount.toFiniteDoubleOrNull()
        val own = myShareAmount.toFiniteDoubleOrNull()
        val otherAmounts = splitParticipants.map { it.shareAmount.toFiniteDoubleOrNull() }
        val totalOthers = otherAmounts.fold(0.0) { total, amount -> total + (amount ?: 0.0) }
        val amountError = when {
            originalAmount.isBlank() -> if (isInitialized) "Enter an amount greater than zero." else null
            bill == null -> "Enter a valid amount."
            bill <= 0.0 -> "Amount must be greater than zero."
            else -> null
        }
        val liveShareError = if (!isSplit) null else when {
            myShareAmount.isBlank() -> "Enter your share."
            own == null || own < 0.0 -> "Enter a valid personal share."
            bill != null && own > bill -> "Your share cannot exceed the full bill."
            bill != null && totalOthers > bill -> "Participant shares cannot exceed the full bill."
            else -> null
        }
        val liveParticipantErrors = splitParticipants.mapIndexedNotNull { index, row ->
            val share = otherAmounts[index]
            val message = when {
                row.name.isBlank() && row.shareAmount.isNotBlank() -> "Enter a participant name."
                row.name.isNotBlank() && (share == null || share <= 0.0) -> "Enter a share greater than zero."
                bill != null && share != null && share > bill -> "A share cannot exceed the full bill."
                else -> null
            }
            message?.let { index to it }
        }.toMap()
        return copy(
            amountError = amountError,
            shareError = liveShareError,
            participantErrors = liveParticipantErrors,
            participantTotalAmount = totalOthers.takeIf(Double::isFinite) ?: 0.0,
            participantTotalIsFinite = totalOthers.isFinite(),
            personalShareValue = own ?: 0.0
        )
    }
}
