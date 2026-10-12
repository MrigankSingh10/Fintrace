package com.fintrace.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.window.DialogProperties
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.data.model.CategoryBudgetConfiguration
import com.fintrace.app.data.model.CategoryBudgetMode
import com.fintrace.app.data.model.CategoryBudgetRules

@Composable
fun BulkEditBudgetsDialog(
    budgets: BulkBudgetEditState,
    onDismiss: () -> Unit,
    onSave: (Map<Long, CategoryBudgetConfiguration>) -> Unit,
    modifier: Modifier = Modifier
) {
    var configurations by remember(budgets.categories) {
        mutableStateOf(budgets.categories.associate { category ->
            category.id to CategoryBudgetRules.fromStored(category.budgetAmount, category.budgetPercentage)
        })
    }
    var budgetTexts by remember(budgets.categories) {
        mutableStateOf(configurations.mapValues { (_, configuration) -> configuration.value?.toString().orEmpty() })
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text("Edit All Budgets", style = MaterialTheme.typography.titleLarge)
                        Text("${budgets.categories.size} categories", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose a fixed amount, a percentage of monthly income, or no budget for each category.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(budgets.categories, key = CategoryEntity::id) { category ->
                        BudgetInput(
                            category = category,
                            mode = configurations[category.id]?.mode ?: CategoryBudgetMode.NONE,
                            value = budgetTexts[category.id].orEmpty(),
                            monthlyIncome = budgets.monthlyIncome,
                            onModeChange = { mode ->
                                if (mode != configurations[category.id]?.mode) {
                                    configurations = configurations + (category.id to CategoryBudgetConfiguration(mode))
                                    budgetTexts = budgetTexts + (category.id to "")
                                    errorText = null
                                }
                            },
                            onValueChange = { value ->
                                budgetTexts = budgetTexts + (category.id to value.filter { it.isDigit() || it == '.' })
                                configurations = configurations + (category.id to CategoryBudgetConfiguration(configurations[category.id]?.mode ?: CategoryBudgetMode.NONE))
                                errorText = null
                            },
                            onClear = {
                                configurations = configurations + (category.id to CategoryBudgetConfiguration(CategoryBudgetMode.NONE))
                                budgetTexts = budgetTexts + (category.id to "")
                                errorText = null
                            }
                        )
                    }
                }
                val totalPercentage = configurations.entries.sumOf { (id, configuration) ->
                    if (configuration.mode == CategoryBudgetMode.PERCENTAGE) budgetTexts[id].orEmpty().toDoubleOrNull() ?: 0.0 else 0.0
                }
                if (totalPercentage > 100.0) {
                    Text("Combined percentage budgets are ${String.format("%.1f", totalPercentage)}% of income.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val invalid = configurations.any { (id, configuration) ->
                                if (configuration.mode == CategoryBudgetMode.NONE) false else {
                                    val value = budgetTexts[id].orEmpty().toDoubleOrNull()
                                    value == null || !value.isFinite() || value < 0.0 ||
                                        (configuration.mode == CategoryBudgetMode.PERCENTAGE && value > 100.0)
                                }
                            }
                            if (invalid) errorText = "Enter a valid non-negative amount, or a percentage from 0 to 100, for each budget."
                            else onSave(configurations.mapValues { (id, configuration) ->
                                if (configuration.mode == CategoryBudgetMode.NONE) configuration
                                else configuration.copy(value = budgetTexts[id]?.toDoubleOrNull())
                            })
                        },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                    ) { Text("Save All") }
                }
            }
        }
    }
}

@Composable
private fun BudgetInput(
    category: CategoryEntity,
    mode: CategoryBudgetMode,
    value: String,
    monthlyIncome: Double,
    onModeChange: (CategoryBudgetMode) -> Unit,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, modifier = Modifier.weight(1f))
            if (category.budgetAmount != null || category.budgetPercentage != null) {
                Text(if (category.budgetPercentage != null) "Current ${category.budgetPercentage}%" else "Current ${formatCurrency(category.budgetAmount ?: 0.0)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(CategoryBudgetMode.NONE to "None", CategoryBudgetMode.FIXED to "Amount", CategoryBudgetMode.PERCENTAGE to "%").forEach { (option, label) ->
                FilterChip(selected = mode == option, onClick = { onModeChange(option) }, label = { Text(label) })
            }
        }
        if (mode != CategoryBudgetMode.NONE) OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (mode == CategoryBudgetMode.FIXED) "Monthly amount" else "Percent of income") },
            placeholder = { Text(if (mode == CategoryBudgetMode.FIXED) "e.g. 5000" else "e.g. 20") },
            prefix = if (mode == CategoryBudgetMode.FIXED) ({ Text("₹ ", fontWeight = FontWeight.Bold) }) else null,
            suffix = if (mode == CategoryBudgetMode.PERCENTAGE) ({ Text("%") }) else null,
            trailingIcon = {
                if (value.isNotEmpty()) IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear ${category.name} budget")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        if (mode == CategoryBudgetMode.PERCENTAGE) {
            val percentage = value.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 }
            val effective = percentage?.let { CategoryBudgetRules.effectiveAmount(CategoryBudgetConfiguration(mode, it), monthlyIncome) }
            Text(effective?.let { "${percentage}% = ${formatCurrency(it)} of ${formatCurrency(monthlyIncome.coerceAtLeast(0.0))}" }
                ?: "Enter a percentage from 0 to 100.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
