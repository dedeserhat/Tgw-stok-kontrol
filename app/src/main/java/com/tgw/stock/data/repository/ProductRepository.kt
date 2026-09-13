package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeAll(): Flow<List<ProductEntity>>
    fun observeById(id: Long): Flow<ProductEntity?>
    suspend fun getAll(): List<ProductEntity>
    suspend fun getById(id: Long): ProductEntity?
    suspend fun getByBarcode(barcode: String): ProductEntity?
    suspend fun upsert(product: ProductEntity): Long
    suspend fun delete(product: ProductEntity)
    fun observeTotalCount(): Flow<Int>
}

class RoomProductRepository(private val dao: ProductDao) : ProductRepository {
    override fun observeAll(): Flow<List<ProductEntity>> = dao.observeAll()
    override fun observeById(id: Long): Flow<ProductEntity?> = dao.observeById(id)
    override suspend fun getAll(): List<ProductEntity> = dao.getAll()
    override suspend fun getById(id: Long): ProductEntity? = dao.getById(id)
    override suspend fun getByBarcode(barcode: String): ProductEntity? = dao.getByBarcode(barcode)
    override suspend fun upsert(product: ProductEntity): Long = dao.insert(product)
    override suspend fun delete(product: ProductEntity) = dao.delete(product)
    override fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()
}
