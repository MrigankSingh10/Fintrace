package com.fintrace.app.data.sms

import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus

/**
 * Selects only pending SMS imports that the current parser no longer recognizes.
 * The status check is deliberately repeated here even though the DAO query is scoped
 * to pending rows, so confirmed, dismissed, and manual data cannot be cleanup targets.
 */
object SmsPendingCleanup {
    private val futureDebitReminderPattern = Regex(
        """\b(?:standing\s+(?:instruction|order)|mandate)\b[\s\S]{0,240}\b(?:due\s+(?:by|on)|scheduled\s+(?:for|on|to)|upcoming)\b|\b(?:due\s+(?:by|on)|scheduled\s+(?:for|on|to)|upcoming)\b[\s\S]{0,240}\b(?:standing\s+(?:instruction|order)|mandate)\b|\b(?:to|will|shall)\s+be\s+debited\b""",
        RegexOption.IGNORE_CASE
    )

    fun invalidRows(rows: List<TransactionEntity>): List<TransactionEntity> = rows.filter { row ->
        val body = row.smsRawBody
        row.status == TransactionStatus.PENDING &&
                !body.isNullOrBlank() &&
                SmsParser.parse(body, row.smsSender, row.timestamp) == null
    }

    /** Newly rejected future-debit reminders, isolated from the older general cleanup pass. */
    fun futureDebitReminderRows(rows: List<TransactionEntity>): List<TransactionEntity> = rows.filter { row ->
        val body = row.smsRawBody
        row.status == TransactionStatus.PENDING &&
                !body.isNullOrBlank() &&
                futureDebitReminderPattern.containsMatchIn(body) &&
                SmsParser.parse(body, row.smsSender, row.timestamp) == null
    }
}
