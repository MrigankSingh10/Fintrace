package com.fintrace.app

import com.fintrace.app.data.local.entity.MonthlyBudgetSalaryEntity
import com.fintrace.app.data.model.SalaryMode
import com.fintrace.app.data.repository.resolveMonthlySalary
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Accounting rules for the effective monthly income shown on the dashboard and used as the
 * category percentage denominator.
 */
class MonthlyIncomeResolutionTest {

    private fun budget(
        salaryAmount: Double,
        salaryMode: SalaryMode = SalaryMode.OVERRIDE
    ) = MonthlyBudgetSalaryEntity(
        monthYear = "2026-09",
        salaryAmount = salaryAmount,
        salaryMode = salaryMode
    )

    @Test
    fun caseA_noManualSalary_usesAllConfirmedIncome() {
        val income = resolveMonthlySalary(
            confirmedIncome = 95218.0,
            confirmedNonSalaryIncome = 95218.0,
            budget = null
        )

        assertEquals(95218.0, income, 0.001)
    }

    @Test
    fun caseA_noManualSalary_withSalarySms_usesAllConfirmedIncome() {
        val income = resolveMonthlySalary(
            confirmedIncome = 100000.0,
            confirmedNonSalaryIncome = 0.0,
            budget = null
        )

        assertEquals(100000.0, income, 0.001)
    }

    @Test
    fun caseB_manualSalaryWithGenericCredit_addsCreditOnTop() {
        val income = resolveMonthlySalary(
            confirmedIncome = 195218.0,
            confirmedNonSalaryIncome = 95218.0,
            budget = budget(100000.0, SalaryMode.OVERRIDE)
        )

        assertEquals(195218.0, income, 0.001)
    }

    @Test
    fun caseC_manualSalaryWithSalaryCreditOnly_doesNotDoubleCount() {
        val income = resolveMonthlySalary(
            confirmedIncome = 100000.0,
            confirmedNonSalaryIncome = 0.0,
            budget = budget(100000.0, SalaryMode.OVERRIDE)
        )

        assertEquals(100000.0, income, 0.001)
    }

    @Test
    fun caseD_manualSalaryWithSalaryCreditAndUnrelatedCredit_addsOnlyUnrelated() {
        val income = resolveMonthlySalary(
            confirmedIncome = 120000.0,
            confirmedNonSalaryIncome = 20000.0,
            budget = budget(100000.0, SalaryMode.OVERRIDE)
        )

        assertEquals(120000.0, income, 0.001)
    }

    @Test
    fun caseE_addToSmsMode_keepsStoredTotal() {
        val income = resolveMonthlySalary(
            confirmedIncome = 100000.0,
            confirmedNonSalaryIncome = 20000.0,
            budget = budget(125000.0, SalaryMode.ADD_TO_SMS)
        )

        assertEquals(125000.0, income, 0.001)
    }

    @Test
    fun addToSmsMode_storesResolvedIncomePlusManualAmountAndIsNotRecalculated() {
        // DashboardViewModel.saveMonthlySalary persists `currentSalary + amount` for
        // ADD_TO_SMS, so the stored row already contains the SMS income for the month.
        val beforeSaving = resolveMonthlySalary(
            confirmedIncome = 100000.0,
            confirmedNonSalaryIncome = 20000.0,
            budget = null
        )
        val storedTotal = beforeSaving + 25000.0

        assertEquals(100000.0, beforeSaving, 0.001)

        val afterSaving = resolveMonthlySalary(
            confirmedIncome = 100000.0,
            confirmedNonSalaryIncome = 20000.0,
            budget = budget(storedTotal, SalaryMode.ADD_TO_SMS)
        )

        assertEquals(125000.0, afterSaving, 0.001)
    }

    @Test
    fun manualSalaryWithSalarySmsGenericUpiAndDividend_sumsToExpectedIncome() {
        // Manual salary 100000, salary SMS 100000, generic UPI credit 95218, dividend 500.
        val income = resolveMonthlySalary(
            confirmedIncome = 195718.0,
            confirmedNonSalaryIncome = 95718.0,
            budget = budget(100000.0, SalaryMode.OVERRIDE)
        )

        assertEquals(195718.0, income, 0.001)
    }

    @Test
    fun manualIncomeRowWithoutSmsBody_countsAsAdditionalIncome() {
        // Manual income rows have smsRawBody = NULL, so the aggregate includes them.
        val income = resolveMonthlySalary(
            confirmedIncome = 5000.0,
            confirmedNonSalaryIncome = 5000.0,
            budget = budget(100000.0, SalaryMode.OVERRIDE)
        )

        assertEquals(105000.0, income, 0.001)
    }
}
