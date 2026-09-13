package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.SupplierPriceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierPriceDao {
    @Query("SELECT * FROM supplier_prices WHERE supplierProductId = :supplierProductId ORDER BY effectiveDate DESC")
    fun observeHistory(supplierProductId: Long): Flow<List<SupplierPriceEntity>>

    @Query("SELECT * FROM supplier_prices WHERE supplierProductId = :supplierProductId ORDER BY effectiveDate DESC")
    suspend fun getHistory(supplierProductId: Long): List<SupplierPriceEntity>

    @Query("SELECT * FROM supplier_prices WHERE supplierProductId = :supplierProductId ORDER BY effectiveDate DESC LIMIT 1")
    suspend fun getLatest(supplierProductId: Long): SupplierPriceEntity?

    @Query("SELECT * FROM supplier_prices")
    suspend fun getAll(): List<SupplierPriceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: SupplierPriceEntity): Long

    @Query("DELETE FROM supplier_prices WHERE isSample = 1")
    suspend fun deleteSample()
}
