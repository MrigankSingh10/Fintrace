package com.fintrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.model.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        ),
        ForeignKey(
            entity = PaymentModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentModeId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["paymentModeId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val timestamp: Long,
    @ColumnInfo(name = "original_amount")
    val originalAmount: Double,
    @ColumnInfo(name = "my_share_amount")
    val myShareAmount: Double,
    @ColumnInfo(name = "categoryId", defaultValue = "1")
    val categoryId: Long,
    @ColumnInfo(name = "paymentModeId", defaultValue = "1")
    val paymentModeId: Long,
    val type: TransactionType = TransactionType.EXPENSE,
    val smsRawBody: String? = null,
    val smsSender: String? = null,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val notes: String? = null
)
