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

@Composable
fun BulkEditBudgetsDialog(
    budgets: BulkBudgetEditState,
    onDismiss: () -> Unit,
    onSave: (Map<Long, Double?>) -> Unit,
    modifier: Modifier = Modifier
) {
    var budgetTexts by remember(budgets.categories) {
        mutableStateOf(budgets.categories.associate { it.id to (it.budgetAmount?.takeIf { amount -> amount > 0 }?.toString() ?: "") })
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
                    "Budgets apply to this category every month. Leave blank or enter 0 to remove a budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(budgets.categories, key = CategoryEntity::id) { category ->
                        BudgetInput(
                            category = category,
                            value = budgetTexts[category.id].orEmpty(),
                            onValueChange = { value ->
                                budgetTexts = budgetTexts + (category.id to value.filter { it.isDigit() || it == '.' })
                                errorText = null
                            },
                            onClear = { budgetTexts = budgetTexts + (category.id to ""); errorText = null }
                        )
                    }
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
                            val parsed = budgetTexts.mapValues { (_, text) ->
                                if (text.isBlank()) null else text.toDoubleOrNull()
                            }
                            val invalid = budgetTexts.values.any { text ->
                                text.isNotBlank() && (text.toDoubleOrNull()?.let { !it.isFinite() || it < 0.0 } ?: true)
                            }
                            if (invalid) errorText = "Enter a finite, non-negative amount for each budget."
                            else onSave(parsed.mapValues { (_, amount) -> amount?.takeIf { it > 0.0 } })
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
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, modifier = Modifier.weight(1f))
            if (category.budgetAmount != null && category.budgetAmount > 0) {
                Text("Current ${formatCurrency(category.budgetAmount)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Monthly budget") },
            placeholder = { Text("No budget") },
            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
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
    }
}
