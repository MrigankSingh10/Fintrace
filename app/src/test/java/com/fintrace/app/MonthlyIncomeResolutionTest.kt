package com.fintrace.app

import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.repository.additiveAdjustment
import com.fintrace.app.data.repository.isIncomeDerived
import com.fintrace.app.data.repository.overrideAdjustment
import com.fintrace.app.data.repository.resolveMonthlyIncome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Accounting rules for effective monthly income:
 *
 * CONFIRMED INCOME TRANSACTIONS + PERSISTENT MANUAL ADJUSTMENT = MONTHLY INCOME
 *
 * Confirmed income is never filtered by wording - a confirmed salary SMS counts exactly like a
 * refund, a dividend or a manually created income row.
 */
class MonthlyIncomeResolutionTest {

    /** A stored monthly budget row. [adjustment] is what `salary_amount` holds. */
    private fun budget(
        adjustment: Double,
        salaryMode: SalaryMode = SalaryMode.OVERRIDE,
        monthYear: String = "2026-09"
    ) = MonthlyBudgetAdjustmentEntity(
        monthYear = monthYear,
        salaryAmount = adjustment,
        salaryMode = salaryMode
    )

    @Test
    fun caseA_confirmedIncomeOnly() {
        assertEquals(100000.0, resolveMonthlyIncome(100000.0, null), 0.001)
    }

    @Test
    fun caseB_confirmedSalarySmsPlusAdjustment_isNotSuppressed() {
        // Manual adjustment 100000 + a confirmed salary credit SMS of 100000. The user confirmed
        // the SMS, so it counts; the app must not infer that it duplicates the manual amount.
        val income = resolveMonthlyIncome(100000.0, budget(100000.0))

        assertEquals(200000.0, income, 0.001)
    }

    @Test
    fun caseC_multipleConfirmedIncomeSources_allCount() {
        // salary SMS 100000 + generic credit 20000 + manual income transaction 5000, no adjustment
        val income = resolveMonthlyIncome(125000.0, budget(0.0))

        assertEquals(125000.0, income, 0.001)
    }

    @Test
    fun caseD_overrideRecalculatesAdjustment() {
        val adjustment = overrideAdjustment(targetIncome = 120000.0, confirmedIncome = 105000.0)

        assertEquals(15000.0, adjustment, 0.001)
        assertEquals(120000.0, resolveMonthlyIncome(105000.0, budget(adjustment)), 0.001)
    }

    @Test
    fun caseE_futureIncomeAfterOverrideKeepsAccumulating() {
        val adjustment = overrideAdjustment(targetIncome = 120000.0, confirmedIncome = 105000.0)
        val atOverride = resolveMonthlyIncome(105000.0, budget(adjustment))
        val afterNewIncome = resolveMonthlyIncome(115000.0, budget(adjustment))

        assertEquals(120000.0, atOverride, 0.001)
        assertEquals(130000.0, afterNewIncome, 0.001)
    }

    @Test
    fun caseF_addIncrementsAdjustment() {
        val adjustment = additiveAdjustment(existingAdjustment = 0.0, amountToAdd = 20000.0)

        assertEquals(20000.0, adjustment, 0.001)
        assertEquals(120000.0, resolveMonthlyIncome(100000.0, budget(adjustment)), 0.001)
    }

    @Test
    fun caseG_futureIncomeAfterAddKeepsAccumulating() {
        val adjustment = additiveAdjustment(existingAdjustment = 0.0, amountToAdd = 20000.0)

        assertEquals(125000.0, resolveMonthlyIncome(105000.0, budget(adjustment)), 0.001)
    }

    @Test
    fun caseH_repeatedAddAccumulatesOnTheAdjustment() {
        val afterFirstAdd = additiveAdjustment(existingAdjustment = 10000.0, amountToAdd = 20000.0)
        val afterSecondAdd = additiveAdjustment(existingAdjustment = afterFirstAdd, amountToAdd = 20000.0)

        assertEquals(30000.0, afterFirstAdd, 0.001)
        assertEquals(50000.0, afterSecondAdd, 0.001)
        assertEquals(130000.0, resolveMonthlyIncome(100000.0, budget(afterFirstAdd)), 0.001)
    }

    @Test
    fun caseI_downwardOverrideProducesNegativeAdjustment() {
        val adjustment = overrideAdjustment(targetIncome = 80000.0, confirmedIncome = 100000.0)

        assertEquals(-20000.0, adjustment, 0.001)
        assertEquals(80000.0, resolveMonthlyIncome(100000.0, budget(adjustment)), 0.001)
        // Future income keeps accumulating on top of the negative adjustment.
        assertEquals(90000.0, resolveMonthlyIncome(110000.0, budget(adjustment)), 0.001)
    }

    @Test
    fun caseJ_migratedRowDisplaysTheSameTotalAsBefore() {
        // Legacy stored total 100000 with 5000 of confirmed income becomes a 95000 adjustment.
        val adjustment = 95000.0

        assertEquals(100000.0, resolveMonthlyIncome(5000.0, budget(adjustment)), 0.001)
    }

    @Test
    fun caseK_isIncomeDerivedTruthTable() {
        assertTrue("no row + income is fully derived", isIncomeDerived(100000.0, null))
        assertFalse("a configured month is never derived", isIncomeDerived(100000.0, budget(0.0)))
        assertFalse("no income means nothing is derived", isIncomeDerived(0.0, null))
        assertFalse("no income means nothing is derived", isIncomeDerived(0.0, budget(50000.0)))
    }

    @Test
    fun caseL_zeroAdjustmentStillCountsAsManuallyConfigured() {
        val row = budget(0.0)

        assertEquals(100000.0, resolveMonthlyIncome(100000.0, row), 0.001)
        assertFalse(
            "an explicit override equal to confirmed income is still a manual configuration",
            isIncomeDerived(100000.0, row)
        )
    }

    @Test
    fun caseM_removedIncomeChangesTotalWithoutTouchingTheAdjustment() {
        val adjustment = 20000.0

        val beforeDismissal = resolveMonthlyIncome(100000.0, budget(adjustment))
        // A 10000 confirmed income row is dismissed or deleted: confirmed income drops, and the
        // stored adjustment must be left exactly as it was.
        val afterDismissal = resolveMonthlyIncome(90000.0, budget(adjustment))

        assertEquals(120000.0, beforeDismissal, 0.001)
        assertEquals(110000.0, afterDismissal, 0.001)
        assertEquals(20000.0, budget(adjustment).salaryAmount, 0.001)
    }

    @Test
    fun mixedIncomeWithAdjustmentMatchesTheProductExample() {
        // salary SMS 100000 + generic UPI credit 95218 + dividend 500 + manual income 5000 = 200718
        val income = resolveMonthlyIncome(200718.0, budget(20000.0))

        assertEquals(220718.0, income, 0.001)
    }
}
