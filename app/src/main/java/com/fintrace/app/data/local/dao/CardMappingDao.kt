package com.fintrace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fintrace.app.data.local.entity.CardMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardMappingDao {

    @Query("SELECT * FROM card_mappings ORDER BY id DESC")
    fun getAllCardMappingsFlow(): Flow<List<CardMappingEntity>>

    @Query("SELECT * FROM card_mappings ORDER BY id DESC")
    suspend fun getAllCardMappings(): List<CardMappingEntity>

    @Query("SELECT * FROM card_mappings WHERE cardLastFour = :lastFour LIMIT 1")
    suspend fun getCardMappingByLastFour(lastFour: String): CardMappingEntity?

    @Query("SELECT * FROM card_mappings WHERE id = :id LIMIT 1")
    suspend fun getCardMappingById(id: Long): CardMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardMapping(cardMapping: CardMappingEntity): Long

    @Update
    suspend fun updateCardMapping(cardMapping: CardMappingEntity)

    @Delete
    suspend fun deleteCardMapping(cardMapping: CardMappingEntity)

    @Query("DELETE FROM card_mappings WHERE cardLastFour = :lastFour")
    suspend fun deleteCardMappingByLastFour(lastFour: String)

    @Query("SELECT COUNT(*) FROM card_mappings")
    suspend fun getCardMappingCount(): Int
}
