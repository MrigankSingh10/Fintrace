package com.fintrace.app

import com.fintrace.app.data.local.fallbackCategoryId
import com.fintrace.app.data.local.resolveCategoryId
import com.fintrace.app.data.local.entity.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryPoliciesTest {

    @Test
    fun deletionReplacementPrefersOthersEvenWhenItIsNotFirst() {
        // These are the remaining seeded categories after default category id=1 is removed.
        val remaining = listOf(
            category(2, "Wants", order = 2),
            category(3, "Investments", order = 3),
            category(4, "Hospital", order = 4),
            category(5, "Others", order = 5)
        )

        assertEquals(5L, fallbackCategoryId(remaining))
    }

    @Test
    fun replacementAfterCategoryIdOneWasAlreadyDeletedIsStable() {
        val remaining = listOf(category(4, "Transport", order = 1), category(8, "Bills", order = 1))

        assertEquals(4L, fallbackCategoryId(remaining))
    }

    @Test
    fun existingTransactionKeepsItsCurrentValidCategoryAgainstDeletedSelection() {
        val categories = listOf(category(2, "Food"), category(5, "Travel"))

        assertEquals(5L, resolveCategoryId(requestedId = 99, currentStoredId = 5, categories = categories))
    }

    @Test
    fun validNewSelectionWinsButStaleSelectionUsesSafeFallback() {
        val categories = listOf(category(2, "Food", order = 0), category(3, "Others", order = 1))

        assertEquals(2L, resolveCategoryId(requestedId = 2, currentStoredId = null, categories = categories))
        assertEquals(3L, resolveCategoryId(requestedId = 99, currentStoredId = null, categories = categories))
        assertEquals(2L, resolveCategoryId(requestedId = 2, currentStoredId = 3, categories = categories))
    }

    @Test
    fun emptyRemainingCategoriesCannotProvideADeleteReplacement() {
        assertNull(fallbackCategoryId(emptyList()))
    }

    private fun category(id: Long, name: String, order: Int = id.toInt()) = CategoryEntity(
        id = id,
        name = name,
        colorHex = "#000000",
        iconName = "MoreHoriz",
        isDefault = id == 1L,
        displayOrder = order
    )
}
