package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.SupplierDao
import com.tgw.stock.data.local.entities.SupplierEntity
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    fun observeAll(): Flow<List<SupplierEntity>>
    fun observeById(id: Long): Flow<SupplierEntity?>
    suspend fun getAll(): List<SupplierEntity>
    suspend fun getById(id: Long): SupplierEntity?
    suspend fun upsert(supplier: SupplierEntity): Long
    suspend fun delete(supplier: SupplierEntity)
}

class RoomSupplierRepository(private val dao: SupplierDao) : SupplierRepository {
    override fun observeAll(): Flow<List<SupplierEntity>> = dao.observeAll()
    override fun observeById(id: Long): Flow<SupplierEntity?> = dao.observeById(id)
    override suspend fun getAll(): List<SupplierEntity> = dao.getAll()
    override suspend fun getById(id: Long): SupplierEntity? = dao.getById(id)
    override suspend fun upsert(supplier: SupplierEntity): Long = dao.insert(supplier)
    override suspend fun delete(supplier: SupplierEntity) = dao.delete(supplier)
}
