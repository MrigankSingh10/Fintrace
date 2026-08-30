package com.fintrace.app.data.local.converter

import androidx.room.TypeConverter
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() } ?: TransactionType.EXPENSE

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus =
        value?.let { runCatching { TransactionStatus.valueOf(it) }.getOrNull() } ?: TransactionStatus.CONFIRMED

    @TypeConverter
    fun fromPaymentModeType(value: PaymentModeType?): String? = value?.name

    @TypeConverter
    fun toPaymentModeType(value: String?): PaymentModeType =
        value?.let { runCatching { PaymentModeType.valueOf(it) }.getOrNull() } ?: PaymentModeType.BANK_DEBIT
}
