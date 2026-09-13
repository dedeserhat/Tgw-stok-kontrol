package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.PurchaseOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderDao {
    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    suspend fun getAll(): List<PurchaseOrderEntity>

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    fun observeById(id: Long): Flow<PurchaseOrderEntity?>

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    suspend fun getById(id: Long): PurchaseOrderEntity?

    @Query("SELECT COUNT(*) FROM purchase_orders")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: PurchaseOrderEntity): Long

    @Update
    suspend fun update(order: PurchaseOrderEntity)

    @Delete
    suspend fun delete(order: PurchaseOrderEntity)

    @Query("DELETE FROM purchase_orders WHERE isSample = 1")
    suspend fun deleteSample()
}
