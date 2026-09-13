package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.CategoryDao
import com.tgw.stock.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    suspend fun getAll(): List<CategoryEntity>
    suspend fun getById(id: Long): CategoryEntity?
    suspend fun upsert(category: CategoryEntity): Long
    suspend fun delete(category: CategoryEntity)
}

class RoomCategoryRepository(private val dao: CategoryDao) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()
    override suspend fun getAll(): List<CategoryEntity> = dao.getAll()
    override suspend fun getById(id: Long): CategoryEntity? = dao.getById(id)
    override suspend fun upsert(category: CategoryEntity): Long = dao.insert(category)
    override suspend fun delete(category: CategoryEntity) = dao.delete(category)
}
