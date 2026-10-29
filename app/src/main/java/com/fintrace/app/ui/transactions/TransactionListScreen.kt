package com.fintrace.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    sharedMonth: YearMonth? = null,
    onSharedMonthSelected: ((YearMonth) -> Unit)? = null,
    showAddFab: Boolean = true,
    onNavigateToSmsReview: () -> Unit = {}
) {
    val allTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedSource by viewModel.sourceTransactions.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentModes by viewModel.paymentModes.collectAsState()
    val displayRows = allTransactions
    LaunchedEffect(sharedMonth) { sharedMonth?.let(viewModel::onMonthSelected) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var deleteTarget by remember { mutableStateOf<TransactionWithDetails?>(null) }
    val activeFilterCount = filter.categoryIds.size + filter.paymentModeIds.size + (if (filter.type != null) 1 else 0) + (if (filter.hasSplit != null) 1 else 0)
    val hasActiveFilters = activeFilterCount > 0 || filter.searchQuery.isNotBlank()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, floatingActionButton = {
        if (showAddFab) FloatingActionButton(onClick = onNavigateToAddTransaction) { Text("Add") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (pendingCount > 0) {
                PendingReviewBanner(pendingCount, onClick = onNavigateToSmsReview)
            }
            MonthSwitcher(month = sharedMonth ?: selectedMonth, onMonthSelected = onSharedMonthSelected ?: viewModel::onMonthSelected, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = filter.searchQuery, onValueChange = viewModel::onSearchQueryChanged, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Search transactions") }, leadingIcon = { Icon(Icons.Default.Search, null) })
                BadgedBox(badge = { if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) } }) {
                    IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Default.FilterList, contentDescription = "Filters") }
                }
            }
            if (filter.categoryIds.isNotEmpty() || filter.paymentModeIds.isNotEmpty() || filter.type != null || filter.hasSplit != null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filter.categoryIds.forEach { id -> categories.firstOrNull { it.id == id }?.let { FilterChip(true, { viewModel.removeCategory(id) }, label = { Text(it.name) }) } }
                    filter.paymentModeIds.forEach { id -> paymentModes.firstOrNull { it.id == id }?.let { FilterChip(true, { viewModel.removePaymentMode(id) }, label = { Text(it.name) }) } }
                    filter.type?.let { FilterChip(true, { viewModel.setType(null) }, label = { Text(it.label) }) }
                    filter.hasSplit?.let { FilterChip(true, { viewModel.setHasSplit(null) }, label = { Text(if (it) "Split" else "No split") }) }
                }
            }
            if (displayRows.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 100.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = if (hasActiveFilters && selectedSource.isNotEmpty()) "No matches" else "No transactions for ${selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}"
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (hasActiveFilters && selectedSource.isNotEmpty()) TextButton(onClick = viewModel::clearFilters) { Text("Clear filters") }
                }
            } else {
                    val presentation = remember(displayRows) { buildTransactionListPresentation(displayRows) }
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (presentation.recurringTransactions.isNotEmpty()) {
                            stickyHeader(key = "recurring-header") {
                                Surface(color = MaterialTheme.colorScheme.background) {
                                    Text("Recurring", modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                            items(presentation.recurringTransactions, key = { it.transaction.id }) { item ->
                                TransactionRow(title = item.transaction.description, myShareAmount = item.transaction.myShareAmount, originalAmount = item.transaction.originalAmount, categoryName = item.category?.name, categoryIcon = item.category?.iconName, categoryColorHex = item.category?.colorHex, detail = item.paymentMode?.name.takeUnless { item.transaction.type == TransactionType.INCOME }, type = item.transaction.type, isRecurring = item.transaction.isRecurring, currencyCode = item.transaction.currency, onClick = { selectedDetailItem = item })
                            }
                        }
                        presentation.dayGroups.forEach { group ->
                            stickyHeader(key = "day-${group.date}") {
                                Surface(color = MaterialTheme.colorScheme.background) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(group.date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                        if (group.hasMixedCurrencies) Text("Mixed currencies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        else group.netByCurrency.entries.firstOrNull()?.let { (currency, amount) -> MoneyText(amount, currencyCode = currency, style = MaterialTheme.typography.labelMedium) }
                                    }
                                }
                            }
                            items(group.transactions, key = { it.transaction.id }) { item ->
                                TransactionRow(title = item.transaction.description, myShareAmount = item.transaction.myShareAmount, originalAmount = item.transaction.originalAmount, categoryName = item.category?.name, categoryIcon = item.category?.iconName, categoryColorHex = item.category?.colorHex, detail = item.paymentMode?.name.takeUnless { item.transaction.type == TransactionType.INCOME }, type = item.transaction.type, isRecurring = item.transaction.isRecurring, currencyCode = item.transaction.currency, onClick = { selectedDetailItem = item })
                            }
                        }
                    }
            }
        }
    }

    if (showFilterSheet) ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(20.dp)) {
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filter transactions", style = MaterialTheme.typography.titleLarge)
            Text("Categories", style = MaterialTheme.typography.titleSmall)
            categories.forEach { category -> FilterChip(category.id in filter.categoryIds, { viewModel.toggleCategory(category.id) }, label = { Text(category.name) }) }
            Text("Payment modes", style = MaterialTheme.typography.titleSmall)
            paymentModes.forEach { mode -> FilterChip(mode.id in filter.paymentModeIds, { viewModel.togglePaymentMode(mode.id) }, label = { Text(mode.name) }) }
            Text("Type", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER).forEach { FilterChip(filter.type == it, { viewModel.setType(if (filter.type == it) null else it) }, label = { Text(it.label) }) } }
            Text("Split bills", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(filter.hasSplit == true, { viewModel.setHasSplit(if (filter.hasSplit == true) null else true) }, label = { Text("Has split") }); FilterChip(filter.hasSplit == false, { viewModel.setHasSplit(if (filter.hasSplit == false) null else false) }, label = { Text("No split") }) }
          }
          Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = viewModel::clearFilters) { Text("Clear all") }; Button(onClick = { showFilterSheet = false }) { Text("Apply") } }
        }
    }
    selectedDetailItem?.let { item -> TransactionDetailSheet(item, onDismiss = { selectedDetailItem = null }, onEdit = { val id = item.transaction.id; selectedDetailItem = null; onNavigateToEditTransaction(id) }, onDelete = { deleteTarget = item }) }
    deleteTarget?.let { item -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete transaction?") }, text = { Text("${item.transaction.description} will be permanently deleted, including its split details. This cannot be undone.") }, confirmButton = { TextButton(onClick = { viewModel.deleteTransaction(item); deleteTarget = null; selectedDetailItem = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }) }
}

@Composable
internal fun PendingReviewBanner(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$count transaction${if (count == 1) "" else "s"} ${if (count == 1) "needs" else "need"} review",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Check imported bank SMS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open SMS review", tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
