package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.StockBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockBatchDao {
    @Query("SELECT * FROM stock_batches WHERE productId = :productId AND quantityRemaining > 0 ORDER BY (expiryDate IS NULL), expiryDate ASC, receivedDate ASC")
    fun observeActiveForProduct(productId: Long): Flow<List<StockBatchEntity>>

    /** FEFO order: earliest expiry first, batches without an expiry go last. */
    @Query("SELECT * FROM stock_batches WHERE productId = :productId AND quantityRemaining > 0 ORDER BY (expiryDate IS NULL), expiryDate ASC, receivedDate ASC")
    suspend fun getConsumableForProduct(productId: Long): List<StockBatchEntity>

    @Query("SELECT * FROM stock_batches WHERE productId = :productId ORDER BY receivedDate DESC")
    fun observeAllForProduct(productId: Long): Flow<List<StockBatchEntity>>

    @Query(
        "SELECT * FROM stock_batches WHERE quantityRemaining > 0 AND expiryDate IS NOT NULL AND expiryDate <= :threshold ORDER BY expiryDate ASC"
    )
    suspend fun getExpiringBefore(threshold: Long): List<StockBatchEntity>

    @Query("SELECT * FROM stock_batches WHERE id = :id")
    suspend fun getById(id: Long): StockBatchEntity?

    @Query("SELECT * FROM stock_batches")
    suspend fun getAll(): List<StockBatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: StockBatchEntity): Long

    @Update
    suspend fun update(batch: StockBatchEntity)

    @Query("UPDATE stock_batches SET quantityRemaining = :remaining WHERE id = :id")
    suspend fun updateRemaining(id: Long, remaining: Double)

    @Delete
    suspend fun delete(batch: StockBatchEntity)

    @Query("DELETE FROM stock_batches WHERE isSample = 1")
    suspend fun deleteSample()
}
