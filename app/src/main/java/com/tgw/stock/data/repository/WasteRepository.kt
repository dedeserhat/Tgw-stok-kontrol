package com.tgw.stock.data.repository

import androidx.room.withTransaction
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.WasteRecordDao
import com.tgw.stock.data.local.entities.WasteRecordEntity
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockOutReason
import com.tgw.stock.domain.WasteReason
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface WasteRepository {
    fun observeAll(): Flow<List<WasteRecordEntity>>
    suspend fun recordWaste(productId: Long, quantity: Double, reason: WasteReason, note: String?, isSample: Boolean = false)
    suspend fun getTotalCost(sinceMillis: Long): Long
    suspend fun getWeeklyTotalCost(): Long
    suspend fun getMonthlyTotalCost(): Long
}

class RoomWasteRepository(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val wasteDao: WasteRecordDao,
    private val stockRepository: StockRepository
) : WasteRepository {

    override fun observeAll(): Flow<List<WasteRecordEntity>> = wasteDao.observeAll()

    override suspend fun recordWaste(productId: Long, quantity: Double, reason: WasteReason, note: String?, isSample: Boolean) {
        require(quantity > 0.0) { "Quantity must be positive" }
        db.withTransaction {
            val product = productDao.getById(productId) ?: return@withTransaction
            val unitCost = product.avgCostPerUnitMinor
            val totalCost = StockCalculations.wasteCost(quantity, unitCost)

            val outReason = when (reason) {
                WasteReason.EXPIRED -> StockOutReason.EXPIRED
                WasteReason.DAMAGE -> StockOutReason.DAMAGE
                else -> StockOutReason.WASTE
            }
            stockRepository.stockOut(productId, quantity, outReason, note ?: reason.label, isSample)

            wasteDao.insert(
                WasteRecordEntity(
                    productId = productId,
                    quantity = quantity,
                    unitCostMinor = unitCost,
                    totalCostMinor = totalCost.cents,
                    reason = reason.name,
                    note = note,
                    isSample = isSample
                )
            )
        }
    }

    override suspend fun getTotalCost(sinceMillis: Long): Long =
        wasteDao.getBetween(sinceMillis, System.currentTimeMillis()).sumOf { it.totalCostMinor }

    override suspend fun getWeeklyTotalCost(): Long =
        getTotalCost(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))

    override suspend fun getMonthlyTotalCost(): Long =
        getTotalCost(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
}
