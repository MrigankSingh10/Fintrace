package com.fintrace.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.MonthlyFinancialSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.ui.components.CategoryIconBadge
import com.fintrace.app.ui.components.DualAmountDisplay
import com.fintrace.app.ui.components.PaymentModeBadge
import com.fintrace.app.ui.components.formatCurrency
import com.fintrace.app.ui.components.parseColorHex
import com.fintrace.app.ui.theme.AccentAmber
import com.fintrace.app.ui.theme.AccentPurple
import com.fintrace.app.ui.theme.DarkSurface
import com.fintrace.app.ui.theme.ExpenseRed
import com.fintrace.app.ui.theme.IncomeGreen
import com.fintrace.app.ui.theme.PrimaryBlue
import com.fintrace.app.ui.theme.PrimaryEmerald
import com.fintrace.app.ui.theme.SplitBadgeBg
import com.fintrace.app.ui.theme.SplitBadgeText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPaymentModes: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit
) {
    val period by viewModel.selectedPeriod.collectAsState()
    val summary by viewModel.monthlySummary.collectAsState()
    val categories by viewModel.categoryBreakdown.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val isSalaryDialogOpen by viewModel.isSalaryDialogOpen.collectAsState()

    val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Month Selector Bar
        MonthSelectorBar(
            displayName = period.displayName,
            onPrevious = { viewModel.onPreviousMonth() },
            onNext = { viewModel.onNextMonth() },
            onReset = { viewModel.onResetToCurrentMonth() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Financial Overview Card
        HeroFinancialCard(
            summary = summary,
            onEditSalary = { viewModel.onOpenSalaryDialog() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Daily Pace & Insights Card
        SpendingPaceCard(
            summary = summary,
            period = period
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Grid
        QuickActionsRow(
            onAddExpense = onNavigateToAddTransaction,
            onSetSalary = { viewModel.onOpenSalaryDialog() },
            onManageCategories = onNavigateToCategories,
            onManagePaymentModes = onNavigateToPaymentModes
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Category Spend Quick Glance
        if (categories.isNotEmpty()) {
            CategoryGlanceSection(
                categories = categories.take(4),
                totalSpent = summary.totalMyShareSpent
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Recent Transactions Feed
        RecentTransactionsSection(
            transactions = recentTransactions,
            dateFormatter = dateFormatter,
            onViewAll = onNavigateToTransactions,
            onTransactionClick = onNavigateToTransactionDetail
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Monthly Salary Dialog
    if (isSalaryDialogOpen) {
        MonthlySalaryDialog(
            currentSalary = summary.salaryAmount,
            monthName = period.displayName,
            isIncomeDerived = summary.isIncomeDerived,
            onDismiss = { viewModel.onDismissSalaryDialog() },
            onSave = { amount -> viewModel.saveMonthlySalary(amount) }
        )
    }
}

@Composable
fun MonthSelectorBar(
    displayName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onReset() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = PrimaryEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HeroFinancialCard(
    summary: MonthlyFinancialSummary,
    onEditSalary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spendFraction = if (summary.salaryAmount > 0) {
        (summary.totalMyShareSpent / summary.salaryAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val progressColor = when {
        spendFraction > 0.9f -> ExpenseRed
        spendFraction > 0.7f -> AccentAmber
        else -> PrimaryEmerald
    }

    val splitSavings = (summary.totalOriginalSpent - summary.totalMyShareSpent).coerceAtLeast(0.0)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryEmerald.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "REMAINING BALANCE",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (summary.salaryAmount <= 0.0) {
                        TextButton(
                            onClick = onEditSalary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PrimaryEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Set Salary", color = PrimaryEmerald, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Balance Number
                Text(
                    text = formatCurrency(summary.remainingBalance),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (summary.remainingBalance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = spendFraction,
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (summary.salaryAmount > 0) "${(spendFraction * 100).toInt()}% of budget spent" else "No salary set",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatCurrency(summary.salaryAmount - summary.totalMyShareSpent)} left",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = progressColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // 2 Column comparison: Salary vs Spends
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Salary
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Salary Credited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!summary.isIncomeDerived) {
                                IconButton(
                                    onClick = onEditSalary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Salary",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatCurrency(summary.salaryAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = IncomeGreen
                        )
                        if (summary.isIncomeDerived) {
                            Text(
                                text = "From confirmed income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Total Spent (My Share)
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Spent (My Share)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(summary.totalMyShareSpent),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ExpenseRed
                        )
                    }
                }

                // Split Savings Note
                if (splitSavings > 0.0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SplitBadgeBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💡 Total charged was ${formatCurrency(summary.totalOriginalSpent)} • Saved ${formatCurrency(splitSavings)} through shared splits",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SplitBadgeText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingPaceCard(
    summary: MonthlyFinancialSummary,
    period: MonthPeriod,
    modifier: Modifier = Modifier
) {
    val dailyAverage = if (period.daysElapsed > 0) {
        summary.totalMyShareSpent / period.daysElapsed
    } else 0.0

    val projectedMonthEnd = dailyAverage * period.daysInMonth

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spending Pace: ${formatCurrency(dailyAverage)} / day",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Day ${period.daysElapsed} of ${period.daysInMonth} • Projected spend: ${formatCurrency(projectedMonthEnd)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onAddExpense: () -> Unit,
    onSetSalary: () -> Unit,
    onManageCategories: () -> Unit,
    onManagePaymentModes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        QuickActionButton(
            label = "+ Expense",
            icon = Icons.Default.Add,
            accentColor = PrimaryEmerald,
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Set Salary",
            icon = Icons.Default.Payments,
            accentColor = PrimaryBlue,
            onClick = onSetSalary,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Categories",
            icon = Icons.Default.Category,
            accentColor = AccentPurple,
            onClick = onManageCategories,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "Modes",
            icon = Icons.Default.CreditCard,
            accentColor = AccentAmber,
            onClick = onManagePaymentModes,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryGlanceSection(
    categories: List<CategorySpendSummary>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top Spending Categories",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            categories.forEach { cat ->
                val fraction = if (totalSpent > 0) (cat.totalMyShareSpent / totalSpent).toFloat() else 0f
                val color = parseColorHex(cat.colorHex)

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${formatCurrency(cat.totalMyShareSpent)} (${String.format("%.1f", cat.percentageOfTotal)}%)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = fraction,
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionWithDetails>,
    dateFormatter: SimpleDateFormat,
    onViewAll: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Recent Spends",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onViewAll) {
                Text("View All", color = PrimaryEmerald, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (transactions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spends recorded this month yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transactions.forEach { item ->
                    val isIncome = item.transaction.type == com.fintrace.app.data.model.TransactionType.INCOME
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTransactionClick(item.transaction.id) }
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
                                    size = 40.dp,
                                    iconSize = 20.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(IncomeGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) { Text("+", color = IncomeGreen, style = MaterialTheme.typography.titleMedium) }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isIncome) "Income received" else item.transaction.description,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dateFormatter.format(Date(item.transaction.timestamp)),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!isIncome) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        PaymentModeBadge(mode = item.paymentMode)
                                    }
                                }
                            }

                            DualAmountDisplay(
                                originalAmount = item.transaction.originalAmount,
                                myShareAmount = item.transaction.myShareAmount,
                                isExpense = !isIncome
                            )
                        }
                    }
                }
            }
        }
    }
}
