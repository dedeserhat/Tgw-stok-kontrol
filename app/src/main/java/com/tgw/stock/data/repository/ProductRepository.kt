package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeAll(): Flow<List<ProductEntity>>
    fun observeById(id: Long): Flow<ProductEntity?>
    suspend fun getAll(): List<ProductEntity>

    /** Same as [getAll] but also returns archived products - use this when building a
     * name/unit lookup map over historical rows (reports, CSV export, old purchase
     * orders/recipes/waste) so an archived product's past records still resolve to a
     * real name instead of silently disappearing. */
    suspend fun getAllIncludingArchived(): List<ProductEntity>
    suspend fun getById(id: Long): ProductEntity?
    suspend fun getByBarcode(barcode: String): ProductEntity?
    suspend fun upsert(product: ProductEntity): Long

    /** Hides the product from every list/picker instead of deleting its row, since a hard
     * delete would cascade and destroy its entire stock_movements/waste_records/
     * purchase_order_items history - silently skewing every past Reports figure that
     * touched it. */
    suspend fun archive(productId: Long)
    fun observeTotalCount(): Flow<Int>
}

class RoomProductRepository(private val dao: ProductDao) : ProductRepository {
    override fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()
    override fun observeById(id: Long): Flow<ProductEntity?> = dao.observeById(id)
    override suspend fun getAll(): List<ProductEntity> = dao.getAll()
    override suspend fun getAllIncludingArchived(): List<ProductEntity> = dao.getAllIncludingArchived()
    override suspend fun getById(id: Long): ProductEntity? = dao.getById(id)
    override suspend fun getByBarcode(barcode: String): ProductEntity? = dao.getByBarcode(barcode)
    override suspend fun upsert(product: ProductEntity): Long = dao.insert(product)
    override suspend fun archive(productId: Long) = dao.archive(productId)
    override fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()
}
