package com.fintrace.app.ui.sms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.ui.components.PaymentModeBadge
import com.fintrace.app.ui.paymentmodes.AddEditPaymentModeDialog
import com.fintrace.app.ui.theme.PrimaryEmerald

@Composable
fun CardMappingDialog(
    cardLastFour: String,
    paymentModes: List<PaymentModeEntity>,
    currentPaymentModeId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (paymentModeId: Long) -> Unit,
    onAddPaymentMode: (name: String, type: PaymentModeType, iconName: String, onCreated: (Long) -> Unit) -> Unit
) {
    val sortedModes = remember(paymentModes) {
        paymentModes.filter { it.type == PaymentModeType.CREDIT_CARD }.sortedBy { it.name }
    }

    var selectedModeId by remember(cardLastFour, currentPaymentModeId) {
        mutableStateOf(currentPaymentModeId?.takeIf { id -> sortedModes.any { it.id == id } } ?: sortedModes.firstOrNull()?.id)
    }
    var showAddPaymentMode by remember(cardLastFour) { mutableStateOf(false) }

    LaunchedEffect(sortedModes) {
        if (selectedModeId == null) selectedModeId = sortedModes.firstOrNull()?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = PrimaryEmerald
            )
        },
        title = {
            Text(
                text = "Map Card ••$cardLastFour",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select which payment mode should be automatically assigned whenever transactions from card ending $cardLastFour arrive:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (sortedModes.isEmpty()) {
                    Text(
                        text = "No credit card payment methods yet. Add one to map this card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(sortedModes, key = { it.id }) { mode ->
                            val isSelected = mode.id == selectedModeId

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        PrimaryEmerald.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedModeId = mode.id }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedModeId = mode.id }
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    PaymentModeBadge(mode = mode)

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = mode.type.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showAddPaymentMode = true }) {
                    Text("Add payment method")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedModeId != null,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    selectedModeId?.let(onConfirm)
                }
            ) {
                Text("Save Mapping", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAddPaymentMode) {
        AddEditPaymentModeDialog(
            mode = null,
            allowedTypes = setOf(PaymentModeType.CREDIT_CARD),
            onDismiss = { showAddPaymentMode = false },
            onSave = { name, type, iconName ->
                onAddPaymentMode(name, type, iconName) { createdId ->
                    selectedModeId = createdId
                    showAddPaymentMode = false
                }
            }
        )
    }
}
