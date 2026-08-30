package com.fintrace.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fintrace.app.data.local.entity.CategoryEntity
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity

data class TransactionWithDetails(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?,

    @Relation(
        parentColumn = "paymentModeId",
        entityColumn = "id"
    )
    val paymentMode: PaymentModeEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "transaction_id"
    )
    val splits: List<TransactionSplitEntity> = emptyList()
) {
    val isSplit: Boolean
        get() = transaction.originalAmount != transaction.myShareAmount || splits.isNotEmpty()
}
