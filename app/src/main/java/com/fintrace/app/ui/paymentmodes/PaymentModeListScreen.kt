package com.fintrace.app.ui.paymentmodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.ui.components.PaymentModeBadge
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.PrimaryEmerald

import androidx.compose.material.icons.filled.CreditCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentModeListScreen(
    viewModel: PaymentModeViewModel,
    onNavigateBack: () -> Unit
) {
    val paymentModes by viewModel.paymentModes.collectAsState()
    val cardMappings by viewModel.cardMappings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var modeToDelete by remember { mutableStateOf<PaymentModeEntity?>(null) }
    var protectedMode by remember { mutableStateOf<PaymentModeEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Payment Modes",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddModeClicked() },
                containerColor = PrimaryEmerald,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Payment Mode", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (paymentModes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No payment modes found.\nTap + to add cards or UPI accounts.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(paymentModes, key = { "pm-${it.id}" }) { mode ->
                        PaymentModeListItem(
                            mode = mode,
                            onEdit = { viewModel.onEditModeClicked(mode) },
                            onDelete = {
                                if (mode.id == 1L) {
                                    protectedMode = mode
                                } else {
                                    modeToDelete = mode
                                }
                            }
                        )
                    }

                    // Card Mappings Section
                    if (cardMappings.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Auto-detected Card Mappings",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming SMS with these card numbers automatically map to the assigned payment mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(cardMappings, key = { "cm-${it.id}" }) { mapping ->
                            val linkedMode = paymentModes.find { it.id == mapping.paymentModeId }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = PrimaryEmerald,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Card ending ••${mapping.cardLastFour}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Mapped to: ${linkedMode?.name ?: "Unknown"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteCardMapping(mapping) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete mapping",
                                            tint = ExpenseRed.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Edit/Add Dialog
            if (uiState.isEditing) {
                AddEditPaymentModeDialog(
                    mode = uiState.editingMode,
                    onDismiss = { viewModel.onDismissDialog() },
                    onSave = { name, type, iconName ->
                        viewModel.savePaymentMode(name, type, iconName)
                    }
                )
            }

            // Delete Confirmation
            modeToDelete?.let { mode ->
                AlertDialog(
                    onDismissRequest = { modeToDelete = null },
                    title = { Text("Delete Payment Mode") },
                    text = { Text("Are you sure you want to delete '${mode.name}'?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deletePaymentMode(mode)
                                modeToDelete = null
                            }
                        ) {
                            Text("Delete", color = ExpenseRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { modeToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Protected mode (seeded DEBIT) cannot be deleted — only edited
            protectedMode?.let { mode ->
                AlertDialog(
                    onDismissRequest = { protectedMode = null },
                    title = { Text("${mode.name} cannot be deleted") },
                    text = { Text("This is a required payment mode and can only be edited.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                protectedMode = null
                                viewModel.onEditModeClicked(mode)
                            }
                        ) {
                            Text("Edit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { protectedMode = null }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentModeListItem(
    mode: PaymentModeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            PaymentModeBadge(mode = mode)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = mode.type.label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = ExpenseRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
