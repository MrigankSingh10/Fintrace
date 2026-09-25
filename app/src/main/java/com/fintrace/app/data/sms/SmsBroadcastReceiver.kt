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

            // Find or match payment mode:
            // 1. If card last 4 digits were detected, check user's card_mappings first
            val paymentModes = database.paymentModeDao().getAllPaymentModes()
            val mappedPaymentModeId = parsed.cardLastFour?.let { lastFour ->
                database.cardMappingDao().getCardMappingByLastFour(lastFour)?.paymentModeId
            }

            val paymentModeId = mappedPaymentModeId ?: run {
                val matchedMode = parsed.paymentModeName?.let { name ->
                    paymentModes.find { it.name.equals(name, ignoreCase = true) }
                        ?: paymentModes.find { it.name.contains(name, ignoreCase = true) }
                }
                    ?: parsed.paymentModeType?.let { type -> paymentModes.find { it.type == type } }
                if (matchedMode != null) {
                    matchedMode.id
                } else if (parsed.paymentModeType == com.fintrace.app.data.model.PaymentModeType.CREDIT_CARD) {
                    // A card parse must never fall back to DEBIT when no credit-card mode exists
                    repository.ensureCreditCardMode()
                } else {
                    paymentModes.find { it.name.equals("DEBIT", ignoreCase = true) }?.id
                        ?: paymentModes.firstOrNull()?.id ?: 1L
                }
            }

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
                notes = "Auto-detected from SMS alert",
                parseConfidence = parsed.parseConfidence,
                cardLastFour = parsed.cardLastFour,
                currency = parsed.currencyCode
            )

            database.transactionDao().insertTransaction(pendingTransaction)
        }
    }
}
