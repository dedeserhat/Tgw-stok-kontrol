package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getForProduct(productId: Long, limit: Int): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentByType(type: String, limit: Int): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE type = :type ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByType(type: String, limit: Int): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    suspend fun getBetween(from: Long, to: Long): List<StockMovementEntity>

    @Query("SELECT * FROM stock_movements")
    suspend fun getAll(): List<StockMovementEntity>

    @Insert
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("DELETE FROM stock_movements WHERE isSample = 1")
    suspend fun deleteSample()
}
