package com.fintrace.app.data.model

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer")
}

enum class TransactionStatus(val label: String) {
    CONFIRMED("Confirmed"),
    PENDING("Pending Review")
}

enum class PaymentModeType(val label: String) {
    BANK_DEBIT("Bank Debit"),
    CREDIT_CARD("Credit Card"),
    UPI("UPI App"),
    CASH("Cash"),
    OTHER("Other")
}
