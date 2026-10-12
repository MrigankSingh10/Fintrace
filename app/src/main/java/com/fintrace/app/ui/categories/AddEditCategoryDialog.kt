package com.fintrace.app.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.model.CategoryBudgetConfiguration
import com.fintrace.app.data.model.CategoryBudgetMode
import com.fintrace.app.ui.components.CategoryIconBadge
import com.fintrace.app.ui.components.ColorPickerRow
import com.fintrace.app.ui.components.IconPickerGrid
import com.fintrace.app.ui.theme.PrimaryEmerald

@Composable
fun AddEditCategoryDialog(
    category: CategoryEntity?,
    otherPercentageBudgets: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, budget: CategoryBudgetConfiguration) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#3B82F6") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "ShoppingCart") }
    var budgetMode by remember { mutableStateOf(when {
        category?.budgetPercentage != null -> CategoryBudgetMode.PERCENTAGE
        category?.budgetAmount != null -> CategoryBudgetMode.FIXED
        else -> CategoryBudgetMode.NONE
    }) }
    var budgetText by remember { mutableStateOf((category?.budgetPercentage ?: category?.budgetAmount)?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header with Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryIconBadge(
                        iconName = selectedIcon,
                        colorHex = selectedColor,
                        size = 48.dp,
                        iconSize = 26.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (category == null) "New Category" else "Edit Category",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (name.isBlank()) "Category Name" else name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Needs, Wants, Hospital...") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Global category budget configuration.
                Text("Budget (applies every month)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(CategoryBudgetMode.NONE to "No budget", CategoryBudgetMode.FIXED to "Amount", CategoryBudgetMode.PERCENTAGE to "Percentage").forEach { (mode, label) ->
                        FilterChip(selected = budgetMode == mode, onClick = {
                            if (budgetMode != mode) {
                                budgetMode = mode
                                budgetText = ""
                                errorText = null
                            }
                        }, label = { Text(label) })
                    }
                }
                if (budgetMode != CategoryBudgetMode.NONE) OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (errorText != null) errorText = null
                    },
                    label = { Text(if (budgetMode == CategoryBudgetMode.FIXED) "Monthly amount" else "Percent of monthly income") },
                    placeholder = { Text(if (budgetMode == CategoryBudgetMode.FIXED) "e.g. 1000" else "e.g. 20") },
                    supportingText = { Text(if (budgetMode == CategoryBudgetMode.FIXED) "Enter 0 for a zero amount budget." else "Enter a value from 0 to 100%.") },
                    prefix = if (budgetMode == CategoryBudgetMode.FIXED) ({ Text("₹ ") }) else null,
                    suffix = if (budgetMode == CategoryBudgetMode.PERCENTAGE) ({ Text("%") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (budgetMode == CategoryBudgetMode.PERCENTAGE) {
                    val percentage = budgetText.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: 0.0
                    if (otherPercentageBudgets + percentage > 100.0) {
                        Text("Combined category budgets exceed 100% of income.", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ColorPickerRow(
                    selectedColorHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Picker
                IconPickerGrid(
                    selectedIconName = selectedIcon,
                    onIconSelected = { selectedIcon = it },
                    activeColorHex = selectedColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.trim().isBlank()) {
                                errorText = "Please enter a name"
                            } else {
                                val budget = budgetText.trim()
                                if (budgetMode == CategoryBudgetMode.NONE) onSave(name.trim(), selectedColor, selectedIcon, CategoryBudgetConfiguration(CategoryBudgetMode.NONE))
                                else {
                                    val value = budget.toDoubleOrNull()
                                    if (value == null || !value.isFinite() || value < 0.0 || (budgetMode == CategoryBudgetMode.PERCENTAGE && value > 100.0)) {
                                        errorText = if (budgetMode == CategoryBudgetMode.PERCENTAGE) "Enter a percentage from 0 to 100" else "Budget must be finite and non-negative"
                                    } else onSave(name.trim(), selectedColor, selectedIcon, CategoryBudgetConfiguration(budgetMode, value))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
