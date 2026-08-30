package com.fintrace.app.data.local

import com.fintrace.app.data.local.dao.CategoryDao
import com.fintrace.app.data.local.dao.PaymentModeDao
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType

object DatabaseSeeder {

    val defaultCategories = listOf(
        CategoryEntity(id = 1, name = "Needs", colorHex = "#3B82F6", iconName = "ShoppingCart", isDefault = true, displayOrder = 1),
        CategoryEntity(id = 2, name = "Wants", colorHex = "#EC4899", iconName = "LocalMall", isDefault = true, displayOrder = 2),
        CategoryEntity(id = 3, name = "Investments", colorHex = "#10B981", iconName = "TrendingUp", isDefault = true, displayOrder = 3),
        CategoryEntity(id = 4, name = "Hospital", colorHex = "#EF4444", iconName = "LocalHospital", isDefault = true, displayOrder = 4),
        CategoryEntity(id = 5, name = "Others", colorHex = "#8B5CF6", iconName = "MoreHoriz", isDefault = true, displayOrder = 5)
    )

    val defaultPaymentModes = listOf(
        PaymentModeEntity(id = 1, name = "DEBIT", type = PaymentModeType.BANK_DEBIT, iconName = "AccountBalance", isDefault = true),
        PaymentModeEntity(id = 2, name = "ICICI Coral", type = PaymentModeType.CREDIT_CARD, iconName = "CreditCard", isDefault = true),
        PaymentModeEntity(id = 3, name = "ICICI APAY", type = PaymentModeType.CREDIT_CARD, iconName = "CreditCard", isDefault = true),
        PaymentModeEntity(id = 4, name = "HDFC Neu", type = PaymentModeType.CREDIT_CARD, iconName = "CreditCard", isDefault = true),
        PaymentModeEntity(id = 5, name = "CASH", type = PaymentModeType.CASH, iconName = "Payments", isDefault = true)
    )

    suspend fun seedDatabaseIfEmpty(categoryDao: CategoryDao, paymentModeDao: PaymentModeDao) {
        // Only insert defaults on first install — never overwrite user changes.
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(defaultCategories)
        }
        if (paymentModeDao.getPaymentModeCount() == 0) {
            paymentModeDao.insertPaymentModes(defaultPaymentModes)
        }
    }
}
