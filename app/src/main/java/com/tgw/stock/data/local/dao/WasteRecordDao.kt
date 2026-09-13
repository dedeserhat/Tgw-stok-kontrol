package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.WasteRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WasteRecordDao {
    @Query("SELECT * FROM waste_records ORDER BY recordedDate DESC")
    fun observeAll(): Flow<List<WasteRecordEntity>>

    @Query("SELECT * FROM waste_records WHERE recordedDate >= :from ORDER BY recordedDate DESC")
    fun observeSince(from: Long): Flow<List<WasteRecordEntity>>

    @Query("SELECT * FROM waste_records WHERE recordedDate >= :from AND recordedDate <= :to ORDER BY recordedDate DESC")
    suspend fun getBetween(from: Long, to: Long): List<WasteRecordEntity>

    @Query("SELECT * FROM waste_records")
    suspend fun getAll(): List<WasteRecordEntity>

    @Insert
    suspend fun insert(record: WasteRecordEntity): Long

    @Query("DELETE FROM waste_records WHERE isSample = 1")
    suspend fun deleteSample()
}
