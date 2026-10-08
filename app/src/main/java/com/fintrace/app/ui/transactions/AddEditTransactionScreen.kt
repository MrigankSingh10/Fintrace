package com.fintrace.app.ui.transactions

import android.app.DatePickerDialog
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.AmountInput
import com.fintrace.app.ui.components.IconMapper
import com.fintrace.app.ui.components.currencySymbol
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.getPaymentModeIcon
import com.fintrace.app.ui.components.parseColorHex
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.FinanceTrackerTheme
import com.fintrace.app.ui.theme.IncomeGreen
import com.fintrace.app.ui.theme.PrimaryEmerald
import com.fintrace.app.ui.theme.SplitBadgeBg
import com.fintrace.app.ui.theme.SplitBadgeText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long,
    viewModel: AddEditTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val paymentModes by viewModel.paymentModes.collectAsState()
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    var deleteDialog by rememberSaveable { mutableStateOf(false) }
    var splitExpanded by rememberSaveable { mutableStateOf(false) }
    var focusRequested by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val requestDismiss = {
        when {
            state.isBusy -> Unit
            state.isDirty -> discardDialog = true
            else -> onNavigateBack()
        }
    }

    LaunchedEffect(transactionId) { viewModel.loadTransaction(transactionId) }
    LaunchedEffect(state.isInitialized) {
        if (state.isInitialized && !splitExpanded) splitExpanded = state.isSplit
    }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            viewModel.resetSaved()
            onNavigateBack()
        }
    }
    LaunchedEffect(state.isInitialized, transactionId) {
        if (transactionId <= 0 && state.isInitialized && !focusRequested) {
            focusRequested = true
            delay(350)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    BackHandler(enabled = true) { requestDismiss() }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden) {
                if (state.isBusy) false
                else if (state.isDirty) { discardDialog = true; false }
                else true
            } else true
        }
    )

    ModalBottomSheet(
        onDismissRequest = { requestDismiss() },
        sheetState = sheetState,
        windowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (transactionId > 0) "Edit transaction" else "Add transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(if (transactionId > 0) "Update the details below" else "Add a transaction to your records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (transactionId > 0) {
                    IconButton(onClick = { deleteDialog = true }, enabled = !state.isBusy) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction", tint = ExpenseRed)
                    }
                }
                IconButton(onClick = requestDismiss, enabled = !state.isBusy) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Loading transaction…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (!state.isInitialized || state.errorMessage == "Transaction not found" || state.errorMessage?.startsWith("Could not load") == true) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.errorMessage ?: "Preparing transaction…", color = MaterialTheme.colorScheme.error)
                    if (state.errorMessage != null) TextButton(onClick = { viewModel.loadTransaction(transactionId) }) { Text("Try again") }
                }
            } else {
                AddEditTransactionForm(
                    state = state,
                    categories = categories,
                    paymentModes = paymentModes,
                    modifier = Modifier.weight(1f),
                    amountFocusRequester = focusRequester,
                    enabled = state.canEdit,
                    splitExpanded = splitExpanded,
                    onSplitExpandedChange = {
                        splitExpanded = it
                        if (it && !state.isSplit) viewModel.onSplitToggle(true)
                    },
                    onTypeSelect = viewModel::onTypeSelect,
                    onAmountChange = viewModel::onOriginalAmountChange,
                    onCategorySelect = viewModel::onCategorySelect,
                    onPaymentModeSelect = viewModel::onPaymentModeSelect,
                    onTimestampChange = viewModel::onTimestampChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onNotesChange = viewModel::onNotesChange,
                    onRecurringChange = viewModel::onRecurringToggle,
                    onMyShareChange = viewModel::onMyShareAmountChange,
                    onQuickSplit = viewModel::onQuickSplit,
                    onAddParticipant = viewModel::onAddParticipant,
                    onUpdateParticipant = viewModel::onUpdateParticipant,
                    onRemoveParticipant = viewModel::onRemoveParticipant
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isSaving || state.isDeleting) {
                        Text(if (state.isDeleting) "Deleting…" else "Saving…", Modifier.weight(1f).padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Button(
                            onClick = viewModel::saveTransaction,
                            enabled = state.canEdit && state.canSave,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) { Text(if (transactionId > 0) "Save changes" else "Save transaction", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (discardDialog) {
        AlertDialog(
            onDismissRequest = { discardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") },
            confirmButton = { TextButton(onClick = { discardDialog = false; onNavigateBack() }) { Text("Discard", color = ExpenseRed) } },
            dismissButton = { TextButton(onClick = { discardDialog = false }) { Text("Keep editing") } }
        )
    }
    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This transaction will be permanently removed.") },
            confirmButton = { TextButton(onClick = { deleteDialog = false; viewModel.deleteTransaction() }) { Text("Delete", color = ExpenseRed) } },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditTransactionForm(
    state: AddEditTransactionUiState,
    categories: List<CategoryEntity>,
    paymentModes: List<PaymentModeEntity>,
    modifier: Modifier = Modifier,
    amountFocusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    splitExpanded: Boolean,
    onSplitExpandedChange: (Boolean) -> Unit,
    onTypeSelect: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onPaymentModeSelect: (Long) -> Unit,
    onTimestampChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onMyShareChange: (String) -> Unit,
    onQuickSplit: (Int) -> Unit,
    onAddParticipant: () -> Unit,
    onUpdateParticipant: (Int, String, String) -> Unit,
    onRemoveParticipant: (Int) -> Unit
) {
    val context = LocalContext.current
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.errorMessage?.let { ErrorNotice(it) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                val selected = state.type == type
                FilterChip(
                    selected = selected,
                    onClick = { onTypeSelect(type) },
                    enabled = enabled,
                    label = { Text(type.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = (if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen).copy(alpha = .13f),
                        selectedLabelColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (state.isSplit) "Full bill amount" else "Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountInput(
                value = state.originalAmount,
                onValueChange = onAmountChange,
                modifier = Modifier.then(if (amountFocusRequester != null) Modifier.focusRequester(amountFocusRequester) else Modifier),
                label = if (state.isSplit) "Full bill" else "Amount",
                currencyPrefix = "${currencySymbol(state.currency)} ",
                error = state.amountError,
                enabled = enabled
            )
            if (state.isSplit) Text("This is the total bill before applying your share.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.type == TransactionType.EXPENSE) {
            SectionCard(title = "Category") {
                val visible = if (categoriesExpanded) categories else categories.take(5)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    visible.forEach { category ->
                        val tint = parseColorHex(category.colorHex)
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { onCategorySelect(category.id) },
                            enabled = enabled,
                            label = { Text(category.name) },
                            leadingIcon = { Icon(IconMapper.getIcon(category.iconName), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    if (categories.size > 5) {
                        FilterChip(
                            selected = categoriesExpanded,
                            onClick = { categoriesExpanded = !categoriesExpanded },
                            enabled = enabled,
                            label = { Text(if (categoriesExpanded) "Less" else "More") },
                            trailingIcon = { Icon(if (categoriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null) }
                        )
                    }
                }
            }
            SectionCard(title = "Payment method") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    paymentModes.forEach { mode ->
                        FilterChip(
                            selected = state.selectedPaymentModeId == mode.id,
                            onClick = { onPaymentModeSelect(mode.id) },
                            enabled = enabled,
                            label = { Text(mode.name) },
                            leadingIcon = { Icon(getPaymentModeIcon(mode.type), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }

        SectionCard(title = "Date") {
            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = state.timestamp }
                    DatePickerDialog(context, { _, year, month, day ->
                        val selected = Calendar.getInstance().apply {
                            timeInMillis = state.timestamp
                            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, day)
                        }
                        onTimestampChange(selected.timeInMillis)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatter.format(Date(state.timestamp)))
            }
        }

        SectionCard(title = "Merchant") {
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                enabled = enabled,
                label = { Text("Merchant or description (required)") },
                placeholder = { Text("e.g. Grocery store") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        SectionCard(title = "Notes") {
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                enabled = enabled,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }

        SectionCard(title = "Split bill", action = {
            TextButton(onClick = { onSplitExpandedChange(!splitExpanded) }, enabled = enabled) { Text(if (splitExpanded) "Collapse" else "Expand") }
        }) {
            if (!splitExpanded) {
                Text(if (state.isSplit) "Split details are saved. Expand to edit shares." else "Add other people’s shares to track your personal expense.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                Text("The amount above stays the full bill. Your share can be zero.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = state.myShareAmount,
                    onValueChange = onMyShareChange,
                    enabled = enabled,
                    label = { Text("Your share") },
                    prefix = { Text("${currencySymbol(state.currency)} ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = state.shareError != null,
                    supportingText = state.shareError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(2 to "Half", 3 to "Thirds", 4 to "Quarters").forEach { (divisor, label) ->
                        OutlinedButton(onClick = { onQuickSplit(divisor) }, enabled = enabled, modifier = Modifier.weight(1f)) { Text(label) }
                    }
                }
                state.splitParticipants.forEachIndexed { index, person ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = person.name,
                                onValueChange = { onUpdateParticipant(index, it, person.shareAmount) },
                                enabled = enabled,
                                label = { Text("Participant (required)") },
                                isError = state.participantErrors[index] != null && person.name.isBlank(),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = person.shareAmount,
                                onValueChange = { onUpdateParticipant(index, person.name, it) },
                                enabled = enabled,
                                label = { Text("Share (required)") },
                                prefix = { Text("${currencySymbol(state.currency)} ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = state.participantErrors[index] != null && person.shareAmount.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { onRemoveParticipant(index) }, enabled = enabled) { Icon(Icons.Default.Close, contentDescription = "Remove ${person.name.ifBlank { "participant" }}") }
                        }
                        state.participantErrors[index]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                TextButton(onClick = onAddParticipant, enabled = enabled) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add person")
                }
                val othersLabel = if (state.participantTotalIsFinite) formatCurrency(state.participantTotalAmount, state.currency) else "Too large"
                Text("Your share ${formatCurrency(state.personalShareValue, state.currency)} · Others $othersLabel", style = MaterialTheme.typography.labelMedium, color = SplitBadgeText)
            }
        }

        SectionCard(title = "Repeat monthly") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Repeat, contentDescription = null, tint = PrimaryEmerald)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Repeat monthly", style = MaterialTheme.typography.titleSmall)
                    Text("Keep this transaction recurring each month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.isRecurring, onCheckedChange = onRecurringChange, enabled = enabled)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionCard(title: String, action: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun ErrorNotice(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

private val previewCategories = listOf(
    CategoryEntity(id = 1, name = "Food", iconName = "restaurant", colorHex = "#F97316"),
    CategoryEntity(id = 2, name = "Shopping", iconName = "shopping_bag", colorHex = "#A855F7"),
    CategoryEntity(id = 3, name = "Transport", iconName = "directions_car", colorHex = "#3B82F6")
)
private val previewModes = listOf(PaymentModeEntity(id = 1, name = "UPI", type = PaymentModeType.UPI), PaymentModeEntity(id = 2, name = "Credit card", type = PaymentModeType.CREDIT_CARD))

@Preview(name = "Add expense · 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable private fun AddExpensePreview() = PreviewForm(AddEditTransactionUiState(isInitialized = true, description = "Dinner with friends", originalAmount = "2550", myShareAmount = "850", isSplit = true, splitParticipants = listOf(SplitParticipantItem(name = "Alex", shareAmount = "850"), SplitParticipantItem(name = "Sam", shareAmount = "850")), selectedCategoryId = 1, selectedPaymentModeId = 1), dark = false)

@Preview(name = "Add income · 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable private fun AddIncomePreview() = PreviewForm(AddEditTransactionUiState(type = TransactionType.INCOME, description = "Salary", originalAmount = "85000", selectedCategoryId = 1, selectedPaymentModeId = 1, isInitialized = true), dark = false)

@Preview(name = "Split validation · Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun SplitValidationPreview() = PreviewForm(AddEditTransactionUiState(description = "Dinner", originalAmount = "1000", myShareAmount = "0", isSplit = true, selectedCategoryId = 1, selectedPaymentModeId = 1, splitParticipants = listOf(SplitParticipantItem(name = "", shareAmount = "1200")), participantErrors = mapOf(0 to "Enter a participant name."), shareError = "Participant shares cannot exceed the full bill.", isInitialized = true), dark = true)

@Preview(name = "Add expense · 200% text", widthDp = 360, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable private fun LargeTextExpensePreview() = AddExpensePreview()

@Composable
private fun PreviewForm(state: AddEditTransactionUiState, dark: Boolean) {
    FinanceTrackerTheme(darkTheme = dark) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Text("Add transaction", Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
            AddEditTransactionForm(
                state = state,
                categories = previewCategories,
                paymentModes = previewModes,
                modifier = Modifier.weight(1f),
                splitExpanded = state.isSplit,
                onSplitExpandedChange = {}, onTypeSelect = {}, onAmountChange = {}, onCategorySelect = {},
                onPaymentModeSelect = {}, onTimestampChange = {}, onDescriptionChange = {}, onNotesChange = {},
                onRecurringChange = {}, onMyShareChange = {}, onQuickSplit = {},
                onAddParticipant = {}, onUpdateParticipant = { _, _, _ -> }, onRemoveParticipant = {}
            )
        }
    }
}
