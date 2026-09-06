package com.fintrace.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.fintrace.app.FinanceTrackerApp
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (context == null) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = StringBuilder()
        var sender: String? = null
        var timestamp = System.currentTimeMillis()

        for (msg in messages) {
            fullBody.append(msg.messageBody)
            sender = msg.originatingAddress
            timestamp = msg.timestampMillis
        }

        val body = fullBody.toString()
        val parsed = SmsParser.parse(body, sender, timestamp) ?: return

        val app = context.applicationContext as? FinanceTrackerApp ?: return
        val repository = app.repository
        val database = app.database

        CoroutineScope(Dispatchers.IO).launch {
            // Deduplicate
            if (repository.isSmsAlreadyProcessed(body)) {
                return@launch
            }

            // Find matching category (default to Needs or first category)
            val categories = database.categoryDao().getAllCategories()
            val defaultCategoryId = categories.find { it.name.equals("Needs", ignoreCase = true) }?.id
                ?: categories.firstOrNull()?.id ?: 1L

            // Find or match payment mode strictly by name first
            val paymentModes = database.paymentModeDao().getAllPaymentModes()
            val matchedMode = parsed.paymentModeName?.let { name ->
                paymentModes.find { it.name.equals(name, ignoreCase = true) }
                    ?: paymentModes.find { it.name.contains(name, ignoreCase = true) }
            }
                ?: parsed.paymentModeType?.let { type -> paymentModes.find { it.type == type } }
                ?: paymentModes.find { it.name.equals("DEBIT", ignoreCase = true) }
            val paymentModeId = matchedMode?.id ?: paymentModes.firstOrNull()?.id ?: 1L

            val pendingTransaction = TransactionEntity(
                id = 0,
                description = parsed.merchant,
                timestamp = parsed.timestamp,
                originalAmount = parsed.amount,
                myShareAmount = parsed.amount, // Defaults to 100% until confirmed or split
                categoryId = defaultCategoryId,
                paymentModeId = paymentModeId,
                type = parsed.transactionType,
                smsRawBody = parsed.rawBody,
                smsSender = parsed.sender,
                status = TransactionStatus.PENDING,
                notes = "Auto-detected from SMS alert"
            )

            database.transactionDao().insertTransaction(pendingTransaction)
        }
    }
}
