package com.fintrace.app

import android.app.Application
import com.fintrace.app.data.local.AppDatabase
import com.fintrace.app.data.local.entity.PaymentModeEntity
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.data.repository.FinanceRepositoryImpl
import com.fintrace.app.data.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceTrackerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        normalizeLegacySalaryRows()
        backfillTransactionParses()
    }

    /**
     * One-time fix for budgets saved when ADD_TO_SMS stored an *increment* on top of the
     * SMS income. Salary rows are now stored as the resulting total, so convert each legacy
     * ADD row to `that month's confirmed income + stored increment` to preserve the display.
     */
    private fun normalizeLegacySalaryRows() {
        val prefs = getSharedPreferences("salary_mode_normalization", MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) return
        applicationScope.launch {
            runCatching {
                val budgetDao = database.monthlyBudgetDao()
                val transactionDao = database.transactionDao()
                val budgets = budgetDao.getAllBudgets()
                for (budget in budgets) {
                    if (budget.salaryMode == SalaryMode.ADD_TO_SMS) {
                        val (start, end) = monthRange(budget.monthYear)
                        val income = transactionDao.getTotalConfirmedIncomeInRange(start, end)
                        budgetDao.upsertBudget(budget.copy(salaryAmount = income + budget.salaryAmount))
                    }
                }
                prefs.edit().putBoolean("done", true).apply()
            }
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
        lateinit var instance: FinanceTrackerApp
            private set
    }
}
