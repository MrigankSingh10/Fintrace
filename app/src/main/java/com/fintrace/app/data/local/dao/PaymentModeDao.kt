package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fintrace.app.data.local.entity.PaymentModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentModeDao {
    @Query("SELECT * FROM payment_modes ORDER BY name ASC")
    fun getAllPaymentModesFlow(): Flow<List<PaymentModeEntity>>

    @Query("SELECT * FROM payment_modes ORDER BY name ASC")
    suspend fun getAllPaymentModes(): List<PaymentModeEntity>

    @Query("SELECT * FROM payment_modes WHERE id = :id LIMIT 1")
    suspend fun getPaymentModeById(id: Long): PaymentModeEntity?

    @Query("SELECT * FROM payment_modes WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getPaymentModeByName(name: String): PaymentModeEntity?

    @Query("SELECT COUNT(*) FROM payment_modes")
    suspend fun getPaymentModeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMode(mode: PaymentModeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentModes(modes: List<PaymentModeEntity>): List<Long>

    @Update
    suspend fun updatePaymentMode(mode: PaymentModeEntity)

    @Delete
    suspend fun deletePaymentMode(mode: PaymentModeEntity)
}
