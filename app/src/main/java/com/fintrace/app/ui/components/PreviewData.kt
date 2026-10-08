package com.fintrace.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType

/** Fixed fixtures for Compose previews. Values are illustrative and never used as app state. */
object FintracePreviewData {
    val categories = listOf(
        CategoryEntity(id = 1, name = "Dining", colorHex = "#F97316", iconName = "Fastfood"),
        CategoryEntity(id = 2, name = "Groceries", colorHex = "#22C55E", iconName = "ShoppingCart"),
        CategoryEntity(id = 3, name = "Transport", colorHex = "#3B82F6", iconName = "DirectionsCar")
    )
    val paymentModes = listOf(
        PaymentModeEntity(id = 1, name = "UPI", type = PaymentModeType.UPI),
        PaymentModeEntity(id = 2, name = "HDFC Credit Card", type = PaymentModeType.CREDIT_CARD)
    )
    val transactions = listOf(
        TransactionEntity(id = 101, description = "Salary · Northstar Labs", timestamp = 1_750_158_000_000, originalAmount = 95_000.0, myShareAmount = 95_000.0, categoryId = 1, paymentModeId = 1, type = TransactionType.INCOME),
        TransactionEntity(id = 102, description = "Dinner with friends", timestamp = 1_750_140_000_000, originalAmount = 2_550.0, myShareAmount = 850.0, categoryId = 1, paymentModeId = 1),
        TransactionEntity(id = 103, description = "Grocery market", timestamp = 1_750_120_000_000, originalAmount = 3_480.75, myShareAmount = 3_480.75, categoryId = 2, paymentModeId = 2),
        TransactionEntity(id = 104, description = "Online purchase", timestamp = 1_750_100_000_000, originalAmount = 12_899.0, myShareAmount = 12_899.0, categoryId = 3, paymentModeId = 2, status = TransactionStatus.PENDING, smsRawBody = "INR 12,899.00 spent on your HDFC Bank Credit Card ending 4218."),
        TransactionEntity(id = 105, description = "Cab fare", timestamp = 1_750_080_000_000, originalAmount = 2_450_000.0, myShareAmount = 2_450_000.0, categoryId = 3, paymentModeId = 1, status = TransactionStatus.DISMISSED)
    )
    val splitParticipants = listOf(
        SplitParticipant("self", "Me", "850.00"),
        SplitParticipant("friend", "Alex", "850.00"),
        SplitParticipant("friend-2", "Sam", "850.00")
    )
}

@Preview(name = "Fintrace component gallery · Light", showBackground = true, heightDp = 1100)
@Composable
private fun FintraceComponentGalleryLightPreview() {
    FintraceComponentPreview(darkTheme = false) { FintraceComponentGallery() }
}

@Preview(name = "Fintrace component gallery · Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 1100)
@Composable
private fun FintraceComponentGalleryDarkPreview() {
    FintraceComponentPreview(darkTheme = true) { FintraceComponentGallery() }
}

@Composable
private fun FintraceComponentGallery() {
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fintrace components", style = MaterialTheme.typography.headlineSmall)
        SummaryCard("June balance", 72_480.50, supportingText = "After confirmed transactions")
        RowStatGallery()
        SectionHeader("Recent activity", actionLabel = "See all", onAction = {})
        TransactionRow("Dinner with friends", 850.0, originalAmount = 2_550.0, categoryName = "Dining", categoryIcon = "Fastfood", categoryColorHex = "#F97316", detail = "UPI · Today", status = TransactionStatus.CONFIRMED)
        TransactionRow("Online purchase", 12_899.0, categoryName = "Shopping", categoryIcon = "LocalMall", categoryColorHex = "#A855F7", status = TransactionStatus.PENDING)
        TransactionRow("Cab fare", 2_450_000.0, categoryName = "Transport", categoryIcon = "DirectionsCar", categoryColorHex = "#3B82F6", status = TransactionStatus.DISMISSED)
        AmountInput("", {}, error = "Enter an amount")
        SplitEditor(2_550.0, FintracePreviewData.splitParticipants, 850.0)
        ChartCard("Spending by category", subtitle = "June 2026") { Text("Chart content supplied by the screen", modifier = Modifier.padding(vertical = 30.dp)) }
    }
}

@Composable
private fun RowStatGallery() {
    androidx.compose.foundation.layout.Row {
        StatPill("Income", 95_000.0, isIncome = true, modifier = Modifier.weight(1f))
        StatPill("Spending", 22_519.50, isIncome = false, modifier = Modifier.weight(1f))
    }
}
