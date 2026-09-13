package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.SupplierProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierProductDao {
    @Query("SELECT * FROM supplier_products WHERE productId = :productId")
    fun observeForProduct(productId: Long): Flow<List<SupplierProductEntity>>

    @Query("SELECT * FROM supplier_products WHERE productId = :productId")
    suspend fun getForProduct(productId: Long): List<SupplierProductEntity>

    @Query("SELECT * FROM supplier_products WHERE supplierId = :supplierId")
    fun observeForSupplier(supplierId: Long): Flow<List<SupplierProductEntity>>

    @Query("SELECT * FROM supplier_products WHERE id = :id")
    suspend fun getById(id: Long): SupplierProductEntity?

    @Query("SELECT * FROM supplier_products")
    suspend fun getAll(): List<SupplierProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SupplierProductEntity): Long

    @Update
    suspend fun update(item: SupplierProductEntity)

    @Delete
    suspend fun delete(item: SupplierProductEntity)

    @Query("DELETE FROM supplier_products WHERE isSample = 1")
    suspend fun deleteSample()
}
