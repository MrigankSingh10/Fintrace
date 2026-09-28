package com.fintrace.app

import android.app.Application
import androidx.room.withTransaction
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.repository.FinanceRepositoryImpl
import com.fintrace.app.data.repository.incomeAdjustmentChanges
import com.fintrace.app.data.sms.SmsPendingCleanup
import com.fintrace.app.data.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceTrackerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * True once stored monthly budget rows can be read with adjustment semantics
     * (`confirmed income + manualAdjustment`). Until then the UI must not read
     * `monthly_budgets`: a legacy row holding a total would otherwise be added to confirmed
     * income and briefly show an inflated income.
     *
     * Set synchronously when no migration is pending, so normal launches are never gated.
     */
    private val _incomeModelReady = MutableStateFlow(false)
    val incomeModelReady: StateFlow<Boolean> = _incomeModelReady.asStateFlow()

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this, applicationScope)
    }

    val repository: FinanceRepository by lazy {
        FinanceRepositoryImpl(
            categoryDao = database.categoryDao(),
            paymentModeDao = database.paymentModeDao(),
            transactionDao = database.transactionDao(),
            monthlyBudgetDao = database.monthlyBudgetDao(),
            cardMappingDao = database.cardMappingDao()
        )
    }

    fun isDarkTheme(defaultValue: Boolean): Boolean =
        getSharedPreferences("display_preferences", MODE_PRIVATE)
            .getBoolean("dark_theme", defaultValue)

    fun setDarkTheme(enabled: Boolean) {
        getSharedPreferences("display_preferences", MODE_PRIVATE)
            .edit()
            .putBoolean("dark_theme", enabled)
            .apply()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        normalizeLegacyIncomeAdjustments()
        backfillTransactionParses()
        cleanupInvalidPendingSmsImports()
    }

    /**
     * One-time conversion of `monthly_budgets.salary_amount` from "final displayed total" to
     * "manual adjustment", so upgrading does not change any month's displayed income.
     *
     * Both legacy passes run in a single transaction because the conversion is not idempotent:
     * `total - confirmedIncome` applied twice to the same row would subtract income twice.
     * The completion flag is written only after the transaction returns, so a failed run simply
     * retries from the original values.
     *
     * Pass A reproduces the previous app version's own normalization for very old `ADD_TO_SMS`
     * rows, which stored only the increment. Pass B converts every stored total to an adjustment.
     */
    private fun normalizeLegacyIncomeAdjustments() {
        val prefs = getSharedPreferences("income_adjustment_model", MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("salary_mode_normalization", MODE_PRIVATE)
        val migrationDone = prefs.getBoolean(MIGRATION_DONE_KEY, false)
        if (migrationDone) {
            _incomeModelReady.value = true
            return
        }

        applicationScope.launch {
            runCatching {
                val budgetDao = database.monthlyBudgetDao()
                val transactionDao = database.transactionDao()
                val legacyAddPassDone = legacyPrefs.getBoolean("done", false)
                val budgets = budgetDao.getAllBudgets()

                val updatedRows = database.withTransaction {
                    val incomeByMonth = budgets.associate { budget ->
                        val (start, end) = monthRange(budget.monthYear)
                        budget.monthYear to transactionDao.getTotalConfirmedIncomeInRange(start, end)
                    }
                    val changes = incomeAdjustmentChanges(
                        migrationDone = false,
                        legacyAddPassDone = legacyAddPassDone,
                        budgets = budgets,
                        confirmedIncomeByMonth = { incomeByMonth[it] ?: 0.0 }
                    )
                    changes.forEach { budgetDao.upsertBudget(it) }
                    changes
                }

                // The old pass is now part of this one entry point, so its own flag is retired here.
                if (!legacyAddPassDone) {
                    legacyPrefs.edit().putBoolean("done", true).apply()
                }
                prefs.edit().putBoolean(MIGRATION_DONE_KEY, true).apply()
                updatedRows
            }
            // Always release the UI gate, even on failure: a wrong number beats a blocked app.
            _incomeModelReady.value = true
        }
    }

    /**
     * One-time backfill that re-parses every stored SMS body with the current parser and
     * refreshes confidence, card digits, currency, and (for card parses) the payment mode.
     * Fixes rows imported by older parser builds that show "Partial Parse" or lose the
     * card-mapping prompt even though the current parser handles them fully.
     */
    private fun backfillTransactionParses() {
        val prefs = getSharedPreferences("parse_backfill", MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) return
        applicationScope.launch {
            runCatching {
                val txDao = database.transactionDao()
                val cardMappingDao = database.cardMappingDao()
                val paymentModeDao = database.paymentModeDao()
                val rows = txDao.getAllTransactionsWithSmsBody()
                for (row in rows) {
                    val body = row.smsRawBody ?: continue
                    val parsed = SmsParser.parse(body, row.smsSender, row.timestamp) ?: continue
                    var updated = row.copy(
                        parseConfidence = parsed.parseConfidence,
                        cardLastFour = parsed.cardLastFour ?: row.cardLastFour,
                        currency = parsed.currencyCode
                    )
                    if (parsed.paymentModeType == PaymentModeType.CREDIT_CARD) {
                        val mapping = parsed.cardLastFour?.let { cardMappingDao.getCardMappingByLastFour(it) }
                        val modeId = mapping?.paymentModeId ?: run {
                            paymentModeDao.getAllPaymentModes().firstOrNull { it.type == PaymentModeType.CREDIT_CARD }?.id
                                ?: paymentModeDao.insertPaymentMode(
                                    PaymentModeEntity(
                                        name = "Credit Card (Unmapped)",
                                        type = PaymentModeType.CREDIT_CARD,
                                        iconName = "CreditCard",
                                        isDefault = false
                                    )
                                )
                        }
                        updated = updated.copy(paymentModeId = modeId)
                    }
                    txDao.updateTransaction(updated)
                }
                prefs.edit().putBoolean("done", true).apply()
            }
        }
    }

    /**
     * One-time cleanup for promotional messages admitted by the old permissive parser.
     * Only pending SMS imports are candidates; confirmed, dismissed, and manual rows are
     * never queried or deleted. The completion marker is written only after every delete
     * succeeds so an interrupted cleanup can safely retry on the next launch.
     */
    private fun cleanupInvalidPendingSmsImports() {
        val prefs = getSharedPreferences("sms_parser_cleanup", MODE_PRIVATE)
        val cleanupKey = "strong_evidence_v1_done"
        if (prefs.getBoolean(cleanupKey, false)) return
        applicationScope.launch {
            runCatching {
                val txDao = database.transactionDao()
                val pendingSmsRows = txDao.getPendingTransactionsWithSmsBody()
                SmsPendingCleanup.invalidRows(pendingSmsRows).forEach { row ->
                    txDao.deleteTransaction(row)
                }
                prefs.edit().putBoolean(cleanupKey, true).apply()
            }
        }
    }

    private fun monthRange(monthYear: String): Pair<Long, Long> {
        val parts = monthYear.split("-")
        val cal = Calendar.getInstance().apply {
            clear()
            set(parts[0].toInt(), parts[1].toInt() - 1, 1, 0, 0, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to (cal.timeInMillis - 1)
    }

    companion object {
        const val MIGRATION_DONE_KEY = "income_adjustment_model_v1_done"
        lateinit var instance: FinanceTrackerApp
            private set
    }
}
