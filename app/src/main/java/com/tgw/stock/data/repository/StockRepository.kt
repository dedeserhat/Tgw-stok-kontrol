package com.tgw.stock.data.repository

import androidx.room.withTransaction
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.StockBatchDao
import com.tgw.stock.data.local.dao.StockMovementDao
import com.tgw.stock.data.local.entities.StockBatchEntity
import com.tgw.stock.data.local.entities.StockMovementEntity
import com.tgw.stock.domain.MovementType
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockInReason
import com.tgw.stock.domain.StockOutReason
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface StockRepository {
    suspend fun stockIn(
        productId: Long,
        quantity: Double,
        unitCostMinor: Long,
        reason: StockInReason,
        note: String?,
        expiryDate: Long?,
        supplierId: Long?,
        relatedPurchaseOrderId: Long? = null,
        isSample: Boolean = false
    )

    suspend fun stockOut(
        productId: Long,
        quantity: Double,
        reason: StockOutReason,
        note: String?,
        isSample: Boolean = false
    )

    /** Sets the product's stock to exactly [countedQuantity] and logs the difference as an
     * ADJUSTMENT movement tied to the given stock count. */
    suspend fun adjustToCount(productId: Long, countedQuantity: Double, stockCountId: Long)

    fun observeRecentMovements(limit: Int): Flow<List<StockMovementEntity>>
    fun observeMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>
    fun observeBatchesForProduct(productId: Long): Flow<List<StockBatchEntity>>
    suspend fun getMovementsForProduct(productId: Long, limit: Int): List<StockMovementEntity>
    suspend fun getExpiringSoon(days: Int): List<StockBatchEntity>
    suspend fun getRecentMovementViews(limit: Int, type: MovementType?): List<MovementView>
    suspend fun getMovementsBetween(from: Long, to: Long): List<StockMovementEntity>
}

class RoomStockRepository(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val batchDao: StockBatchDao,
    private val movementDao: StockMovementDao
) : StockRepository {

    override suspend fun stockIn(
        productId: Long,
        quantity: Double,
        unitCostMinor: Long,
        reason: StockInReason,
        note: String?,
        expiryDate: Long?,
        supplierId: Long?,
        relatedPurchaseOrderId: Long?,
        isSample: Boolean
    ) {
        require(quantity > 0.0) { "Quantity must be positive" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: return@withTransaction
            val batchId = batchDao.insert(
                StockBatchEntity(
                    productId = productId,
                    quantityOriginal = quantity,
                    quantityRemaining = quantity,
                    unitCostMinor = unitCostMinor,
                    expiryDate = expiryDate,
                    supplierId = supplierId,
                    isSample = isSample
                )
            )
            val newAvgCost = StockCalculations.newAverageCost(
                product.currentStock, product.avgCostPerUnitMinor, quantity, unitCostMinor
            )
            productDao.adjustStock(productId, quantity)
            productDao.updateCostInfo(productId, newAvgCost, unitCostMinor, System.currentTimeMillis())
            movementDao.insert(
                StockMovementEntity(
                    productId = productId,
                    batchId = batchId,
                    type = MovementType.IN.name,
                    quantity = quantity,
                    reason = reason.name,
                    note = note,
                    unitCostMinor = unitCostMinor,
                    totalValueMinor = Math.round(quantity * unitCostMinor),
                    relatedPurchaseOrderId = relatedPurchaseOrderId,
                    isSample = isSample
                )
            )
        }
    }

    override suspend fun stockOut(productId: Long, quantity: Double, reason: StockOutReason, note: String?, isSample: Boolean) {
        require(quantity > 0.0) { "Quantity must be positive" }
        db.withTransaction {
            consumeFefo(productId, quantity, reason.name, note, relatedStockCountId = null, isSample = isSample)
            productDao.adjustStock(productId, -quantity)
        }
    }

    override suspend fun adjustToCount(productId: Long, countedQuantity: Double, stockCountId: Long) {
        db.withTransaction {
            val product = productDao.getById(productId) ?: return@withTransaction
            val diff = countedQuantity - product.currentStock
            if (kotlin.math.abs(diff) < 0.0001) return@withTransaction

            if (diff > 0) {
                val batchId = batchDao.insert(
                    StockBatchEntity(
                        productId = productId,
                        quantityOriginal = diff,
                        quantityRemaining = diff,
                        unitCostMinor = product.avgCostPerUnitMinor,
                        expiryDate = null,
                        supplierId = null
                    )
                )
                movementDao.insert(
                    StockMovementEntity(
                        productId = productId,
                        batchId = batchId,
                        type = MovementType.ADJUSTMENT.name,
                        quantity = diff,
                        reason = StockInReason.STOCK_CORRECTION.name,
                        note = "Stock count adjustment",
                        unitCostMinor = product.avgCostPerUnitMinor,
                        totalValueMinor = Math.round(diff * product.avgCostPerUnitMinor),
                        relatedStockCountId = stockCountId
                    )
                )
                productDao.adjustStock(productId, diff)
            } else {
                val toConsume = -diff
                consumeFefo(productId, toConsume, StockOutReason.STOCK_CORRECTION.name, "Stock count adjustment", stockCountId)
                productDao.adjustStock(productId, diff)
            }
        }
    }

    /** Consumes [quantity] of a product's batches oldest-expiry-first, logging one movement
     * per batch touched. If batches run out before quantity is fully consumed (data drift),
     * the remainder is logged against the product's average cost with no batch reference. */
    private suspend fun consumeFefo(
        productId: Long,
        quantity: Double,
        reason: String,
        note: String?,
        relatedStockCountId: Long?,
        isSample: Boolean = false
    ) {
        var remaining = quantity
        val batches = batchDao.getConsumableForProduct(productId)
        for (batch in batches) {
            if (remaining <= 0.0) break
            val consume = minOf(batch.quantityRemaining, remaining)
            if (consume <= 0.0) continue
            batchDao.updateRemaining(batch.id, batch.quantityRemaining - consume)
            movementDao.insert(
                StockMovementEntity(
                    productId = productId,
                    batchId = batch.id,
                    type = if (relatedStockCountId != null) MovementType.ADJUSTMENT.name else MovementType.OUT.name,
                    quantity = consume,
                    reason = reason,
                    note = note,
                    unitCostMinor = batch.unitCostMinor,
                    totalValueMinor = Math.round(consume * batch.unitCostMinor),
                    relatedStockCountId = relatedStockCountId,
                    isSample = isSample
                )
            )
            remaining -= consume
        }
        if (remaining > 0.0) {
            val product = productDao.getById(productId)
            val cost = product?.avgCostPerUnitMinor ?: 0
            movementDao.insert(
                StockMovementEntity(
                    productId = productId,
                    batchId = null,
                    type = if (relatedStockCountId != null) MovementType.ADJUSTMENT.name else MovementType.OUT.name,
                    quantity = remaining,
                    reason = reason,
                    note = note,
                    unitCostMinor = cost,
                    totalValueMinor = Math.round(remaining * cost),
                    relatedStockCountId = relatedStockCountId,
                    isSample = isSample
                )
            )
        }
    }

    override fun observeRecentMovements(limit: Int): Flow<List<StockMovementEntity>> = movementDao.observeRecent(limit)
    override fun observeMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>> = movementDao.observeForProduct(productId)
    override fun observeBatchesForProduct(productId: Long): Flow<List<StockBatchEntity>> = batchDao.observeAllForProduct(productId)

    override suspend fun getMovementsForProduct(productId: Long, limit: Int): List<StockMovementEntity> =
        movementDao.getForProduct(productId, limit)

    override suspend fun getExpiringSoon(days: Int): List<StockBatchEntity> {
        val threshold = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return batchDao.getExpiringBefore(threshold)
    }

    override suspend fun getRecentMovementViews(limit: Int, type: MovementType?): List<MovementView> {
        val movements = if (type != null) {
            movementDao.getRecentByType(type.name, limit)
        } else {
            movementDao.getRecent(limit)
        }
        return movements.map { m ->
            val product = productDao.getById(m.productId)
            MovementView(
                id = m.id,
                productId = m.productId,
                productName = product?.name ?: "Unknown",
                type = m.type,
                quantity = m.quantity,
                unit = product?.unit ?: "",
                reason = m.reason,
                note = m.note,
                createdAt = m.createdAt
            )
        }
    }

    override suspend fun getMovementsBetween(from: Long, to: Long): List<StockMovementEntity> =
        movementDao.getBetween(from, to)
}
