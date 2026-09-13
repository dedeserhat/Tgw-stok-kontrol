package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.PurchaseOrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderItemDao {
    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    fun observeForOrder(orderId: Long): Flow<List<PurchaseOrderItemEntity>>

    @Query("SELECT * FROM purchase_order_items WHERE purchaseOrderId = :orderId")
    suspend fun getForOrder(orderId: Long): List<PurchaseOrderItemEntity>

    @Query("SELECT * FROM purchase_order_items")
    suspend fun getAll(): List<PurchaseOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PurchaseOrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseOrderItemEntity>)

    @Update
    suspend fun update(item: PurchaseOrderItemEntity)

    @Query("UPDATE purchase_order_items SET receivedQuantity = :receivedQuantity WHERE id = :id")
    suspend fun updateReceived(id: Long, receivedQuantity: Double)

    @Delete
    suspend fun delete(item: PurchaseOrderItemEntity)

    @Query("DELETE FROM purchase_order_items WHERE isSample = 1")
    suspend fun deleteSample()
}
