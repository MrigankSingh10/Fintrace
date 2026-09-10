package com.fintrace.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.ui.theme.PrimaryEmerald

private fun fmt(value: Double): String = String.format("%.0f", value)

@Composable
fun MonthlySalaryDialog(
    currentSalary: Double,
    monthName: String,
    isIncomeDerived: Boolean,
    confirmedIncome: Double,
    monthlyBudget: MonthlyBudgetSalaryEntity?,
    onDismiss: () -> Unit,
    onSave: (Double, SalaryMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(monthlyBudget?.salaryMode ?: SalaryMode.OVERRIDE) }
    var salaryText by remember {
        mutableStateOf(initialSalaryText(monthlyBudget, currentSalary, selectedMode))
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun applyMode(mode: SalaryMode) {
        selectedMode = mode
        salaryText = when (mode) {
            SalaryMode.OVERRIDE ->
                monthlyBudget?.takeIf { it.salaryAmount > 0 }?.let { fmt(it.salaryAmount) }
                    ?: if (currentSalary > 0) fmt(currentSalary) else ""
            SalaryMode.ADD_TO_SMS -> ""
        }
    }

    val enteredAmount = salaryText.toDoubleOrNull()
    val previewSalary = when {
        enteredAmount == null -> null
        selectedMode == SalaryMode.ADD_TO_SMS -> currentSalary + enteredAmount
        else -> enteredAmount
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = PrimaryEmerald,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "Set Monthly Salary",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = monthName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Current salary: ₹ ${fmt(currentSalary)}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryEmerald
                )

                if (isIncomeDerived) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SMS income detected: ₹ ${fmt(confirmedIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose how your manual amount should behave.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyMode(SalaryMode.OVERRIDE) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedMode == SalaryMode.OVERRIDE,
                            onClick = { applyMode(SalaryMode.OVERRIDE) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Override salary",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Monthly salary becomes exactly the amount you enter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyMode(SalaryMode.ADD_TO_SMS) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedMode == SalaryMode.ADD_TO_SMS,
                            onClick = { applyMode(SalaryMode.ADD_TO_SMS) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Add to current salary",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current salary (₹ ${fmt(currentSalary)}) plus the amount you enter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter the salary credited for this month. Confirmed income will replace this manual amount.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = salaryText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                        salaryText = filtered
                        if (errorText != null) errorText = null
                    },
                    label = {
                        Text(
                            if (selectedMode == SalaryMode.ADD_TO_SMS) "Add to current salary"
                            else "Salary (Override)"
                        )
                    },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = PrimaryEmerald) },
                    placeholder = { Text("e.g. 100000") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (previewSalary != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedMode == SalaryMode.ADD_TO_SMS)
                            "New salary: ₹ ${fmt(currentSalary)} + ₹ ${fmt(enteredAmount!!)} = ₹ ${fmt(previewSalary)}"
                        else
                            "New salary: ₹ ${fmt(previewSalary)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryEmerald
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val chipValues = if (selectedMode == SalaryMode.ADD_TO_SMS)
                        listOf(5000.0 to "+5k", 10000.0 to "+10k", 25000.0 to "+25k")
                    else
                        listOf(50000.0 to "+50k", 100000.0 to "100k", 150000.0 to "150k")
                    chipValues.forEach { (amount, label) ->
                        OutlinedButton(
                            onClick = { salaryText = fmt(amount) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            val parsed = salaryText.toDoubleOrNull()
                            if (parsed == null || parsed < 0.0) {
                                errorText = "Please enter a valid amount"
                            } else {
                                onSave(parsed, selectedMode)
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

private fun initialSalaryText(
    monthlyBudget: MonthlyBudgetSalaryEntity?,
    currentSalary: Double,
    selectedMode: SalaryMode
): String = when (selectedMode) {
    SalaryMode.OVERRIDE ->
        monthlyBudget?.takeIf { it.salaryAmount > 0 }?.let { fmt(it.salaryAmount) }
            ?: if (currentSalary > 0) fmt(currentSalary) else ""
    SalaryMode.ADD_TO_SMS -> ""
}