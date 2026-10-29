package com.fintrace.app.ui.transactions

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrace.app.ui.components.DateHeader
import com.fintrace.app.ui.components.FintraceComponentPreview
import com.fintrace.app.ui.components.TransactionRow

@Composable
private fun TransactionsStaticPreview() {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PendingReviewBanner(2, onClick = {})
        Text("JUNE 2026")
        DateHeader("Today · Wed, Jun 18", -850.0)
        TransactionRow("Dinner with friends", 850.0, originalAmount = 2_550.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "UPI · 8:45 PM")
        TransactionRow("Refund · Cafe", -145.0, originalAmount = -145.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "Card · 2:10 PM")
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
