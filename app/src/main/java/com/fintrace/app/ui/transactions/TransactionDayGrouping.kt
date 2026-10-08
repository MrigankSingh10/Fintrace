package com.fintrace.app.ui.transactions

import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class TransactionDayGroup(
    val date: LocalDate,
    val transactions: List<TransactionWithDetails>,
    val netByCurrency: Map<String, Double>
) {
    val hasMixedCurrencies: Boolean get() = netByCurrency.keys.size > 1
}

internal fun groupTransactionsByLocalDay(
    transactions: List<TransactionWithDetails>,
    zone: ZoneId = ZoneId.systemDefault()
): List<TransactionDayGroup> = transactions
    .groupBy { Instant.ofEpochMilli(it.transaction.timestamp).atZone(zone).toLocalDate() }
    .toSortedMap(compareByDescending { it })
    .map { (date, rows) ->
        val ordered = rows.sortedForPresentation()
        val nets = ordered.groupBy { it.transaction.currency.uppercase() }.mapValues { (_, currencyRows) ->
            currencyRows.sumOf { row ->
                val transaction = row.transaction
                when (transaction.type) {
                    TransactionType.INCOME -> transaction.myShareAmount
                    TransactionType.EXPENSE -> -transaction.myShareAmount
                    TransactionType.TRANSFER -> 0.0
                }
            }
        }
        TransactionDayGroup(date, ordered, nets)
    }
