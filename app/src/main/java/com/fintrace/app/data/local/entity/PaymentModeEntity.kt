package com.fintrace.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrace.app.data.model.PaymentModeType

@Entity(
    tableName = "payment_modes",
    indices = [Index(value = ["name"], unique = true)]
)
data class PaymentModeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: PaymentModeType = PaymentModeType.BANK_DEBIT,
    val iconName: String = "CreditCard",
    val isDefault: Boolean = false
)
