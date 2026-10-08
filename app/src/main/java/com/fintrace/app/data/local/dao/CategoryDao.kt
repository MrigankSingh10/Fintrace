package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fintrace.app.data.local.fallbackCategoryId
import com.fintrace.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM categories WHERE id <> :excludedId ORDER BY displayOrder ASC, id ASC")
    suspend fun getCategoriesExcept(excludedId: Long): List<CategoryEntity>

    @Query("UPDATE transactions SET categoryId = :replacementId WHERE categoryId = :deletedId")
    suspend fun reassignTransactions(deletedId: Long, replacementId: Long): Int

    @Transaction
    suspend fun deleteCategoryAndReassign(categoryId: Long): CategoryDeleteResult {
        val source = getCategoryById(categoryId) ?: return CategoryDeleteResult.ALREADY_ABSENT
        if (getCategoryCount() <= 1) return CategoryDeleteResult.LAST_CATEGORY

        val remaining = getCategoriesExcept(source.id)
        val replacementId = fallbackCategoryId(remaining)
            ?: return CategoryDeleteResult.LAST_CATEGORY

        reassignTransactions(source.id, replacementId)
        deleteCategory(source)
        return CategoryDeleteResult.DELETED
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

enum class CategoryDeleteResult { DELETED, ALREADY_ABSENT, LAST_CATEGORY }
