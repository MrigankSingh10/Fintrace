package com.fintrace.app.ui.dashboard

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.data.model.CategoryBudgetConfiguration
import com.fintrace.app.data.model.CategoryBudgetMode
import com.fintrace.app.data.model.CategoryBudgetRules

@Composable
fun CategoryBudgetDialog(
    categoryName: String,
    currentBudget: CategoryBudgetConfiguration,
    monthlyIncome: Double,
    otherPercentageBudgets: Double = 0.0,
    onSave: (budget: CategoryBudgetConfiguration) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember(currentBudget) { mutableStateOf(currentBudget.mode) }
    var budgetText by remember(currentBudget) { mutableStateOf(currentBudget.value?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Set Budget — $categoryName",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                        text = when (currentBudget.mode) {
                            CategoryBudgetMode.FIXED -> "Current monthly budget: ${formatCurrency(currentBudget.value ?: 0.0)}"
                            CategoryBudgetMode.PERCENTAGE -> "Current budget: ${currentBudget.value}% of income"
                            CategoryBudgetMode.NONE -> "No monthly budget set"
                        },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose a fixed amount or a percentage of monthly income. The choice applies to this category every month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CategoryBudgetMode.NONE to "No budget", CategoryBudgetMode.FIXED to "Amount", CategoryBudgetMode.PERCENTAGE to "Percentage").forEach { (option, label) ->
                        FilterChip(selected = mode == option, onClick = {
                            if (mode != option) {
                                mode = option
                                budgetText = ""
                                errorText = null
                            }
                        }, label = { Text(label) })
                    }
                }

                if (mode != CategoryBudgetMode.NONE) OutlinedTextField(
                    value = budgetText,
                    onValueChange = {
                        budgetText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        if (errorText != null) errorText = null
                    },
                    label = { Text(if (mode == CategoryBudgetMode.FIXED) "Budget Amount" else "Percent of monthly income") },
                    placeholder = { Text(if (mode == CategoryBudgetMode.FIXED) "e.g. 5000" else "e.g. 20") },
                    prefix = if (mode == CategoryBudgetMode.FIXED) ({ Text("₹ ", fontWeight = FontWeight.Bold) }) else null,
                    suffix = if (mode == CategoryBudgetMode.PERCENTAGE) ({ Text("%") }) else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode == CategoryBudgetMode.PERCENTAGE) {
                    val percentage = budgetText.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 }
                    val effective = percentage?.let { CategoryBudgetRules.effectiveAmount(CategoryBudgetConfiguration(mode, it), monthlyIncome) }
                    Text(
                        text = effective?.let { "${percentage}% of ${formatCurrency(monthlyIncome.coerceAtLeast(0.0))} = ${formatCurrency(it)}" }
                            ?: "Enter a percentage from 0 to 100.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (otherPercentageBudgets + (percentage ?: 0.0) > 100.0) {
                        Text(
                            "Combined category budgets would exceed 100% of income.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val budget = budgetText.trim()
                            if (mode == CategoryBudgetMode.NONE) onSave(CategoryBudgetConfiguration(CategoryBudgetMode.NONE))
                            else {
                                val value = budget.toDoubleOrNull()
                                val maximum = if (mode == CategoryBudgetMode.PERCENTAGE) 100.0 else Double.MAX_VALUE
                                if (value == null || !value.isFinite() || value < 0.0 || value > maximum) {
                                    errorText = if (mode == CategoryBudgetMode.PERCENTAGE) "Enter a percentage from 0 to 100" else "Enter a finite, non-negative budget"
                                } else onSave(CategoryBudgetConfiguration(mode, value))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
