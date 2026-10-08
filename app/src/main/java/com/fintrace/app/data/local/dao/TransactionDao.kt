package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fintrace.app.data.local.resolveCategoryId
import com.fintrace.app.data.local.entity.TransactionEntity
import com.fintrace.app.data.local.entity.TransactionSplitEntity
import com.fintrace.app.data.local.relation.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

data class CategoryAggregateRaw(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalMyShare: Double,
    val totalOriginal: Double,
    val count: Int
)

@Dao
interface TransactionDao {

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = 'CONFIRMED' ORDER BY timestamp DESC")
    fun getConfirmedTransactionsFlow(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingTransactionsFlow(): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE status = 'DISMISSED' ORDER BY timestamp DESC")
    fun getDismissedTransactionsFlow(): Flow<List<TransactionWithDetails>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'CONFIRMED' 
          AND timestamp >= :startTimestamp 
          AND timestamp <= :endTimestamp 
        ORDER BY timestamp DESC
    """)
    fun getTransactionsInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<TransactionWithDetails>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionWithDetailsById(id: Long): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, id ASC")
    suspend fun getCategoriesForResolution(): List<com.fintrace.app.data.local.entity.CategoryEntity>

    @Query("SELECT categoryId FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getStoredCategoryId(transactionId: Long): Long?

    private suspend fun normalizedCategoryId(transaction: TransactionEntity): Long {
        val storedCategoryId = transaction.id.takeIf { it > 0L }?.let { getStoredCategoryId(it) }
        return resolveCategoryId(
            requestedId = transaction.categoryId,
            currentStoredId = storedCategoryId,
            categories = getCategoriesForResolution()
        )
            ?: throw IllegalStateException("At least one category is required to save a transaction.")
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionIgnoringConflict(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplitEntity>)

    @Query("DELETE FROM transaction_splits WHERE transaction_id = :transactionId")
    suspend fun deleteSplitsForTransaction(transactionId: Long)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("""
        SELECT COALESCE(SUM(my_share_amount), 0.0) 
        FROM transactions 
        WHERE status = 'CONFIRMED' 
          AND type = 'EXPENSE' 
          AND timestamp >= :startTimestamp 
          AND timestamp <= :endTimestamp
    """)
    fun getTotalMyShareSpentInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(original_amount), 0.0) 
        FROM transactions 
        WHERE status = 'CONFIRMED' 
          AND type = 'EXPENSE' 
          AND timestamp >= :startTimestamp 
          AND timestamp <= :endTimestamp
    """)
    fun getTotalOriginalSpentInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(my_share_amount), 0.0)
        FROM transactions
        WHERE status = 'CONFIRMED'
          AND type = 'INCOME'
          AND timestamp >= :startTimestamp
          AND timestamp <= :endTimestamp
    """)
    fun getTotalConfirmedIncomeInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(my_share_amount), 0.0)
        FROM transactions
        WHERE status = 'CONFIRMED'
          AND type = 'INCOME'
          AND timestamp >= :startTimestamp
          AND timestamp <= :endTimestamp
    """)
    suspend fun getTotalConfirmedIncomeInRange(startTimestamp: Long, endTimestamp: Long): Double

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            c.colorHex AS colorHex,
            c.iconName AS iconName,
            COALESCE(SUM(t.my_share_amount), 0.0) AS totalMyShare,
            COALESCE(SUM(t.original_amount), 0.0) AS totalOriginal,
            COUNT(t.id) AS count
        FROM categories c
        LEFT JOIN transactions t 
            ON t.categoryId = c.id 
            AND t.status = 'CONFIRMED' 
            AND t.type = 'EXPENSE'
            AND t.timestamp >= :startTimestamp 
            AND t.timestamp <= :endTimestamp
        GROUP BY c.id
        HAVING totalMyShare > 0
        ORDER BY totalMyShare DESC
    """)
    fun getCategoryAggregatesInRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<CategoryAggregateRaw>>

    @Query("SELECT * FROM transactions WHERE smsRawBody = :smsBody LIMIT 1")
    suspend fun getTransactionBySmsBody(smsBody: String): TransactionEntity?

    @Query(
        "SELECT * FROM transactions WHERE smsRawBody = :body AND smsSender IS :sender " +
            "AND (sms_source_timestamp = :sourceTimestamp OR " +
            "(sms_source_timestamp IS NULL AND " +
            "(timestamp = :sourceTimestamp OR timestamp = :receivedTimestamp))) LIMIT 1"
    )
    suspend fun getTransactionBySmsIdentity(
        body: String,
        sender: String?,
        sourceTimestamp: Long,
        receivedTimestamp: Long
    ): TransactionEntity?

    @Transaction
    suspend fun insertPendingSmsIfNew(
        transaction: TransactionEntity,
        body: String,
        sender: String?,
        sourceTimestamp: Long,
        receivedTimestamp: Long
    ): Boolean {
        if (getTransactionBySmsIdentity(body, sender, sourceTimestamp, receivedTimestamp) != null) return false
        insertTransaction(transaction.copy(categoryId = normalizedCategoryId(transaction)))
        return true
    }

    @Query("SELECT * FROM transactions WHERE smsRawBody IS NOT NULL AND smsRawBody <> ''")
    suspend fun getAllTransactionsWithSmsBody(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND smsRawBody IS NOT NULL AND smsRawBody <> ''")
    suspend fun getPendingTransactionsWithSmsBody(): List<TransactionEntity>

    @Query("SELECT * FROM transaction_splits WHERE transaction_id = :transactionId ORDER BY id")
    suspend fun getSplitsForTransaction(transactionId: Long): List<TransactionSplitEntity>

    @Query(
        """
        SELECT t.* FROM transactions t
        WHERE t.is_recurring = 1
          AND t.recurring_series_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM transactions newer
              WHERE newer.is_recurring = 1
                AND newer.recurring_series_id = t.recurring_series_id
                AND newer.timestamp > t.timestamp
          )
        """
    )
    suspend fun getLatestRecurringTransactions(): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE recurring_series_id = :seriesId " +
            "AND timestamp > :timestamp ORDER BY timestamp"
    )
    suspend fun getRecurringTransactionsAfter(seriesId: String, timestamp: Long): List<TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE recurring_series_id = :seriesId " +
            "AND recurring_month = :month LIMIT 1"
    )
    suspend fun getRecurringTransactionForMonth(seriesId: String, month: String): TransactionEntity?

    @Query(
        "UPDATE transactions SET is_recurring = 0, recurring_series_id = NULL, recurring_month = NULL, " +
            "recurring_day_of_month = NULL " +
            "WHERE recurring_series_id = :seriesId"
    )
    suspend fun clearRecurringSeries(seriesId: String)

    @Transaction
    suspend fun insertRecurringTransactionWithSplits(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long {
        val transactionId = insertTransactionIgnoringConflict(
            transaction.copy(categoryId = normalizedCategoryId(transaction))
        )
        if (transactionId == -1L) return -1L
        if (splits.isNotEmpty()) {
            insertSplits(splits.map { it.copy(id = 0L, transactionId = transactionId) })
        }
        return transactionId
    }

    @Transaction
    suspend fun saveTransactionWithSplits(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long {
        val normalizedTransaction = transaction.copy(categoryId = normalizedCategoryId(transaction))
        val transactionId = if (normalizedTransaction.id == 0L) {
            insertTransaction(normalizedTransaction)
        } else {
            updateTransaction(normalizedTransaction)
            deleteSplitsForTransaction(normalizedTransaction.id)
            normalizedTransaction.id
        }
        val splitsToInsert = splits.map { it.copy(transactionId = transactionId) }
        if (splitsToInsert.isNotEmpty()) {
            insertSplits(splitsToInsert)
        }
        return transactionId
    }

    @Transaction
    suspend fun updateTransactionPreservingCategory(transaction: TransactionEntity) {
        updateTransaction(transaction.copy(categoryId = normalizedCategoryId(transaction)))
    }
}
