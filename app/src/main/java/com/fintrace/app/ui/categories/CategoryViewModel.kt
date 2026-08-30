package com.fintrace.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.local.entity.CategoryEntity
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
    val errorMessage: String? = null
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
        _uiState.value = CategoryUiState(isEditing = true, editingCategory = null)
    }

    fun onEditCategoryClicked(category: CategoryEntity) {
        _uiState.value = CategoryUiState(isEditing = true, editingCategory = category)
    }

    fun onDismissDialog() {
        _uiState.value = CategoryUiState(isEditing = false, editingCategory = null)
    }

    fun saveCategory(name: String, colorHex: String, iconName: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Category name cannot be empty")
            return
        }

        viewModelScope.launch {
            val current = _uiState.value.editingCategory
            if (current == null) {
                // Insert new
                val newCategory = CategoryEntity(
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName,
                    isDefault = false,
                    displayOrder = (categories.value.maxOfOrNull { it.displayOrder } ?: 0) + 1
                )
                repository.addCategory(newCategory)
            } else {
                // Update existing
                val updated = current.copy(
                    name = trimmedName,
                    colorHex = colorHex,
                    iconName = iconName
                )
                repository.updateCategory(updated)
            }
            onDismissDialog()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
