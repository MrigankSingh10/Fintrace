package com.fintrace.app.ui.paymentmodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaymentModeUiState(
    val isEditing: Boolean = false,
    val editingMode: PaymentModeEntity? = null,
    val errorMessage: String? = null
)

class PaymentModeViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val paymentModes: StateFlow<List<PaymentModeEntity>> = repository.getAllPaymentModes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(PaymentModeUiState())
    val uiState: StateFlow<PaymentModeUiState> = _uiState.asStateFlow()

    fun onAddModeClicked() {
        _uiState.value = PaymentModeUiState(isEditing = true, editingMode = null)
    }

    fun onEditModeClicked(mode: PaymentModeEntity) {
        _uiState.value = PaymentModeUiState(isEditing = true, editingMode = mode)
    }

    fun onDismissDialog() {
        _uiState.value = PaymentModeUiState(isEditing = false, editingMode = null)
    }

    fun savePaymentMode(name: String, type: PaymentModeType, iconName: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Payment mode name cannot be empty")
            return
        }

        viewModelScope.launch {
            val current = _uiState.value.editingMode
            if (current == null) {
                repository.addPaymentMode(
                    PaymentModeEntity(
                        name = trimmed,
                        type = type,
                        iconName = iconName,
                        isDefault = false
                    )
                )
            } else {
                repository.updatePaymentMode(
                    current.copy(
                        name = trimmed,
                        type = type,
                        iconName = iconName
                    )
                )
            }
            onDismissDialog()
        }
    }

    fun deletePaymentMode(mode: PaymentModeEntity) {
        viewModelScope.launch {
            repository.deletePaymentMode(mode)
        }
    }
}
