package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY name")
    suspend fun getByCategory(categoryId: Long): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products WHERE isSample = 1")
    suspend fun deleteSample()

    @Query("UPDATE products SET currentStock = currentStock + :delta WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double)

    @Query("UPDATE products SET currentStock = :newStock WHERE id = :id")
    suspend fun setStock(id: Long, newStock: Double)

    @Query(
        "UPDATE products SET avgCostPerUnitMinor = :avgCostMinor, lastCostPerUnitMinor = :lastCostMinor, lastPurchaseDate = :purchaseDate WHERE id = :id"
    )
    suspend fun updateCostInfo(id: Long, avgCostMinor: Long, lastCostMinor: Long, purchaseDate: Long)

    @Query("SELECT COUNT(*) FROM products WHERE isArchived = 0")
    fun observeTotalCount(): Flow<Int>
}
