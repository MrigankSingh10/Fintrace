package com.fintrace.app.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsScanResult(
    val totalProcessed: Int,
    val newlyImported: Int
)

object SmsInboxScanner {

    suspend fun scanInbox(
        context: Context,
        repository: FinanceRepository,
        database: AppDatabase,
        lookbackDays: Int = 30
    ): SmsScanResult = withContext(Dispatchers.IO) {
        var processedCount = 0
        var importedCount = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val cutoffTimestamp = System.currentTimeMillis() - (lookbackDays * 24L * 60L * 60L * 1000L)
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                val categories = database.categoryDao().getAllCategories()
                val defaultCategoryId = categories.find { it.name.equals("Needs", ignoreCase = true) }?.id
                    ?: categories.firstOrNull()?.id ?: 1L
                val paymentModes = database.paymentModeDao().getAllPaymentModes()

                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                do {
                    val body = cursor.getString(bodyIdx) ?: continue
                    val sender = cursor.getString(addressIdx)
                    val date = cursor.getLong(dateIdx)

                    processedCount++

                    val parsed = SmsParser.parse(body, sender, date)
                    if (parsed != null) {
                        if (!repository.isSmsAlreadyProcessed(body)) {
                            val matchedMode = parsed.paymentModeName?.let { name ->
                                paymentModes.find { it.name.equals(name, ignoreCase = true) }
                                    ?: paymentModes.find { it.name.contains(name, ignoreCase = true) }
                            }
                                ?: parsed.paymentModeType?.let { type -> paymentModes.find { it.type == type } }
                                ?: paymentModes.find { it.name.equals("DEBIT", ignoreCase = true) }
                            val paymentModeId = matchedMode?.id ?: paymentModes.firstOrNull()?.id ?: 1L

                            val pending = TransactionEntity(
                                id = 0,
                                description = parsed.merchant,
                                timestamp = parsed.timestamp,
                                originalAmount = parsed.amount,
                                myShareAmount = parsed.amount,
                                categoryId = defaultCategoryId,
                                paymentModeId = paymentModeId,
                                type = parsed.transactionType,
                                smsRawBody = parsed.rawBody,
                                smsSender = parsed.sender,
                                status = TransactionStatus.PENDING,
                                notes = "Imported from SMS inbox"
                            )

                            database.transactionDao().insertTransaction(pending)
                            importedCount++
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        SmsScanResult(totalProcessed = processedCount, newlyImported = importedCount)
    }
}
