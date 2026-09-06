package com.fintrace.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.CategoryIconBadge
import com.fintrace.app.ui.components.DualAmountDisplay
import com.fintrace.app.ui.components.PaymentModeBadge
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.parseColorHex
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.PrimaryEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeFormatter
import com.fintrace.app.ui.dashboard.MonthSelectorBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val categories by viewModel.categories.collectAsState()
    val paymentModes by viewModel.paymentModes.collectAsState()

    var selectedDetailItem by remember { mutableStateOf<TransactionWithDetails?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
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
                    Text("Add Expense", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MonthSelectorBar(
                displayName = monthLabel,
                onPrevious = viewModel::onPreviousMonth,
                onNext = viewModel::onNextMonth,
                onReset = viewModel::onResetToCurrentMonth,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Search Bar & Filter Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search merchant, note, category...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (filter.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = filter.categoryId == null,
                            onClick = { viewModel.onCategoryFilterChanged(null) },
                            label = { Text("All Categories", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        val isSelected = filter.categoryId == cat.id
                        val color = parseColorHex(cat.colorHex)

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategoryFilterChanged(cat.id) },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }
            }

            // Summary Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${summary.transactionCount} Transactions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: ${formatCurrency(summary.totalMyShareSpent)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = ExpenseRed
                )
            }

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filter.searchQuery.isNotBlank() || filter.categoryId != null)
                                "No transactions match your filter in $monthLabel."
                            else
                                "No transactions recorded in $monthLabel.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Expense' to record a spend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(transactions, key = { it.transaction.id }) { item ->
                        TransactionRowItem(
                            item = item,
                            dateFormatter = dateFormatter,
                            onClick = { selectedDetailItem = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
                    }
                }
            }
        }

        // Transaction Detail Sheet
        selectedDetailItem?.let { item ->
            TransactionDetailSheet(
                item = item,
                onDismiss = { selectedDetailItem = null },
                onEdit = {
                    val id = item.transaction.id
                    selectedDetailItem = null
                    onNavigateToEditTransaction(id)
                },
                onDelete = {
                    viewModel.deleteTransaction(item)
                    selectedDetailItem = null
                }
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    item: TransactionWithDetails,
    dateFormatter: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = item.transaction
    val isIncome = t.type == TransactionType.INCOME

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (!isIncome) {
                CategoryIconBadge(
                    iconName = item.category?.iconName,
                    colorHex = item.category?.colorHex,
                    size = 44.dp,
                    iconSize = 22.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PrimaryEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = PrimaryEmerald, style = MaterialTheme.typography.titleLarge) }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Merchant / Description & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isIncome) "Income received" else t.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormatter.format(Date(t.timestamp)),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isIncome) {
                        Spacer(modifier = Modifier.width(6.dp))
                        PaymentModeBadge(mode = item.paymentMode)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Dual Amount Display (Prominent My Share + Total Original)
            DualAmountDisplay(
                originalAmount = t.originalAmount,
                myShareAmount = t.myShareAmount,
                isExpense = !isIncome
            )
        }
    }
}
