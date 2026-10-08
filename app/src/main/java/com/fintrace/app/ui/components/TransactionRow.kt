package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.ui.theme.LocalFintraceColors

@Composable
fun TransactionRow(
    title: String,
    myShareAmount: Double,
    modifier: Modifier = Modifier,
    originalAmount: Double = myShareAmount,
    categoryName: String? = null,
    categoryIcon: String? = null,
    categoryColorHex: String? = null,
    detail: String? = null,
    type: TransactionType = TransactionType.EXPENSE,
    status: TransactionStatus = TransactionStatus.CONFIRMED,
    isRecurring: Boolean = false,
    currencyCode: String = "INR",
    onClick: (() -> Unit)? = null
) {
    val split = originalAmount != myShareAmount
    val signedAmount = transactionAmountDisplay(type, myShareAmount)
    val amountTone = when (type) {
        TransactionType.INCOME -> MoneyTone.Income
        TransactionType.EXPENSE -> MoneyTone.Expense
        TransactionType.TRANSFER -> MoneyTone.Neutral
    }
    val subdued = status == TransactionStatus.DISMISSED
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier.fillMaxWidth().fintraceCardBorder(cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == TransactionStatus.PENDING) {
                Box(
                    Modifier.width(4.dp).height(48.dp).background(LocalFintraceColors.current.pending, RoundedCornerShape(4.dp))
                )
            }
            Row(
                modifier = Modifier.weight(1f).padding(
                    start = if (status == TransactionStatus.PENDING) 12.dp else 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(categoryIcon, categoryColorHex, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        color = if (subdued) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categoryName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    if (isRecurring) RecurringBadge()
                    if (status == TransactionStatus.PENDING) StatusBadge(status = status)
                    if (status == TransactionStatus.DISMISSED) StatusBadge(status = status)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (split) {
                            Icon(Icons.Default.CallSplit, contentDescription = "Split bill", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        MoneyText(
                            amount = signedAmount.amount,
                            tone = if (subdued) MoneyTone.Neutral else amountTone,
                            sign = signedAmount.sign,
                            currencyCode = currencyCode,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (split) Text("of ${formatCurrency(kotlin.math.abs(originalAmount), currencyCode)} bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RecurringBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(top = 2.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(12.dp))
            Text("Recurring", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DateHeader(label: String, netAmount: Double, modifier: Modifier = Modifier, currencyCode: String = "INR") {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        MoneyText(netAmount, currencyCode = currencyCode, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusBadge(status: TransactionStatus, modifier: Modifier = Modifier) {
    val semantic = LocalFintraceColors.current
    val (label, color, bg) = when (status) {
        TransactionStatus.PENDING -> Triple("Pending review", semantic.pending, semantic.pendingContainer)
        TransactionStatus.DISMISSED -> Triple("Dismissed", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
        TransactionStatus.CONFIRMED -> Triple("Confirmed", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }
    Surface(
        modifier = modifier.padding(top = 2.dp),
        color = bg,
        contentColor = color,
        shape = CircleShape
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(name = "Transaction row · Light", showBackground = true)
@Composable private fun TransactionRowLightPreview() { FintraceComponentPreview(false) { TransactionRow("Dinner with friends", 850.0, originalAmount = 2550.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "UPI · 8:45 PM", status = TransactionStatus.PENDING, isRecurring = true, modifier = Modifier.padding(8.dp)) } }
@Preview(name = "Transaction row · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun TransactionRowDarkPreview() { FintraceComponentPreview(true) { TransactionRow("Salary", 95000.0, type = TransactionType.INCOME, status = TransactionStatus.DISMISSED, modifier = Modifier.padding(8.dp)) } }
@Preview(name = "Date header · Light", showBackground = true)
@Composable private fun DateHeaderLightPreview() { FintraceComponentPreview(false) { DateHeader("Today · Wed, Jun 18", -2840.0) } }
@Preview(name = "Date header · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun DateHeaderDarkPreview() { FintraceComponentPreview(true) { DateHeader("Yesterday", 12950.0) } }
@Preview(name = "Status badge · Light", showBackground = true)
@Composable private fun StatusBadgeLightPreview() { FintraceComponentPreview(false) { StatusBadge(TransactionStatus.PENDING) } }
@Preview(name = "Status badge · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun StatusBadgeDarkPreview() { FintraceComponentPreview(true) { StatusBadge(TransactionStatus.DISMISSED) } }
