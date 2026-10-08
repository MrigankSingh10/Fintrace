package com.fintrace.app.ui.transactions

import com.fintrace.app.data.local.relation.TransactionWithDetails

/** Sorts visible transactions without changing the chronological order used by persistence. */
internal fun List<TransactionWithDetails>.sortedForPresentation(): List<TransactionWithDetails> =
    sortedWith(
        compareByDescending<TransactionWithDetails> { it.transaction.isRecurring }
            .thenByDescending { it.transaction.timestamp }
    )
