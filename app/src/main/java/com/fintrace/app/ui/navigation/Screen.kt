package com.fintrace.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Overview", Icons.Default.AccountBalanceWallet)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.ReceiptLong)
    object SmsInbox : Screen("sms_inbox", "SMS Review", Icons.Default.MarkEmailUnread)
    object Analytics : Screen("analytics", "Reports", Icons.Default.PieChart)
    object Categories : Screen("categories", "Categories", Icons.Default.Category)
    object PaymentModes : Screen("payment_modes", "Payment Modes", Icons.Default.CreditCard)
    object AddTransaction : Screen("add_transaction?transactionId={transactionId}", "Add Transaction") {
        fun createRoute(transactionId: Long = 0L) = "add_transaction?transactionId=$transactionId"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Transactions, SmsInbox, Analytics)
    }
}
