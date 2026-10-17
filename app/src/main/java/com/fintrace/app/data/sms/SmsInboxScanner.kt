package com.fintrace.app.data.sms

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.model.TransactionStatus
import com.fintrace.app.data.repository.FinanceRepository
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class SmsScanResult(
    val totalProcessed: Int,
    val newlyImported: Int
)

data class SmsScanBounds(val startInclusive: Long, val endInclusive: Long)

object SmsScanRangeRules {
    const val DEFAULT_LOOKBACK_DAYS = 30
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun incremental(checkpoint: Long?, upperBound: Long, lookbackDays: Int = DEFAULT_LOOKBACK_DAYS): SmsScanBounds {
        // A future checkpoint means the device clock moved back (or stored data is corrupt).
        // Re-scan a bounded recent window and let the successful scan rebase the checkpoint.
        val usableCheckpoint = checkpoint?.takeIf { it <= upperBound }
        return SmsScanBounds(
            startInclusive = usableCheckpoint ?: (upperBound - lookbackDays * MILLIS_PER_DAY),
            endInclusive = upperBound
        )
    }

    fun historical(upperBound: Long, lookbackDays: Int = DEFAULT_LOOKBACK_DAYS): SmsScanBounds =
        SmsScanBounds(upperBound - lookbackDays * MILLIS_PER_DAY, upperBound)

    fun lastYear(upperBound: Long, timeZone: TimeZone = TimeZone.getDefault()): SmsScanBounds {
        val start = Calendar.getInstance(timeZone).apply {
            timeInMillis = upperBound
            add(Calendar.YEAR, -1)
        }.timeInMillis
        return SmsScanBounds(start, upperBound)
    }

    /** Converts the selected device-local calendar day into an inclusive start-of-day bound. */
    fun customStartOfDay(year: Int, monthZeroBased: Int, day: Int, timeZone: TimeZone = TimeZone.getDefault()): Long? {
        val selected = try {
            LocalDate.of(year, monthZeroBased + 1, day)
        } catch (_: DateTimeException) {
            return null
        }
        return selected.atStartOfDay(timeZone.toZoneId()).toInstant().toEpochMilli()
    }

    fun isFutureLocalDate(year: Int, monthZeroBased: Int, day: Int, upperBound: Long, timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        val selected = try {
            LocalDate.of(year, monthZeroBased + 1, day)
        } catch (_: DateTimeException) {
            return true
        }
        val today = Instant.ofEpochMilli(upperBound).atZone(timeZone.toZoneId()).toLocalDate()
        return selected.isAfter(today)
    }

    fun checkpointAfterSuccessfulIncrementalScan(existing: Long?, upperBound: Long): Long =
        if (existing != null && existing > upperBound) upperBound else maxOf(existing ?: Long.MIN_VALUE, upperBound)

    fun shouldAdvanceCheckpoint(isIncrementalScan: Boolean): Boolean = isIncrementalScan
}

object SmsScanCheckpoint {
    private const val PREFERENCES_NAME = "sms_scan_checkpoint"
    private const val KEY_LAST_SUCCESSFUL_UPPER_BOUND = "last_successful_upper_bound"

    fun read(context: Context): Long? = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .let { preferences ->
            if (preferences.contains(KEY_LAST_SUCCESSFUL_UPPER_BOUND)) {
                preferences.getLong(KEY_LAST_SUCCESSFUL_UPPER_BOUND, 0L)
            } else null
        }

    fun write(context: Context, upperBound: Long) {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val existing = read(context)
        val monotonicValue = SmsScanRangeRules.checkpointAfterSuccessfulIncrementalScan(existing, upperBound)
        check(preferences.edit().putLong(KEY_LAST_SUCCESSFUL_UPPER_BOUND, monotonicValue).commit()) {
            "Could not save SMS scan checkpoint"
        }
    }
}

object SmsInboxScanner {

    suspend fun scanInbox(
        context: Context,
        repository: FinanceRepository,
        database: AppDatabase,
        lookbackDays: Int = SmsScanRangeRules.DEFAULT_LOOKBACK_DAYS
    ): SmsScanResult = scan(context, repository, database, lookbackDays, useCheckpoint = true)

    suspend fun rescanLastDays(
        context: Context,
        repository: FinanceRepository,
        database: AppDatabase,
        lookbackDays: Int = SmsScanRangeRules.DEFAULT_LOOKBACK_DAYS
    ): SmsScanResult = scan(context, repository, database, lookbackDays, useCheckpoint = false)

    suspend fun rescanLastYear(context: Context, repository: FinanceRepository, database: AppDatabase): SmsScanResult =
        scan(context, repository, database, SmsScanRangeRules.DEFAULT_LOOKBACK_DAYS, useCheckpoint = false, startInclusive = null, oneYear = true)

    suspend fun rescanFrom(context: Context, repository: FinanceRepository, database: AppDatabase, startInclusive: Long): SmsScanResult =
        scan(context, repository, database, SmsScanRangeRules.DEFAULT_LOOKBACK_DAYS, useCheckpoint = false, startInclusive = startInclusive)

    private suspend fun scan(
        context: Context,
        repository: FinanceRepository,
        database: AppDatabase,
        lookbackDays: Int,
        useCheckpoint: Boolean,
        startInclusive: Long? = null,
        oneYear: Boolean = false
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val checkpoint = if (useCheckpoint) SmsScanCheckpoint.read(appContext) else null
        val upperBound = System.currentTimeMillis()
        val bounds = if (useCheckpoint) {
            SmsScanRangeRules.incremental(checkpoint, upperBound, lookbackDays)
        } else {
            when {
                oneYear -> SmsScanRangeRules.lastYear(upperBound)
                startInclusive != null -> SmsScanBounds(startInclusive, upperBound)
                else -> SmsScanRangeRules.historical(upperBound, lookbackDays)
            }
        }
        var processedCount = 0
        var importedCount = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.DATE_SENT
        )
        // DATE is capped at this captured time; SMS rows dated in the device-clock future
        // are intentionally left for a later scan when their timestamp is no longer future.
        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val selectionArgs = arrayOf(bounds.startInclusive.toString(), bounds.endInclusive.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            ) ?: throw IllegalStateException("Could not read SMS inbox")

            if (cursor.moveToFirst()) {
                val categories = database.categoryDao().getAllCategories()
                val defaultCategoryId = categories.find { it.name.equals("Needs", ignoreCase = true) }?.id
                    ?: categories.firstOrNull()?.id ?: 1L
                val paymentModes = database.paymentModeDao().getAllPaymentModes()
                val cardMappings = database.cardMappingDao().getAllCardMappings()
                val cardMappingMap = cardMappings.associateBy { it.cardLastFour }

                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val dateSentIdx = cursor.getColumnIndex(Telephony.Sms.DATE_SENT)

                do {
                    currentCoroutineContext().ensureActive()
                    val body = cursor.getString(bodyIdx) ?: continue
                    val sender = cursor.getString(addressIdx)
                    val receivedDate = cursor.getLong(dateIdx)
                    val sentDate = dateSentIdx.takeIf { it >= 0 }?.let { cursor.getLong(it) }
                    val sourceTimestamp = sentDate?.takeIf { it > 0L } ?: receivedDate

                    processedCount++

                    val parsed = SmsParser.parse(body, sender, receivedDate)
                    if (parsed != null) {
                        val mappedPaymentModeId = parsed.cardLastFour?.let { lastFour ->
                            cardMappingMap[lastFour]?.paymentModeId
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
                                repository.ensureCreditCardMode()
                            } else {
                                paymentModes.find { it.name.equals("DEBIT", ignoreCase = true) }?.id
                                    ?: paymentModes.firstOrNull()?.id ?: 1L
                            }
                        }

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
                            smsSourceTimestamp = sourceTimestamp,
                            status = TransactionStatus.PENDING,
                            notes = "Imported from SMS inbox",
                            parseConfidence = parsed.parseConfidence,
                            cardLastFour = parsed.cardLastFour,
                            currency = parsed.currencyCode
                        )

                        if (repository.insertPendingSmsIfNew(pending, receivedDate)) importedCount++
                    }
                } while (cursor.moveToNext())
            }

            cursor.close()
            cursor = null
            currentCoroutineContext().ensureActive()
            if (SmsScanRangeRules.shouldAdvanceCheckpoint(useCheckpoint)) SmsScanCheckpoint.write(appContext, upperBound)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            cursor?.close()
        }

        SmsScanResult(totalProcessed = processedCount, newlyImported = importedCount)
    }
}
