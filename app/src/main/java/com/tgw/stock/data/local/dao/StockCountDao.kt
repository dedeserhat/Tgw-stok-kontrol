package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.StockCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockCountDao {
    @Query("SELECT * FROM stock_counts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<StockCountEntity>>

    @Query("SELECT * FROM stock_counts ORDER BY createdAt DESC")
    suspend fun getAll(): List<StockCountEntity>

    @Query("SELECT * FROM stock_counts WHERE status = :status AND countDate BETWEEN :from AND :to")
    suspend fun getByStatusBetween(status: String, from: Long, to: Long): List<StockCountEntity>

    @Query("SELECT * FROM stock_counts WHERE id = :id")
    fun observeById(id: Long): Flow<StockCountEntity?>

    @Query("SELECT * FROM stock_counts WHERE id = :id")
    suspend fun getById(id: Long): StockCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(count: StockCountEntity): Long

    @Update
    suspend fun update(count: StockCountEntity)

    @Delete
    suspend fun delete(count: StockCountEntity)

    @Query("DELETE FROM stock_counts WHERE isSample = 1")
    suspend fun deleteSample()
}
