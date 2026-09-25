package com.fintrace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_mappings",
    foreignKeys = [
        ForeignKey(
            entity = PaymentModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentModeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cardLastFour"], unique = true),
        Index(value = ["paymentModeId"])
    ]
)
data class CardMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "cardLastFour")
    val cardLastFour: String,
    @ColumnInfo(name = "paymentModeId")
    val paymentModeId: Long,
    @ColumnInfo(name = "label")
    val label: String? = null
)
