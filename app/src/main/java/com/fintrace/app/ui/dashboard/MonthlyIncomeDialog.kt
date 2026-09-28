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
import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.ui.components.formatSignedAdjustment
import com.fintrace.app.ui.theme.PrimaryEmerald

private fun fmt(value: Double): String = String.format("%.0f", value)

@Composable
fun MonthlyIncomeDialog(
    currentIncome: Double,
    monthName: String,
    confirmedIncome: Double,
    manualAdjustment: Double,
    monthlyBudget: MonthlyBudgetAdjustmentEntity?,
    onDismiss: () -> Unit,
    onSave: (Double, SalaryMode) -> Unit
) {
    // The last used mode is restored; it is never reset to OVERRIDE on reopen.
    var selectedMode by remember { mutableStateOf(monthlyBudget?.salaryMode ?: SalaryMode.OVERRIDE) }
    var amountText by remember {
        mutableStateOf(initialAmountText(currentIncome, selectedMode))
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Mode choices are offered whenever there is income to work with, no matter where that
    // income came from (SMS, manual row, or a mix).
    val hasExistingIncome = currentIncome > 0.0

    fun applyMode(mode: SalaryMode) {
        selectedMode = mode
        amountText = when (mode) {
            // Prefill the resolved total, never the raw stored adjustment.
            SalaryMode.OVERRIDE -> if (currentIncome > 0) fmt(currentIncome) else ""
            SalaryMode.ADD_TO_SMS -> ""
        }
    }

    val enteredAmount = amountText.toDoubleOrNull()
    val previewIncome = when {
        enteredAmount == null -> null
        selectedMode == SalaryMode.ADD_TO_SMS -> currentIncome + enteredAmount
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
                            text = "Adjust Monthly Income",
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
                    text = "Current monthly income: ₹ ${fmt(currentIncome)}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryEmerald
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("Confirmed income: ₹ ${fmt(confirmedIncome)}")
                        if (manualAdjustment != 0.0) {
                            append(" · Adjustment: ${formatSignedAdjustment(manualAdjustment)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasExistingIncome) {
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
                                text = "Override monthly income",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "This month shows exactly the amount you enter. Income confirmed later still adds on top.",
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
                                text = "Add to current income",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current income (₹ ${fmt(currentIncome)}) plus the amount you enter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No income recorded for this month yet. Enter the monthly income to use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                        amountText = filtered
                        if (errorText != null) errorText = null
                    },
                    label = {
                        Text(
                            if (selectedMode == SalaryMode.ADD_TO_SMS) "Amount to add"
                            else "Target monthly income"
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

                if (previewIncome != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedMode == SalaryMode.ADD_TO_SMS)
                            "New monthly income: ₹ ${fmt(currentIncome)} + ₹ ${fmt(enteredAmount!!)} = ₹ ${fmt(previewIncome)}"
                        else
                            "New monthly income: ₹ ${fmt(previewIncome)}",
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
                        listOf(50000.0 to "50k", 100000.0 to "100k", 150000.0 to "150k")
                    chipValues.forEach { (amount, label) ->
                        OutlinedButton(
                            onClick = { amountText = fmt(amount) },
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
                            val parsed = amountText.toDoubleOrNull()
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

private fun initialAmountText(
    currentIncome: Double,
    selectedMode: SalaryMode
): String = when (selectedMode) {
    // Never prefill the stored adjustment: that value is not a target total.
    SalaryMode.OVERRIDE -> if (currentIncome > 0) fmt(currentIncome) else ""
    SalaryMode.ADD_TO_SMS -> ""
}