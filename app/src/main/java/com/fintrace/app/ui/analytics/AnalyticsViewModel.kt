package com.fintrace.app.ui.analytics

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrace.app.data.model.PaymentModeType
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

enum class AnalyticsTimeframe(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    YEAR_TO_DATE("Year to Date"),
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
    private val repository: FinanceRepository
) : ViewModel() {

    private val _selectedTimeframe = MutableStateFlow(AnalyticsTimeframe.THIS_MONTH)
    val selectedTimeframe = _selectedTimeframe.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategorySpendSummary?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val periodTimestamps: StateFlow<Pair<Long, Long>> = _selectedTimeframe.map {
        computeTimeframeRange(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = computeTimeframeRange(AnalyticsTimeframe.THIS_MONTH)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryBreakdown: StateFlow<List<CategorySpendSummary>> = periodTimestamps.flatMapLatest { (start, end) ->
        repository.getCategoryBreakdown(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionWithDetails>> = periodTimestamps.flatMapLatest { (start, end) ->
        repository.getTransactionsForRange(start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paymentModeBreakdown: StateFlow<List<PaymentModeSpendSummary>> = combine(
        transactions,
        categoryBreakdown
    ) { txns, _ ->
        val totalMyShare = txns.sumOf { it.transaction.myShareAmount }
        val grouped = txns.groupBy { it.transaction.paymentModeId }

        grouped.map { (modeId, items) ->
            val mode = items.firstOrNull()?.paymentMode
            val modeName = mode?.name ?: "Unknown"
            val modeType = mode?.type
            val myShare = items.sumOf { it.transaction.myShareAmount }
            val original = items.sumOf { it.transaction.originalAmount }
            val splitOwed = (original - myShare).coerceAtLeast(0.0)
            val pct = if (totalMyShare > 0) (myShare / totalMyShare) * 100.0 else 0.0

            PaymentModeSpendSummary(
                paymentModeId = modeId,
                paymentModeName = modeName,
                paymentModeType = modeType,
                totalMyShareSpent = myShare,
                totalOriginalSpent = original,
                totalSplitOwed = splitOwed,
                count = items.size,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.totalOriginalSpent }
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

    fun onCategorySelected(category: CategorySpendSummary?) {
        _selectedCategory.value = if (_selectedCategory.value?.categoryId == category?.categoryId) null else category
    }

    /**
     * Default suggested filename for SAF CreateDocument.
     */
    fun defaultExportFilename(): String {
        val cal = Calendar.getInstance()
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: "Report"
        val year  = cal.get(Calendar.YEAR)
        return "Finance_Tracker_${month}_${year}.xlsx"
    }

    /**
     * Write the .xlsx file to the Uri chosen by the user via SAF CreateDocument.
     * Call this AFTER the launcher returns a non-null Uri.
     */
    fun exportToXlsx(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val txns     = transactions.value
                val cats     = categoryBreakdown.value
                val modes    = paymentModeBreakdown.value
                val label    = _selectedTimeframe.value.label

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        ExcelWorkbookGenerator.generateXlsx(
                            outputStream       = outStream,
                            transactions       = txns,
                            categoryBreakdown  = cats,
                            paymentModeBreakdown = modes,
                            timeframeLabel     = label
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Excel report saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun computeTimeframeRange(timeframe: AnalyticsTimeframe): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

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
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
