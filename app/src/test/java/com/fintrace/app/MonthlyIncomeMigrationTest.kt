package com.fintrace.app

import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.repository.incomeAdjustmentChanges
import com.fintrace.app.data.repository.legacyAddToSmsTotal
import com.fintrace.app.data.repository.legacyTotalToAdjustment
import com.fintrace.app.data.repository.resolveMonthlyIncome
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Upgrade tests for `monthly_budgets.salary_amount`.
 *
 * Before this change the column stored the final displayed total, so a stored total of 100000
 * alongside 5000 of confirmed income must become a 95000 adjustment. Both legacy passes run in a
 * single transaction because the conversion is not idempotent.
 */
class MonthlyIncomeMigrationTest {

    private val confirmedIncome = mapOf(
        "2026-07" to 100000.0,
        "2026-08" to 80000.0,
        "2026-09" to 5000.0
    )

    private fun budget(
        monthYear: String,
        storedTotal: Double,
        salaryMode: SalaryMode = SalaryMode.OVERRIDE
    ) = MonthlyBudgetAdjustmentEntity(
        monthYear = monthYear,
        salaryAmount = storedTotal,
        salaryMode = salaryMode
    )

    private fun convert(budgets: List<MonthlyBudgetAdjustmentEntity>, legacyAddPassDone: Boolean) =
        incomeAdjustmentChanges(
            migrationDone = false,
            legacyAddPassDone = legacyAddPassDone,
            budgets = budgets,
            confirmedIncomeByMonth = { confirmedIncome[it] ?: 0.0 }
        )

    @Test
    fun totalToAdjustmentArithmetic() {
        assertEquals(95000.0, legacyTotalToAdjustment(100000.0, 5000.0), 0.001)
        assertEquals(100000.0, legacyTotalToAdjustment(100000.0, 0.0), 0.001)
        // A total already below confirmed income yields a negative adjustment, which is valid.
        assertEquals(-20000.0, legacyTotalToAdjustment(80000.0, 100000.0), 0.001)
    }

    @Test
    fun legacyAddPassRebuildsIncrementOnlyRows() {
        assertEquals(
            120000.0,
            legacyAddToSmsTotal(storedIncrement = 20000.0, confirmedIncome = 100000.0),
            0.001
        )
    }

    @Test
    fun alreadyMigratedMonthKeepsItsAdjustment() {
        assertEquals(emptyList<MonthlyBudgetAdjustmentEntity>(), convert(listOf(), legacyAddPassDone = true))
    }

    @Test
    fun currentBuildRowsAreConvertedWithoutTheLegacyAddPass() {
        val changes = convert(
            budgets = listOf(
                budget("2026-07", storedTotal = 100000.0),
                budget("2026-08", storedTotal = 80000.0, salaryMode = SalaryMode.ADD_TO_SMS)
            ),
            legacyAddPassDone = true
        )

        assertEquals(2, changes.size)
        assertEquals(0.0, changes.single { it.monthYear == "2026-07" }.salaryAmount, 0.001)
        // ADD_TO_SMS already stored the total, so it is converted like any other row.
        assertEquals(0.0, changes.single { it.monthYear == "2026-08" }.salaryAmount, 0.001)
    }
    @Test
    fun upgradeState_oneOldOverrideRow() {
        val changes = convert(listOf(budget("2026-07", storedTotal = 100000.0)), legacyAddPassDone = true)

        assertEquals(listOf("2026-07"), changes.map { it.monthYear })
        assertEquals(0.0, changes.single().salaryAmount, 0.001)
    }

    @Test
    fun upgradeState_twoOldAddToSmsRows() {
        val changes = convert(
            budgets = listOf(
                budget("2026-07", storedTotal = 100000.0, salaryMode = SalaryMode.ADD_TO_SMS),
                budget("2026-08", storedTotal = 80000.0, salaryMode = SalaryMode.ADD_TO_SMS)
            ),
            legacyAddPassDone = true
        )

        assertEquals(listOf(0.0, 0.0), changes.map { it.salaryAmount })
    }

    @Test
    fun upgradeState_mixedAddAndOverrideRows() {
        val changes = convert(
            budgets = listOf(
                budget("2026-07", storedTotal = 100000.0),
                budget("2026-08", storedTotal = 80000.0, salaryMode = SalaryMode.ADD_TO_SMS)
            ),
            legacyAddPassDone = true
        )

        assertEquals(2, changes.size)
        assertEquals(SalaryMode.OVERRIDE, changes.single { it.monthYear == "2026-07" }.salaryMode)
        assertEquals(SalaryMode.ADD_TO_SMS, changes.single { it.monthYear == "2026-08" }.salaryMode)
        assertEquals(0.0, changes.single { it.monthYear == "2026-08" }.salaryAmount, 0.001)
    }

    @Test
    fun upgradeState_noStoredBudgets() {
        assertEquals(emptyList<MonthlyBudgetAdjustmentEntity>(), convert(listOf(), legacyAddPassDone = false))
    }

    @Test
    fun upgradeState_veryOldAddToSmsIncrementRows() {
        // The oldest build stored only the increment for ADD_TO_SMS, so pass A must first rebuild
        // the total: 20000 + 100000 = 120000, then pass B turns that into a 20000 adjustment.
        val changes = convert(
            budgets = listOf(
                budget("2026-07", storedTotal = 20000.0, salaryMode = SalaryMode.ADD_TO_SMS)
            ),
            legacyAddPassDone = false
        )

        val july = changes.single()
        assertEquals(20000.0, july.salaryAmount, 0.001)
        assertEquals(120000.0, resolveMonthlyIncome(100000.0, july), 0.001)
    }

    @Test
    fun upgradeState_mixedVeryOldAndCurrentRows() {
        val changes = convert(
            budgets = listOf(
                // increment-only, needs pass A
                budget("2026-07", storedTotal = 20000.0, salaryMode = SalaryMode.ADD_TO_SMS),
                // a plain OVERRIDE total, untouched by pass A
                budget("2026-08", storedTotal = 80000.0)
            ),
            legacyAddPassDone = false
        )

        assertEquals(20000.0, changes.single { it.monthYear == "2026-07" }.salaryAmount, 0.001)
        assertEquals(0.0, changes.single { it.monthYear == "2026-08" }.salaryAmount, 0.001)
    }

    @Test
    fun upgradeState_rowAlreadyBelowConfirmedIncome() {
        val changes = convert(listOf(budget("2026-07", storedTotal = 80000.0)), legacyAddPassDone = true)

        assertEquals(-20000.0, changes.single().salaryAmount, 0.001)
        assertEquals(80000.0, resolveMonthlyIncome(100000.0, changes.single()), 0.001)
    }

    @Test
    fun upgradeState_rowWithNoConfirmedIncome() {
        // 2026-06 is absent from the income map, so the user configured the month before any
        // income existed. The stored total is pure adjustment.
        val changes = convert(listOf(budget("2026-06", storedTotal = 60000.0)), legacyAddPassDone = true)

        assertEquals(60000.0, changes.single().salaryAmount, 0.001)
        assertEquals(60000.0, resolveMonthlyIncome(0.0, changes.single()), 0.001)
    }

    @Test
    fun migrationIsNotIdempotentWhichIsWhyItRunsInOneTransaction() {
        val stored = budget("2026-07", storedTotal = 100000.0)
        val once = convert(listOf(stored), legacyAddPassDone = true).single()
        val twice = convert(listOf(once), legacyAddPassDone = true).single()

        // The first run preserves the displayed total. A second run would subtract the confirmed
        // income again and zero the month out, so the completion flag is written only after the
        // transaction commits.
        assertEquals(100000.0, resolveMonthlyIncome(100000.0, once), 0.001)
        assertEquals(0.0, resolveMonthlyIncome(100000.0, twice), 0.001)
    }

    @Test
    fun migrationPreservesEveryMonthsDisplayedTotal() {
        val stored = listOf(
            budget("2026-07", storedTotal = 100000.0),
            budget("2026-08", storedTotal = 80000.0, salaryMode = SalaryMode.ADD_TO_SMS),
            budget("2026-09", storedTotal = 6000.0)
        )

        val migrated = convert(stored, legacyAddPassDone = true)

        stored.forEach { row ->
            val after = migrated.single { it.monthYear == row.monthYear }
            assertEquals(
                "displayed total changed for ${row.monthYear}",
                row.salaryAmount,
                resolveMonthlyIncome(confirmedIncome.getValue(row.monthYear), after),
                0.001
            )
        }
    }
}
