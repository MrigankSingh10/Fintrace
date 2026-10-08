package com.fintrace.app.data.repository

import com.fintrace.app.data.local.entity.MonthlyBudgetAdjustmentEntity
import com.fintrace.app.data.model.SalaryMode

/**
 * Pure accounting rules for monthly income. Kept free of Android, Room and Flow types so the
 * whole model is unit testable.
 *
 * The single invariant:
 *
 * ```
 * CONFIRMED INCOME TRANSACTIONS + PERSISTENT MANUAL ADJUSTMENT = MONTHLY INCOME
 * ```
 *
 * Confirmed income transactions are never inspected for wording: a confirmed salary credit, a
 * refund, a dividend, a generic UPI credit and a manually created income row all count the same
 * way. A user who does not want a transaction counted dismisses it.
 *
 * The manual adjustment lives in the legacy `monthly_budgets.salary_amount` column (see
 * [MonthlyBudgetAdjustmentEntity.salaryAmount]). [SalaryMode] only decides how the user changed that
 * adjustment, never how transactions are counted.
 */

/** Effective monthly income shown on the dashboard and used as the category percentage denominator. */
internal fun resolveMonthlyIncome(
    confirmedIncome: Double,
    budget: MonthlyBudgetAdjustmentEntity?
): Double =
    confirmedIncome + (budget?.salaryAmount ?: 0.0)

/**
 * Descriptive only: true when the displayed total comes entirely from confirmed income and the
 * month has never been manually configured. A row with a zero adjustment still counts as manual
 * configuration - the user explicitly set the month, they just landed on the same number.
 *
 * Never use this to decide whether income can be edited.
 */
internal fun isIncomeDerived(
    confirmedIncome: Double,
    budget: MonthlyBudgetAdjustmentEntity?
): Boolean =
    confirmedIncome > 0.0 && budget == null

/**
 * Adjustment that makes the current total exactly [targetIncome]. Future confirmed income keeps
 * accumulating on top of this adjustment, so an override is not a frozen total. May be negative.
 */
fun overrideAdjustment(
    targetIncome: Double,
    confirmedIncome: Double
): Double =
    targetIncome - confirmedIncome

/** Adjustment for an additive change. Modifies the stored adjustment instead of a snapshot total. */
fun additiveAdjustment(
    existingAdjustment: Double,
    amountToAdd: Double
): Double =
    existingAdjustment + amountToAdd

/**
 * Reinterprets a legacy stored total as a manual adjustment. Negative results are valid: a stored
 * total below the month's confirmed income means the user lowered the month.
 */
internal fun legacyTotalToAdjustment(
    legacyTotal: Double,
    confirmedIncome: Double
): Double =
    legacyTotal - confirmedIncome

/**
 * Very old `ADD_TO_SMS` rows stored only the increment, not the resulting total. Reproduces the
 * total the previous app version displayed so the adjustment ends up equal to that increment.
 */
internal fun legacyAddToSmsTotal(
    storedIncrement: Double,
    confirmedIncome: Double
): Double =
    storedIncrement + confirmedIncome

/**
 * Rows to persist when moving stored totals to the adjustment model. Empty when the migration has
 * already completed, which is what makes the conversion safe to retry: [legacyTotalToAdjustment]
 * is not idempotent, so a second pass over converted rows would subtract income again.
 *
 * @param legacyAddPassDone whether the previous `ADD_TO_SMS` normalization already ran. When it
 * has not, those rows still hold a bare increment and must be converted to a total first.
 */
internal fun incomeAdjustmentChanges(
    migrationDone: Boolean,
    legacyAddPassDone: Boolean,
    budgets: List<MonthlyBudgetAdjustmentEntity>,
    confirmedIncomeByMonth: (String) -> Double
): List<MonthlyBudgetAdjustmentEntity> {
    if (migrationDone) return emptyList()

    return budgets.map { budget ->
        val confirmedIncome = confirmedIncomeByMonth(budget.monthYear)
        val legacyTotal = if (!legacyAddPassDone && budget.salaryMode == SalaryMode.ADD_TO_SMS) {
            legacyAddToSmsTotal(budget.salaryAmount, confirmedIncome)
        } else {
            budget.salaryAmount
        }
        budget.copy(salaryAmount = legacyTotalToAdjustment(legacyTotal, confirmedIncome))
    }
}
