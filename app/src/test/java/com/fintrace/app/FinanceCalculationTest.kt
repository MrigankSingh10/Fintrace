package com.fintrace.app

import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculationTest {

    @Test
    fun testSplitExpenseCalculations() {
        val category = CategoryEntity(id = 1, name = "Needs", colorHex = "#3B82F6")
        val paymentMode = PaymentModeEntity(id = 1, name = "ICICI CORAL CREDIT", type = PaymentModeType.CREDIT_CARD)

        // Cab ride example: total ₹200, my share ₹100, friend share ₹100
        val transaction = TransactionEntity(
            id = 10,
            description = "Uber Cab Ride",
            timestamp = System.currentTimeMillis(),
            originalAmount = 200.0,
            myShareAmount = 100.0,
            categoryId = category.id,
            paymentModeId = paymentMode.id,
            type = TransactionType.EXPENSE,
            status = TransactionStatus.CONFIRMED
        )

        val splits = listOf(
            TransactionSplitEntity(
                id = 1,
                transactionId = 10,
                personName = "Me",
                shareAmount = 100.0,
                isUser = true
            ),
            TransactionSplitEntity(
                id = 2,
                transactionId = 10,
                personName = "Rahul",
                shareAmount = 100.0,
                isUser = false
            )
        )

        val transactionWithDetails = TransactionWithDetails(
            transaction = transaction,
            category = category,
            paymentMode = paymentMode,
            splits = splits
        )

        // Verify split detection
        assertTrue("Transaction should be detected as split expense", transactionWithDetails.isSplit)
        assertEquals(200.0, transactionWithDetails.transaction.originalAmount, 0.001)
        assertEquals(100.0, transactionWithDetails.transaction.myShareAmount, 0.001)

        // Verify that sum of splits equals original amount
        val splitSum = splits.sumOf { it.shareAmount }
        assertEquals(200.0, splitSum, 0.001)
    }

    @Test
    fun testMonthlyRemainingBalanceMath() {
        val salary = 100000.0

        // 3 expenses:
        // 1: Rent (Solo) -> original 25000, my share 25000
        // 2: Dinner split -> original 4000, my share 1000
        // 3: Groceries split -> original 6000, my share 3000
        val transactions = listOf(
            TransactionEntity(id = 1, description = "Rent", timestamp = 1000L, originalAmount = 25000.0, myShareAmount = 25000.0, categoryId = 1, paymentModeId = 1),
            TransactionEntity(id = 2, description = "Dinner with team", timestamp = 2000L, originalAmount = 4000.0, myShareAmount = 1000.0, categoryId = 2, paymentModeId = 2),
            TransactionEntity(id = 3, description = "Groceries", timestamp = 3000L, originalAmount = 6000.0, myShareAmount = 3000.0, categoryId = 1, paymentModeId = 4)
        )

        val totalOriginalCharged = transactions.sumOf { it.originalAmount }
        val totalMyShareSpent = transactions.sumOf { it.myShareAmount }
        val remainingBalance = salary - totalMyShareSpent

        assertEquals(35000.0, totalOriginalCharged, 0.001)
        assertEquals(29000.0, totalMyShareSpent, 0.001)
        assertEquals(71000.0, remainingBalance, 0.001)
    }

    @Test
    fun testUnequalSplitCalculation() {
        val totalBill = 1200.0
        val friend1Share = 350.0
        val friend2Share = 450.0

        val friendsSum = friend1Share + friend2Share
        val autoDeductedMyShare = (totalBill - friendsSum).coerceAtLeast(0.0)

        assertEquals(800.0, friendsSum, 0.001)
        assertEquals(400.0, autoDeductedMyShare, 0.001)
    }
}
