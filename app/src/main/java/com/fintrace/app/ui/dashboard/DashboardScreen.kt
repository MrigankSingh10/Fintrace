package com.fintrace.app.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.components.CategoryIcon
import com.fintrace.app.ui.components.EmptyState
import com.fintrace.app.ui.components.MoneyTone
import com.fintrace.app.ui.components.MoneyText
import com.fintrace.app.ui.components.HomeMonthSwitcher
import com.fintrace.app.ui.components.RecurringBadge
import com.fintrace.app.ui.components.StatPill
import com.fintrace.app.ui.components.TransactionRow
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.fintraceCardBorder
import com.fintrace.app.ui.theme.LocalFintraceColors
import com.fintrace.app.ui.theme.FinanceTrackerTheme
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

internal fun categoryGlanceToggleLabel(isExpanded: Boolean): String =
    if (isExpanded) "Collapse top categories" else "Expand top categories"

internal fun dashboardCategoryBudgetSubtitle(category: CategorySpendSummary): String? {
    val isConfigured = category.configuredBudgetAmount != null || category.budgetPercentage != null
    if (!isConfigured) return null

    val effectiveBudget = category.budgetAmount ?: 0.0
    val utilization = if (effectiveBudget > 0.0) {
        (category.totalMyShareSpent / effectiveBudget).coerceAtLeast(0.0)
    } else {
        null
    }
    val utilizationLabel = utilization?.let { "${String.format(Locale.getDefault(), "%.0f", it * 100)}% used" }
        ?: if (category.budgetPercentage != null) "no income base" else "no spend capacity"
    return "Budget ${formatCurrency(effectiveBudget)} · $utilizationLabel"
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    sharedMonth: YearMonth? = null,
    onSharedMonthSelected: ((YearMonth) -> Unit)? = null,
    pendingCount: Int = 0,
    onNavigateToSmsReview: () -> Unit = {},
    onScanSmsInbox: (Context) -> Unit = {},
    onEnableSmsImport: () -> Unit = onNavigateToSmsReview,
    onNavigateToAnalytics: () -> Unit = onNavigateToTransactions,
    endOfMonthControls: @Composable () -> Unit = {}
) {
    val period by viewModel.selectedPeriod.collectAsState()
    val dashboardState by viewModel.monthUiState.collectAsState()
    val incomeDialogState by viewModel.incomeDialogState.collectAsState()
    val categoryBudgetDialog by viewModel.categoryBudgetDialogState.collectAsState()
    val bulkBudgetState by viewModel.bulkBudgetState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasSmsPermission by remember(context) { mutableStateOf(hasSmsImportPermissions(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasSmsPermission = hasSmsImportPermissions(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(sharedMonth, period.monthYearKey) {
        val requestedMonth = sharedMonth ?: return@LaunchedEffect
        if (runCatching { YearMonth.parse(period.monthYearKey) }.getOrNull() != requestedMonth) {
            viewModel.onMonthSelected(requestedMonth)
        }
    }

    val month = sharedMonth ?: runCatching { YearMonth.parse(period.monthYearKey) }.getOrElse { YearMonth.now() }
    val monthData = dashboardStateForMonth(dashboardState, month)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeMonthSwitcher(
                month = month,
                onMonthSelected = onSharedMonthSelected ?: viewModel::onMonthSelected,
                trailingContent = endOfMonthControls
            )

            if (pendingCount > 0) {
                PendingReviewBanner(count = pendingCount, onClick = onNavigateToSmsReview)
            }

            if (monthData == null) {
                DashboardLoadingCard(month)
            } else {
                val summary = requireNotNull(monthData.summary)
                val recentTransactions = latestDashboardTransactions(monthData.confirmedTransactions)
                val topCategories = monthData.categories
                    .sortedWith(compareByDescending<CategorySpendSummary> { it.totalMyShareSpent }.thenBy { it.categoryId })
                    .take(4)

                DashboardHero(summary = summary, onAdjustIncome = { viewModel.onOpenIncomeDialog(month) })

                if (topCategories.isNotEmpty()) {
                    CategoryGlanceSection(
                        categories = topCategories,
                        totalSpent = summary.totalMyShareSpent,
                        onEditBudget = viewModel::onEditCategoryBudget,
                        onOpenBulkBudgets = viewModel::onOpenBulkBudgets,
                        onSeeAll = onNavigateToAnalytics,
                        onManageCategories = onNavigateToCategories,
                        onManagePaymentModes = onNavigateToPaymentModes
                    )
                } else {
                    BudgetAccessCard(
                        onOpenBudgets = viewModel::onOpenBulkBudgets,
                        onManageCategories = onNavigateToCategories,
                        onManagePaymentModes = onNavigateToPaymentModes
                    )
                }

                RecentTransactionsSection(
                    transactions = recentTransactions,
                    onViewAll = onNavigateToTransactions,
                    onTransactionClick = onNavigateToTransactionDetail,
                    onAdd = onNavigateToAddTransaction,
                    smsActionLabel = if (hasSmsPermission) "Scan messages" else "Enable SMS import",
                    onReviewSms = {
                        if (hasSmsPermission) onScanSmsInbox(context) else onEnableSmsImport()
                    }
                )
            }

            Spacer(Modifier.height(88.dp)) // Lets the final content scroll clear of the shared Add button.
        }
    }

    incomeDialogState?.let { dialog ->
        MonthlyIncomeDialog(
            currentIncome = dialog.currentIncome,
            monthName = dialog.displayName,
            monthlyBudget = dialog.monthlyBudget,
            onDismiss = viewModel::onDismissIncomeDialog,
            onSave = viewModel::saveMonthlyIncomeAdjustment
        )
    }
    categoryBudgetDialog?.let { state ->
        CategoryBudgetDialog(
            categoryName = state.categoryName,
            currentBudget = state.currentBudget,
            monthlyIncome = state.monthlyIncome,
            otherPercentageBudgets = state.otherPercentageBudgets,
            onSave = { viewModel.onSaveCategoryBudget(state.categoryId, it) },
            onDismiss = viewModel::onDismissCategoryBudgetDialog
        )
    }
    if (bulkBudgetState.categories.isNotEmpty()) {
        BulkEditBudgetsDialog(
            budgets = bulkBudgetState,
            onDismiss = viewModel::onDismissBulkBudgets,
            onSave = viewModel::onSaveBulkBudgets
        )
    }
}

private fun hasSmsImportPermissions(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun DashboardLoadingCard(month: YearMonth) {
    Card(
        modifier = Modifier.fillMaxWidth().fintraceCardBorder(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Updating ${month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))}", style = MaterialTheme.typography.titleMedium)
            Text("Loading this month’s finances…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardHero(summary: MonthlyFinancialSummary, onAdjustIncome: () -> Unit) {
    val left = dashboardLeftThisMonth(summary.monthlyIncome, summary.totalMyShareSpent)
    val progress = dashboardSpendProgress(summary.monthlyIncome, summary.totalMyShareSpent)
    val spendPercent = dashboardSpendPercent(summary.monthlyIncome, summary.totalMyShareSpent)
    val semantic = LocalFintraceColors.current
    val adjustment = summary.manualAdjustment
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LEFT THIS MONTH", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = 1.sp)
                TextButton(onClick = onAdjustIncome) { Text("Adjust income", color = MaterialTheme.colorScheme.primary) }
            }
            MoneyText(
                amount = left,
                tone = if (left < 0.0) MoneyTone.Expense else MoneyTone.Neutral,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                currencyCode = "INR"
            )
            if (adjustment != 0.0) {
                Text(
                    text = "Includes ${if (adjustment > 0.0) "+" else "−"}${formatCurrency(kotlin.math.abs(adjustment))} manual adjustment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            LinearProgressIndicator(
                progress = progress,
                color = if (left < 0.0) semantic.expense else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )
            Text(
                text = spendPercent?.let { "${String.format(Locale.getDefault(), "%.1f", it)}% of monthly income spent" }
                    ?: "Set positive monthly income to track your spending",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Income", summary.monthlyIncome, modifier = Modifier.weight(1f), isIncome = true)
                StatPill("Spent", summary.totalMyShareSpent, modifier = Modifier.weight(1f), isIncome = false)
            }
        }
    }
}

@Composable
private fun PendingReviewBanner(count: Int, onClick: () -> Unit) {
    val semantic = LocalFintraceColors.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = semantic.pendingContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = semantic.pending)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Transactions to review", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text("$count pending ${if (count == 1) "transaction" else "transactions"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Review", style = MaterialTheme.typography.labelLarge, color = semantic.pending, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CategoryGlanceSection(
    categories: List<CategorySpendSummary>,
    totalSpent: Double,
    onEditBudget: (CategorySpendSummary) -> Unit,
    onOpenBulkBudgets: () -> Unit,
    onSeeAll: () -> Unit,
    onManageCategories: () -> Unit,
    onManagePaymentModes: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val shape = RoundedCornerShape(22.dp)
    Card(modifier = Modifier.fillMaxWidth().fintraceCardBorder(shape), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = shape) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { expanded = !expanded }
                        .semantics(mergeDescendants = true) {
                            contentDescription = categoryGlanceToggleLabel(expanded)
                            stateDescription = if (expanded) "Expanded" else "Collapsed"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Top categories", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onSeeAll) { Text("See all") }
            }
            if (expanded) {
                categories.forEach { category ->
                    val share = dashboardCategoryShare(category.totalMyShareSpent, totalSpent)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onEditBudget(category) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(category.iconName, category.colorHex, size = 38.dp, iconSize = 19.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(category.categoryName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${String.format(Locale.getDefault(), "%.1f", share)}% of spending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            MoneyText(category.totalMyShareSpent, style = MaterialTheme.typography.titleSmall)
                        }
                        LinearProgressIndicator(
                            progress = (share / 100.0).toFloat().coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                        )
                        dashboardCategoryBudgetSubtitle(category)?.let { subtitle ->
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Button(
                    onClick = onOpenBulkBudgets,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Edit category budgets") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onManageCategories, modifier = Modifier.weight(1f)) {
                        Text("Manage categories", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    TextButton(onClick = onManagePaymentModes, modifier = Modifier.weight(1f)) {
                        Text("Payment modes", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAccessCard(onOpenBudgets: () -> Unit, onManageCategories: () -> Unit, onManagePaymentModes: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Card(modifier = Modifier.fillMaxWidth().fintraceCardBorder(shape), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = shape) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your categories", style = MaterialTheme.typography.titleMedium)
            Text("Set category budgets now; your spending breakdown will appear as confirmed expenses are added.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenBudgets,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Edit category budgets") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onManageCategories, modifier = Modifier.weight(1f)) {
                    Text("Manage categories", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                TextButton(onClick = onManagePaymentModes, modifier = Modifier.weight(1f)) {
                    Text("Payment modes", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    transactions: List<TransactionWithDetails>,
    onViewAll: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAdd: () -> Unit,
    smsActionLabel: String,
    onReviewSms: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent transactions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onViewAll) { Text("See all") }
        }
        if (transactions.isEmpty()) {
            val shape = RoundedCornerShape(22.dp)
            Card(modifier = Modifier.fillMaxWidth().fintraceCardBorder(shape), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = shape) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmptyState(
                        title = "No confirmed activity yet",
                        message = "Add a transaction to start seeing your monthly picture.",
                        actionLabel = "Add transaction",
                        onAction = onAdd
                    )
                    OutlinedButton(onClick = onReviewSms, modifier = Modifier.fillMaxWidth()) { Text(smsActionLabel) }
                }
            }
        } else {
            transactions.forEach { item ->
                val dateDetail = SimpleDateFormat("dd MMM · h:mm a", Locale.getDefault()).format(Date(item.transaction.timestamp))
                val isIncome = item.transaction.type == TransactionType.INCOME
                val detail = if (isIncome) null else listOfNotNull(
                    item.paymentMode?.name,
                    dateDetail
                ).joinToString(" · ")
                TransactionRow(
                    title = item.transaction.description,
                    myShareAmount = item.transaction.myShareAmount,
                    originalAmount = item.transaction.originalAmount,
                    categoryName = item.category?.name,
                    categoryIcon = item.category?.iconName,
                    categoryColorHex = item.category?.colorHex,
                    detail = detail,
                    incomeDetail = if (isIncome) dateDetail else null,
                    type = item.transaction.type,
                    isRecurring = item.transaction.isRecurring,
                    currencyCode = item.transaction.currency,
                    onClick = { onTransactionClick(item.transaction.id) }
                )
            }
        }
    }
}

@Preview(name = "Home · Light")
@Composable
private fun DashboardLightPreview() = DashboardPreview(dark = false, left = 72480.5, empty = false, pending = 2)

@Preview(name = "Home · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardDarkPreview() = DashboardPreview(dark = true, left = 72480.5, empty = false, pending = 2)

@Preview(name = "Home · Empty + pending", fontScale = 2f)
@Composable
private fun DashboardEmptyPreview() = DashboardPreview(dark = false, left = 0.0, empty = true, pending = 3)

@Preview(name = "Home · Negative balance", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardNegativePreview() = DashboardPreview(dark = true, left = -4250.0, empty = false, pending = 0)

@Composable
private fun DashboardPreview(dark: Boolean, left: Double, empty: Boolean, pending: Int) {
    FinanceTrackerTheme(darkTheme = dark) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HomeMonthSwitcher(YearMonth.of(2026, 6), {}, trailingContent = {
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
            })
            DashboardHero(
                summary = MonthlyFinancialSummary("2026-06", 95000.0, 95000.0 - left, 95000.0 - left, left, manualAdjustment = 5000.0),
                onAdjustIncome = {}
            )
            if (pending > 0) PendingReviewBanner(pending) {}
            if (empty) {
                RecentTransactionsSection(emptyList(), {}, {}, {}, "Enable SMS import", {})
            } else {
                CategoryGlanceSection(
                    categories = previewCategories,
                    totalSpent = 22519.5,
                    onEditBudget = {}, onOpenBulkBudgets = {}, onSeeAll = {}, onManageCategories = {}, onManagePaymentModes = {}
                )
                Text("Recent transactions", style = MaterialTheme.typography.titleMedium)
                TransactionRow("Dinner with friends", 850.0, originalAmount = 2550.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "UPI · 8:45 PM", isRecurring = true, currencyCode = "INR")
            }
        }
    }
}

@Preview(name = "Home phone · 200%", widthDp = 360, heightDp = 1200, fontScale = 2f)
@Composable
private fun DashboardPhonePreview() {
    FinanceTrackerTheme {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HomeMonthSwitcher(YearMonth.of(2026, 6), {}, trailingContent = {
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
            })
            CategoryGlanceSection(
                categories = previewCategories,
                totalSpent = 22519.5,
                onEditBudget = {}, onOpenBulkBudgets = {}, onSeeAll = {}, onManageCategories = {}, onManagePaymentModes = {}
            )
        }
    }
}

private val previewCategories = listOf(
    CategorySpendSummary(1, "Dining", "#F97316", "Fastfood", 12500.0, 14000.0, 8, budgetAmount = 16000.0, budgetUtilization = 0.78),
    CategorySpendSummary(2, "Shopping", "#8B5CF6", "LocalMall", 6800.0, 6800.0, 3),
    CategorySpendSummary(3, "Transport", "#3B82F6", "DirectionsCar", 3219.5, 3219.5, 5)
)
