package com.fintrace.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.*
import com.fintrace.app.ui.sms.*
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
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
    smsInboxViewModel: SmsInboxViewModel? = null,
    onNavigateToSmsReview: () -> Unit = {}
) {
    val segment by viewModel.segment.collectAsState()
    val allTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedSource by viewModel.sourceTransactions.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentModes by viewModel.paymentModes.collectAsState()
    val pending = smsInboxViewModel?.pendingTransactions?.collectAsState(initial = emptyList())?.value.orEmpty()
    val pendingSource = smsInboxViewModel?.pendingSourceTransactions?.collectAsState(initial = emptyList())?.value.orEmpty()
    val dismissed = smsInboxViewModel?.dismissedTransactions?.collectAsState(initial = emptyList())?.value.orEmpty()
    val smsState = smsInboxViewModel?.uiState?.collectAsState(initial = SmsInboxUiState())?.value ?: SmsInboxUiState()
    val undoables = smsInboxViewModel?.undoableActions?.collectAsState(initial = emptyList())?.value.orEmpty()
    val cardMappings = smsInboxViewModel?.cardMappings?.collectAsState(initial = emptyList())?.value.orEmpty()
    val displayRows = when (segment) {
        TransactionSegment.ALL -> allTransactions
        TransactionSegment.PENDING -> if (smsState.pendingLoaded) pending.filter { matchesTransactionFilter(it, filter) }.sortedForPresentation() else emptyList()
        TransactionSegment.DISMISSED -> dismissed.filter { matchesTransactionFilter(it, filter) }.sortedForPresentation()
    }
    LaunchedEffect(sharedMonth) { sharedMonth?.let(viewModel::onMonthSelected) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedDetailItem by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var pendingCardToMap by remember { mutableStateOf<Pair<String, TransactionWithDetails>?>(null) }
    var deleteTarget by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var confirmAllSnapshot by remember { mutableStateOf<List<TransactionWithDetails>?>(null) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val activeFilterCount = filter.categoryIds.size + filter.paymentModeIds.size + (if (filter.type != null) 1 else 0) + (if (filter.hasSplit != null) 1 else 0)
    val hasActiveFilters = activeFilterCount > 0 || filter.searchQuery.isNotBlank()
    val sourceCount = when (segment) {
        TransactionSegment.ALL -> selectedSource.size
        TransactionSegment.PENDING -> pendingSource.size
        TransactionSegment.DISMISSED -> dismissed.size
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, floatingActionButton = {
        if (showAddFab) FloatingActionButton(onClick = onNavigateToAddTransaction) { Text("Add") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TransactionSegment.entries, key = { it.name }) { item ->
                    val count = when (item) { TransactionSegment.ALL -> null; TransactionSegment.PENDING -> pending.size; TransactionSegment.DISMISSED -> dismissed.size }
                    FilterChip(selected = segment == item, onClick = { viewModel.selectSegment(item) }, label = {
                        Text(when (item) { TransactionSegment.ALL -> "All"; TransactionSegment.PENDING -> "Pending${count?.let { " $it" } ?: ""}"; TransactionSegment.DISMISSED -> "Dismissed${count?.let { " $it" } ?: ""}" })
                    })
                }
            }
            if (segment == TransactionSegment.ALL) {
                MonthSwitcher(month = sharedMonth ?: selectedMonth, onMonthSelected = onSharedMonthSelected ?: viewModel::onMonthSelected, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            } else Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("All months", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (segment == TransactionSegment.PENDING && pending.isNotEmpty()) TextButton(onClick = { confirmAllSnapshot = pending.toList() }) { Text("Confirm all") }
            }
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
            if (segment == TransactionSegment.PENDING || segment == TransactionSegment.DISMISSED) {
                smsState.actionError?.let { error -> Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                undoables.forEach { action ->
                    Snackbar(Modifier.padding(horizontal = 16.dp, vertical = 2.dp), action = { TextButton(onClick = { smsInboxViewModel?.undoPendingAction(action.transactionId) }) { Text("Undo") } }) {
                        Text("${if (action.action == PendingAction.CONFIRM) "Confirmed" else "Dismissed"} ${action.description}")
                    }
                }
                if (!smsState.pendingLoaded && segment == TransactionSegment.PENDING) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (displayRows.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = when {
                        segment == TransactionSegment.PENDING && !smsState.pendingLoaded -> "Loading pending review…"
                        hasActiveFilters && sourceCount > 0 -> "No matches"
                        segment == TransactionSegment.PENDING && pending.isEmpty() && sourceCount > 0 -> "Saving review action…"
                        segment == TransactionSegment.ALL -> "No transactions for ${selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}"
                        segment == TransactionSegment.PENDING -> "All caught up"
                        else -> "No dismissed transactions"
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (hasActiveFilters && sourceCount > 0) TextButton(onClick = viewModel::clearFilters) { Text("Clear filters") }
                    if (segment == TransactionSegment.PENDING && smsState.pendingLoaded && sourceCount == 0) {
                        Text("New bank SMS alerts will appear here for review.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = onNavigateToSmsReview) { Text("Scan SMS") }
                    }
                }
            } else when (segment) {
                TransactionSegment.ALL -> {
                    val groups = remember(displayRows) { groupTransactionsByLocalDay(displayRows) }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { group ->
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
                                TransactionRow(title = item.transaction.description, myShareAmount = item.transaction.myShareAmount, originalAmount = item.transaction.originalAmount, categoryName = item.category?.name, categoryIcon = item.category?.iconName, categoryColorHex = item.category?.colorHex, detail = item.paymentMode?.name, type = item.transaction.type, isRecurring = item.transaction.isRecurring, currencyCode = item.transaction.currency, onClick = { selectedDetailItem = item })
                            }
                        }
                    }
                }
                TransactionSegment.PENDING -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayRows, key = { it.transaction.id }) { item ->
                        PendingSmsCard(item, categories, cardMappings, dateFormatter,
                            onConfirm = { smsInboxViewModel?.onConfirmTransaction(item) }, onSplitAndEdit = { onNavigateToEditTransaction(item.transaction.id) },
                            onDismiss = { smsInboxViewModel?.onDismissTransaction(item) }, onSelectCategory = { id -> smsInboxViewModel?.onQuickCategoryChange(item, id) },
                            onMapCard = { lastFour -> pendingCardToMap = lastFour to item },
                            onSelectPaymentMode = { id -> smsInboxViewModel?.onQuickPaymentModeChange(item, id) }, paymentModes = paymentModes)
                    }
                }
                TransactionSegment.DISMISSED -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayRows, key = { it.transaction.id }) { item ->
                        TransactionRow(title = item.transaction.description, myShareAmount = item.transaction.myShareAmount, originalAmount = item.transaction.originalAmount, categoryName = item.category?.name, categoryIcon = item.category?.iconName, categoryColorHex = item.category?.colorHex, type = item.transaction.type, status = com.fintrace.app.data.model.TransactionStatus.DISMISSED, isRecurring = item.transaction.isRecurring, currencyCode = item.transaction.currency)
                        TextButton(onClick = { smsInboxViewModel?.onRestoreTransaction(item) }) { Text("Restore for review") }
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
    pendingCardToMap?.let { (lastFour, item) -> CardMappingDialog(lastFour, paymentModes, item.transaction.paymentModeId, { pendingCardToMap = null }, { modeId -> smsInboxViewModel?.onCreateCardMapping(lastFour, modeId, item); pendingCardToMap = null }) }
    confirmAllSnapshot?.let { snapshot ->
        val totals = snapshot.groupBy { it.transaction.currency.uppercase() }.mapValues { (_, rows) -> rows.sumOf { it.transaction.myShareAmount } }
        AlertDialog(onDismissRequest = { confirmAllSnapshot = null }, title = { Text("Confirm all pending?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("This confirms ${snapshot.size} transactions across all months. Active filters do not limit this action.")
                totals.forEach { (currency, amount) -> Text("${formatCurrency(amount, currency)} personal share") }
                Text("This bulk action cannot be undone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } },
            confirmButton = { TextButton(onClick = { smsInboxViewModel?.onConfirmAllPending(snapshot.map { it.transaction.id }.toSet()); confirmAllSnapshot = null }) { Text("Confirm all") } },
            dismissButton = { TextButton(onClick = { confirmAllSnapshot = null }) { Text("Cancel") } })
    }
}
