package com.fintrace.app.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.ui.components.DateHeader
import com.fintrace.app.ui.components.FintraceComponentPreview
import com.fintrace.app.ui.components.SwipeableReviewCard
import com.fintrace.app.ui.components.TransactionRow

@Composable
private fun TransactionsStaticPreview() {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(true, {}, label = { Text("All") })
            FilterChip(false, {}, label = { Text("Pending 2") })
            FilterChip(false, {}, label = { Text("Dismissed 1") })
        }
        Text("ALL · JUNE 2026", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DateHeader("Today · Wed, Jun 18", -850.0)
        TransactionRow("Dinner with friends", 850.0, originalAmount = 2_550.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "UPI · 8:45 PM")
        TransactionRow("Refund · Cafe", -145.0, originalAmount = -145.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "Card · 2:10 PM")
        Spacer(Modifier.height(8.dp))
        Text("PENDING · ALL MONTHS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SwipeableReviewCard(
            title = "Online purchase", amount = 12_899.0,
            smsBody = "Card purchase of INR 12,899 at online merchant.",
            sender = "HDFCBK", categoryLabel = "Shopping", paymentLabel = "Credit card",
            onConfirm = {}, onDismiss = {}, onCategoryClick = {}, onPaymentClick = {}, onEdit = {}
        )
        Spacer(Modifier.height(8.dp))
        Text("DISMISSED · ALL MONTHS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TransactionRow("Cab fare", 420.0, categoryName = "Transport", categoryIcon = "DirectionsCar", categoryColorHex = "#3B82F6", status = TransactionStatus.DISMISSED)
        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Restore for review") }
    }
}

@Preview(name = "Transactions · 360dp light", widthDp = 360, heightDp = 1400, showBackground = true)
@Composable
private fun TransactionsLightPreview() = FintraceComponentPreview(false) { TransactionsStaticPreview() }

@Preview(name = "Transactions · 360dp dark", widthDp = 360, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TransactionsDarkPreview() = FintraceComponentPreview(true) { TransactionsStaticPreview() }

@Preview(name = "Transactions · 360dp 200% text", widthDp = 360, heightDp = 1600, fontScale = 2f, showBackground = true)
@Composable
private fun TransactionsLargeTextPreview() = FintraceComponentPreview(false) { TransactionsStaticPreview() }
