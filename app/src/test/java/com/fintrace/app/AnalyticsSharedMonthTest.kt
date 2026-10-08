package com.fintrace.app

import com.fintrace.app.data.local.relation.CategorySpendSummary
import com.fintrace.app.data.repository.FinanceRepository
import com.fintrace.app.ui.analytics.AnalyticsTimeframe
import com.fintrace.app.ui.analytics.AnalyticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsSharedMonthTest {
    @Test
    fun initialSharedMonthSelectsSpecificMonthThenLaterSyncPreservesUserTimeframe() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = AnalyticsViewModel(unusedRepository())
            val initialMonth = YearMonth.of(2025, 11)

            viewModel.onSharedMonthPresented(initialMonth)

            assertEquals(initialMonth, viewModel.selectedMonth.value)
            assertEquals(AnalyticsTimeframe.SPECIFIC_MONTH, viewModel.selectedTimeframe.value)

            viewModel.onTimeframeSelected(AnalyticsTimeframe.LAST_3_MONTHS)
            val laterSharedMonth = YearMonth.of(2026, 2)
            viewModel.onSharedMonthPresented(laterSharedMonth)

            assertEquals(laterSharedMonth, viewModel.selectedMonth.value)
            assertEquals(AnalyticsTimeframe.LAST_3_MONTHS, viewModel.selectedTimeframe.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sharedMonthSyncClearsCategoryOnlyWhenMonthChanges() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val viewModel = AnalyticsViewModel(unusedRepository())
            val month = YearMonth.of(2025, 11)
            val category = CategorySpendSummary(
                categoryId = 7,
                categoryName = "Food",
                colorHex = "#008877",
                iconName = "Restaurant",
                totalMyShareSpent = 20.0,
                totalOriginalSpent = 20.0,
                transactionCount = 1
            )
            viewModel.onSharedMonthPresented(month)
            viewModel.onCategorySelected(category)

            viewModel.onSharedMonthPresented(month)
            assertEquals(category, viewModel.selectedCategory.value)

            viewModel.onSharedMonthPresented(month.plusMonths(1))
            assertNull(viewModel.selectedCategory.value)
            assertEquals(AnalyticsTimeframe.SPECIFIC_MONTH, viewModel.selectedTimeframe.value)

            viewModel.onTimeframeSelected(AnalyticsTimeframe.ALL_TIME)
            viewModel.onCategorySelected(category)
            viewModel.onSharedMonthPresented(month.plusMonths(2))

            assertNull(viewModel.selectedCategory.value)
            assertEquals(AnalyticsTimeframe.ALL_TIME, viewModel.selectedTimeframe.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun unusedRepository(): FinanceRepository = Proxy.newProxyInstance(
        FinanceRepository::class.java.classLoader,
        arrayOf(FinanceRepository::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "toString" -> "unused FinanceRepository test double"
            "hashCode" -> 0
            "equals" -> false
            else -> error("Repository should not be accessed by shared-month state tests: ${method.name}")
        }
    } as FinanceRepository
}
