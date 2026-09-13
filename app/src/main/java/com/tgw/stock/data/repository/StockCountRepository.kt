package com.tgw.stock.data.repository

import androidx.room.withTransaction
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.StockCountDao
import com.tgw.stock.data.local.dao.StockCountItemDao
import com.tgw.stock.data.local.entities.StockCountEntity
import com.tgw.stock.data.local.entities.StockCountItemEntity
import com.tgw.stock.domain.StockCountStatus
import kotlinx.coroutines.flow.Flow

interface StockCountRepository {
    fun observeAll(): Flow<List<StockCountEntity>>
    suspend fun getById(id: Long): StockCountEntity?
    fun observeItems(stockCountId: Long): Flow<List<StockCountItemEntity>>
    suspend fun getItems(stockCountId: Long): List<StockCountItemEntity>

    /** Creates a new draft count snapshotting every active product's current stock. */
    suspend fun startCount(productIds: List<Long>? = null): Long
    suspend fun updateCountedQuantity(itemId: Long, countedQuantity: Double?)

    /** Applies every entered difference as a stock adjustment and marks the count complete. */
    suspend fun commitCount(stockCountId: Long)
}

class RoomStockCountRepository(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val countDao: StockCountDao,
    private val countItemDao: StockCountItemDao,
    private val stockRepository: StockRepository
) : StockCountRepository {

    override fun observeAll(): Flow<List<StockCountEntity>> = countDao.observeAll()
    override suspend fun getById(id: Long): StockCountEntity? = countDao.getById(id)
    override fun observeItems(stockCountId: Long): Flow<List<StockCountItemEntity>> = countItemDao.observeForCount(stockCountId)
    override suspend fun getItems(stockCountId: Long): List<StockCountItemEntity> = countItemDao.getForCount(stockCountId)

    override suspend fun startCount(productIds: List<Long>?): Long = db.withTransaction {
        val products = if (productIds != null) {
            productIds.mapNotNull { productDao.getById(it) }
        } else {
            productDao.getAll()
        }
        val countId = countDao.insert(StockCountEntity(status = StockCountStatus.DRAFT.name))
        countItemDao.insertAll(
            products.map { p ->
                StockCountItemEntity(stockCountId = countId, productId = p.id, systemQuantity = p.currentStock)
            }
        )
        countId
    }

    override suspend fun updateCountedQuantity(itemId: Long, countedQuantity: Double?) {
        countItemDao.updateCountedQuantity(itemId, countedQuantity)
    }

    override suspend fun commitCount(stockCountId: Long) {
        db.withTransaction {
            val count = countDao.getById(stockCountId) ?: return@withTransaction
            val items = countItemDao.getForCount(stockCountId)
            for (item in items) {
                val counted = item.countedQuantity ?: continue
                stockRepository.adjustToCount(item.productId, counted, stockCountId)
            }
            countDao.update(count.copy(status = StockCountStatus.COMPLETED.name))
        }
    }
}
