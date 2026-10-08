package com.fintrace.app.data.local

import com.fintrace.app.data.local.entity.CategoryEntity

/** Shared deterministic choice used when a stored category reference is no longer valid. */
internal fun fallbackCategoryId(categories: List<CategoryEntity>): Long? =
    categories.firstOrNull { it.name.equals("Others", ignoreCase = true) }?.id
        ?: categories.minWithOrNull(compareBy<CategoryEntity> { it.displayOrder }.thenBy { it.id })?.id

/** Valid explicit choices win; stale forms retain the persisted category when it is still valid. */
internal fun resolveCategoryId(
    requestedId: Long,
    currentStoredId: Long?,
    categories: List<CategoryEntity>
): Long? {
    val ids = categories.asSequence().map { it.id }.toSet()
    return requestedId.takeIf(ids::contains)
        ?: currentStoredId?.takeIf(ids::contains)
        ?: fallbackCategoryId(categories)
}
