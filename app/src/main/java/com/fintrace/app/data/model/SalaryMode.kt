package com.fintrace.app.data.model

/**
 * How the user last changed the month's manual adjustment.
 *
 * Neither value affects how confirmed income transactions are counted. Both only decide how the
 * stored adjustment in `monthly_budgets.salary_amount` is updated.
 */
enum class SalaryMode {
    /** Set the current total to the entered amount, recalculated as an adjustment. */
    OVERRIDE,

    /**
     * Legacy persisted enum name. Its current user-facing/domain meaning is ADD_TO_CURRENT_INCOME.
     *
     * The name is persisted through Room, and Room's generated enum reader throws on any unknown
     * stored value, so it must not be renamed without a data migration. UI copy must always say
     * "Add to current income", never "Add to SMS".
     */
    ADD_TO_SMS
}
