package com.fintrace.app.data.sms

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus

/**
 * Selects only pending SMS imports that the current parser no longer recognizes.
 * The status check is deliberately repeated here even though the DAO query is scoped
 * to pending rows, so confirmed, dismissed, and manual data cannot be cleanup targets.
 */
object SmsPendingCleanup {
    fun invalidRows(rows: List<TransactionEntity>): List<TransactionEntity> = rows.filter { row ->
        val body = row.smsRawBody
        row.status == TransactionStatus.PENDING &&
                !body.isNullOrBlank() &&
                SmsParser.parse(body, row.smsSender, row.timestamp) == null
    }
}
