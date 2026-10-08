package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    currencyCode: String = "INR",
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fintraceCardBorder(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leadingContent?.invoke()
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MoneyText(amount = amount, currencyCode = currencyCode, style = MaterialTheme.typography.headlineSmall)
            supportingText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    isIncome: Boolean,
    currencyCode: String = "INR"
) {
    val tone = if (isIncome) MoneyTone.Income else MoneyTone.Expense
    val icon = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyText(amount, tone = tone, currencyCode = currencyCode, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Preview(name = "Summary card · Light", showBackground = true)
@Composable private fun SummaryCardLightPreview() { FintraceComponentPreview(false) { SummaryCard("Monthly balance", 72480.50, supportingText = "June 2026") } }
@Preview(name = "Summary card · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun SummaryCardDarkPreview() { FintraceComponentPreview(true) { SummaryCard("Monthly balance", 72480.50, supportingText = "June 2026") } }
@Preview(name = "Stat pill · Light", showBackground = true)
@Composable private fun StatPillLightPreview() { FintraceComponentPreview(false) { StatPill("Income", 95000.0, isIncome = true) } }
@Preview(name = "Stat pill · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable private fun StatPillDarkPreview() { FintraceComponentPreview(true) { StatPill("Spending", 22519.50, isIncome = false) } }
