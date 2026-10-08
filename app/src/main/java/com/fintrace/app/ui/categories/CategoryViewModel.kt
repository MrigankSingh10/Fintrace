package com.fintrace.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.dao.CategoryDeleteResult
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryUiState(
    val isEditing: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val errorMessage: String? = null,
    val deletingCategoryId: Long? = null
)

class CategoryViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun onAddCategoryClicked() {
        _uiState.value = _uiState.value.copy(isEditing = true, editingCategory = null, errorMessage = null)
    }

    fun onEditCategoryClicked(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(isEditing = true, editingCategory = category, errorMessage = null)
    }

    fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(isEditing = false, editingCategory = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveCategory(name: String, colorHex: String, iconName: String, budgetAmount: Double?) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Category name cannot be empty")
            return
        }
        val normalizedBudget = budgetAmount?.takeIf { it > 0.0 }

        viewModelScope.launch {
            val current = _uiState.value.editingCategory
            if (current == null) {
                // Insert new
                val newCategory = CategoryEntity(
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName,
                    isDefault = false,
                    displayOrder = (categories.value.maxOfOrNull { it.displayOrder } ?: 0) + 1,
                    budgetAmount = normalizedBudget
                )
                repository.addCategory(newCategory)
            } else {
                // Update existing
                val updated = current.copy(
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName,
                    budgetAmount = normalizedBudget
                )
                repository.updateCategory(updated)
            }
            onDismissDialog()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (_uiState.value.deletingCategoryId != null) return
        _uiState.value = _uiState.value.copy(deletingCategoryId = category.id)
        viewModelScope.launch {
            val result = runCatching { repository.deleteCategory(category) }
            val failureMessage = when {
                result.isFailure -> "Couldn't delete '${category.name}'. Please try again."
                result.getOrNull() == CategoryDeleteResult.LAST_CATEGORY ->
                    "You need at least one category. Add another category before deleting this one."
                else -> null
            }
            _uiState.value = _uiState.value.copy(
                deletingCategoryId = null,
                errorMessage = failureMessage ?: _uiState.value.errorMessage
            )
        }
    }
}
