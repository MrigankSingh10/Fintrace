package com.fintrace.app.ui.analytics

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.model.PaymentModeType
import com.fintrace.app.data.model.TransactionType
import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.local.relation.TransactionWithDetails
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.util.ExcelWorkbookGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class AnalyticsTimeframe(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    YEAR_TO_DATE("Year to Date"),
    SPECIFIC_MONTH("Specific month"),
    ALL_TIME("All Time")
}

data class PaymentModeSpendSummary(
    val paymentModeId: Long,
    val paymentModeName: String,
    val paymentModeType: PaymentModeType? = null,
    val totalMyShareSpent: Double,
    val totalOriginalSpent: Double,
    val totalSplitOwed: Double = (totalOriginalSpent - totalMyShareSpent).coerceAtLeast(0.0),
    val count: Int,
    val percentageOfTotal: Double = 0.0
)

data class AnalyticsSummaryMetrics(
    val totalMyShareSpent: Double = 0.0,
    val totalOriginalCharged: Double = 0.0,
    val totalSavingsFromSplits: Double = 0.0,
    val transactionCount: Int = 0,
    val topCategory: CategorySpendSummary? = null
)

class AnalyticsViewModel(
    private val repository: FinanceRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _selectedTimeframe = MutableStateFlow(AnalyticsTimeframe.THIS_MONTH)
    val selectedTimeframe = _selectedTimeframe.asStateFlow()

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategorySpendSummary?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private var hasReceivedInitialSharedMonth = false

    private val periodTimestamps: StateFlow<Pair<Long, Long>> = combine(_selectedTimeframe, _selectedMonth) { timeframe, month ->
        computeTimeframeRangeAt(timeframe, System.currentTimeMillis(), month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = computeTimeframeRangeAt(AnalyticsTimeframe.THIS_MONTH, System.currentTimeMillis(), YearMonth.now())
    )

    val displayedPeriod: StateFlow<String> = combine(_selectedTimeframe, _selectedMonth) { timeframe, month ->
        if (timeframe == AnalyticsTimeframe.SPECIFIC_MONTH) month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        else timeframe.label
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsTimeframe.THIS_MONTH.label)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryBreakdown: StateFlow<List<CategorySpendSummary>> = periodTimestamps.flatMapLatest { (start, end) ->
        repository.getCategoryBreakdown(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            categoryBreakdown.collect { values ->
                val selectedId = _selectedCategory.value?.categoryId ?: return@collect
                if (values.none { it.categoryId == selectedId }) _selectedCategory.value = null
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionWithDetails>> = periodTimestamps.flatMapLatest { (start, end) ->
        repository.getTransactionsForRange(start, end)
            .map { transactions -> transactions.filter { it.transaction.type == TransactionType.EXPENSE } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paymentModeBreakdown: StateFlow<List<PaymentModeSpendSummary>> = combine(
        transactions,
        categoryBreakdown
    ) { txns, _ ->
        buildPaymentModeSummaries(txns)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val metrics: StateFlow<AnalyticsSummaryMetrics> = combine(
        categoryBreakdown,
        transactions
    ) { cats, txns ->
        val totalMyShare = cats.sumOf { it.totalMyShareSpent }
        val totalOriginal = cats.sumOf { it.totalOriginalSpent }
        val savings = (totalOriginal - totalMyShare).coerceAtLeast(0.0)
        val topCat = cats.maxByOrNull { it.totalMyShareSpent }

        AnalyticsSummaryMetrics(
            totalMyShareSpent = totalMyShare,
            totalOriginalCharged = totalOriginal,
            totalSavingsFromSplits = savings,
            transactionCount = txns.size,
            topCategory = topCat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsSummaryMetrics()
    )

    fun onTimeframeSelected(timeframe: AnalyticsTimeframe) {
        _selectedTimeframe.value = timeframe
        _selectedCategory.value = null
    }

    fun onMonthSelected(month: YearMonth) {
        if (_selectedMonth.value == month) return
        _selectedMonth.value = month
        _selectedCategory.value = null
    }

    /**
     * The shell month becomes the initial report period once Analytics is first presented.
     * After that, changing the shell month updates SPECIFIC_MONTH's value without overriding
     * a timeframe the user explicitly chose.
     */
    fun onSharedMonthPresented(month: YearMonth) {
        if (!hasReceivedInitialSharedMonth) {
            hasReceivedInitialSharedMonth = true
            if (_selectedMonth.value != month) {
                _selectedMonth.value = month
                _selectedCategory.value = null
            }
            _selectedTimeframe.value = AnalyticsTimeframe.SPECIFIC_MONTH
            return
        }
        syncSharedMonth(month)
    }

    /** Synchronizes a changed shell month while preserving the selected report timeframe. */
    fun syncSharedMonth(month: YearMonth) {
        if (_selectedMonth.value != month) {
            _selectedMonth.value = month
            _selectedCategory.value = null
        }
    }

    fun onCategorySelected(category: CategorySpendSummary?) {
        _selectedCategory.value = if (_selectedCategory.value?.categoryId == category?.categoryId) null else category
    }

    /**
     * Default suggested filename for SAF CreateDocument.
     */
    fun beginExport(): String {
        val timeframe = _selectedTimeframe.value
        val nowMillis = System.currentTimeMillis()
        val selectedMonth = _selectedMonth.value
        val period = analyticsExportPeriod(timeframe, nowMillis, selectedMonth)
        savedStateHandle[KEY_PENDING_EXPORT_TIMEFRAME] = timeframe.name
        savedStateHandle[KEY_PENDING_EXPORT_TIMESTAMP] = nowMillis
        if (timeframe == AnalyticsTimeframe.SPECIFIC_MONTH) savedStateHandle[KEY_PENDING_EXPORT_MONTH] = selectedMonth.toString()
        else savedStateHandle.remove<String>(KEY_PENDING_EXPORT_MONTH)
        val safePeriod = period.first.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        return "Finance_Tracker_${safePeriod}.xlsx"
    }

    /**
     * Write the .xlsx file to the Uri chosen by the user via SAF CreateDocument.
     * Call this AFTER the launcher returns a non-null Uri.
     */
    fun cancelPendingExport() {
        savedStateHandle.remove<String>(KEY_PENDING_EXPORT_TIMEFRAME)
        savedStateHandle.remove<Long>(KEY_PENDING_EXPORT_TIMESTAMP)
        savedStateHandle.remove<String>(KEY_PENDING_EXPORT_MONTH)
    }

    fun exportToXlsx(context: Context, uri: Uri): Boolean {
        val timeframe = savedStateHandle.get<String>(KEY_PENDING_EXPORT_TIMEFRAME)
            ?.let { runCatching { AnalyticsTimeframe.valueOf(it) }.getOrNull() }
            ?: return false
        val periodTimestamp = savedStateHandle.get<Long>(KEY_PENDING_EXPORT_TIMESTAMP) ?: return false
        val capturedMonth = savedStateHandle.get<String>(KEY_PENDING_EXPORT_MONTH)
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        val appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val exportMonth = capturedMonth ?: YearMonth.from(
                    java.time.Instant.ofEpochMilli(periodTimestamp).atZone(ZoneId.systemDefault())
                )
                val (start, end) = computeTimeframeRangeAt(timeframe, periodTimestamp, exportMonth)
                val exportTransactions = repository.getTransactionsForRange(start, end).first()
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                val exportCategories = repository.getCategoryBreakdown(start, end).first()
                val exportPaymentModes = buildPaymentModeSummaries(exportTransactions)
                val timeframeLabel = analyticsExportPeriod(timeframe, periodTimestamp, exportMonth).second
                withContext(Dispatchers.IO) {
                    val output = appContext.contentResolver.openOutputStream(uri)
                        ?: throw java.io.IOException("Could not open the selected file")
                    output.use { outStream ->
                        ExcelWorkbookGenerator.generateXlsx(
                            outputStream       = outStream,
                            transactions       = exportTransactions,
                            categoryBreakdown  = exportCategories,
                            paymentModeBreakdown = exportPaymentModes,
                            timeframeLabel     = timeframeLabel
                        )
                    }
                }
                cancelPendingExport()

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Excel report saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        return true
    }

    private fun computeTimeframeRangeAt(timeframe: AnalyticsTimeframe, nowMillis: Long, selectedMonth: YearMonth = YearMonth.now()): Pair<Long, Long> {
        if (timeframe == AnalyticsTimeframe.SPECIFIC_MONTH) return specificMonthTimestampRange(selectedMonth)
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val startCal = now.clone() as Calendar
        val endCal = now.clone() as Calendar
        when (timeframe) {
            AnalyticsTimeframe.THIS_MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
            }
            AnalyticsTimeframe.LAST_MONTH -> {
                startCal.add(Calendar.MONTH, -1)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.add(Calendar.MONTH, -1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
            }
            AnalyticsTimeframe.LAST_3_MONTHS -> {
                startCal.add(Calendar.MONTH, -3)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
            }
            AnalyticsTimeframe.YEAR_TO_DATE -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
            }
            AnalyticsTimeframe.ALL_TIME -> {
                startCal.set(2020, 0, 1, 0, 0, 0)
            }
            AnalyticsTimeframe.SPECIFIC_MONTH -> return specificMonthTimestampRange(selectedMonth)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    private companion object {
        const val KEY_PENDING_EXPORT_TIMEFRAME = "pending_export_timeframe"
        const val KEY_PENDING_EXPORT_TIMESTAMP = "pending_export_timestamp"
        const val KEY_PENDING_EXPORT_MONTH = "pending_export_month"
    }
}

internal fun specificMonthTimestampRange(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val nextMonth = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to nextMonth - 1
}

internal fun analyticsExportPeriod(timeframe: AnalyticsTimeframe, nowMillis: Long, selectedMonth: YearMonth? = null): Pair<String, String> {
    if (timeframe == AnalyticsTimeframe.SPECIFIC_MONTH) {
        val month = selectedMonth ?: YearMonth.now()
        val label = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
        return month.format(DateTimeFormatter.ofPattern("MMMM_yyyy", Locale.US)) to label
    }
    val current = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val start = current.clone() as Calendar
    val end = current.clone() as Calendar
    when (timeframe) {
        AnalyticsTimeframe.THIS_MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
        AnalyticsTimeframe.LAST_MONTH -> {
            start.add(Calendar.MONTH, -1)
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.add(Calendar.MONTH, -1)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        AnalyticsTimeframe.LAST_3_MONTHS -> start.add(Calendar.MONTH, -3)
        AnalyticsTimeframe.YEAR_TO_DATE -> start.set(Calendar.DAY_OF_YEAR, 1)
        AnalyticsTimeframe.SPECIFIC_MONTH -> error("Handled above")
        AnalyticsTimeframe.ALL_TIME -> return "All_Time" to "All Time"
    }
    val startMonth = start.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "Report"
    val endMonth = end.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: "Report"
    val startYear = start.get(Calendar.YEAR)
    val endYear = end.get(Calendar.YEAR)
    return when (timeframe) {
        AnalyticsTimeframe.THIS_MONTH, AnalyticsTimeframe.LAST_MONTH -> {
            val month = start.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Report"
            "${month}_$startYear" to "$month $startYear"
        }
        AnalyticsTimeframe.LAST_3_MONTHS -> {
            val startDay = start.get(Calendar.DAY_OF_MONTH)
            val endDay = end.get(Calendar.DAY_OF_MONTH)
            val name = if (startYear == endYear) {
                "${startMonth}_${startDay}-${endMonth}_${endDay}_$endYear"
            } else {
                "${startMonth}_${startDay}_${startYear}-${endMonth}_${endDay}_$endYear"
            }
            val label = if (startYear == endYear) {
                "$startMonth $startDay – $endMonth $endDay, $endYear"
            } else {
                "$startMonth $startDay, $startYear – $endMonth $endDay, $endYear"
            }
            name to label
        }
        AnalyticsTimeframe.YEAR_TO_DATE -> "January-${endMonth}_$endYear" to "January–$endMonth $endYear"
        AnalyticsTimeframe.SPECIFIC_MONTH -> error("Handled above")
        AnalyticsTimeframe.ALL_TIME -> "All_Time" to "All Time"
    }
}

private fun buildPaymentModeSummaries(txns: List<TransactionWithDetails>): List<PaymentModeSpendSummary> {
    val totalMyShare = txns.sumOf { it.transaction.myShareAmount }
    return txns.groupBy { it.transaction.paymentModeId }.map { (modeId, items) ->
        val mode = items.firstOrNull()?.paymentMode
        val myShare = items.sumOf { it.transaction.myShareAmount }
        val original = items.sumOf { it.transaction.originalAmount }
        PaymentModeSpendSummary(
            paymentModeId = modeId,
            paymentModeName = mode?.name ?: "Unknown",
            paymentModeType = mode?.type,
            totalMyShareSpent = myShare,
            totalOriginalSpent = original,
            totalSplitOwed = (original - myShare).coerceAtLeast(0.0),
            count = items.size,
            percentageOfTotal = if (totalMyShare > 0.0) myShare / totalMyShare * 100.0 else 0.0
        )
    }.sortedByDescending { it.totalOriginalSpent }
}
