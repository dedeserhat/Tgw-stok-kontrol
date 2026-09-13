package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.StockCountItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockCountItemDao {
    @Query("SELECT * FROM stock_count_items WHERE stockCountId = :stockCountId")
    fun observeForCount(stockCountId: Long): Flow<List<StockCountItemEntity>>

    @Query("SELECT * FROM stock_count_items WHERE stockCountId = :stockCountId")
    suspend fun getForCount(stockCountId: Long): List<StockCountItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StockCountItemEntity>)

    @Update
    suspend fun update(item: StockCountItemEntity)

    @Query("UPDATE stock_count_items SET countedQuantity = :countedQuantity WHERE id = :id")
    suspend fun updateCountedQuantity(id: Long, countedQuantity: Double?)

    @Query("SELECT * FROM stock_count_items WHERE id = :id")
    suspend fun getById(id: Long): StockCountItemEntity?

    @Query("DELETE FROM stock_count_items WHERE isSample = 1")
    suspend fun deleteSample()
}
