package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

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

    @Query("SELECT * FROM transactions WHERE smsRawBody IS NOT NULL AND smsRawBody <> ''")
    suspend fun getAllTransactionsWithSmsBody(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND smsRawBody IS NOT NULL AND smsRawBody <> ''")
    suspend fun getPendingTransactionsWithSmsBody(): List<TransactionEntity>

    @Transaction
    suspend fun saveTransactionWithSplits(
        transaction: TransactionEntity,
        splits: List<TransactionSplitEntity>
    ): Long {
        val transactionId = if (transaction.id == 0L) {
            insertTransaction(transaction)
        } else {
            updateTransaction(transaction)
            deleteSplitsForTransaction(transaction.id)
            transaction.id
        }
        val splitsToInsert = splits.map { it.copy(transactionId = transactionId) }
        if (splitsToInsert.isNotEmpty()) {
            insertSplits(splitsToInsert)
        }
        return transactionId
    }
}
