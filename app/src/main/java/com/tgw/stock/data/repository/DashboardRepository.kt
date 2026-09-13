package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.domain.MovementType
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockStatus

interface DashboardRepository {
    suspend fun getDashboardData(): DashboardData
}

class DefaultDashboardRepository(
    private val productDao: ProductDao,
    private val stockRepository: StockRepository,
    private val purchaseRepository: PurchaseRepository
) : DashboardRepository {

    override suspend fun getDashboardData(): DashboardData {
        val products = productDao.getAll()
        var critical = 0
        var outOfStock = 0
        var totalValue = 0L
        for (p in products) {
            totalValue += Math.round(p.currentStock * p.avgCostPerUnitMinor)
            when (StockCalculations.stockStatus(p.currentStock, p.minStock)) {
                StockStatus.CRITICAL -> critical++
                StockStatus.OUT_OF_STOCK -> outOfStock++
                else -> {}
            }
        }
        val expiringSoon = stockRepository.getExpiringSoon(3)
        val purchaseLines = purchaseRepository.buildPurchaseListLines()

        return DashboardData(
            totalProducts = products.size,
            criticalCount = critical,
            outOfStockCount = outOfStock,
            expiringSoonCount = expiringSoon.size,
            purchaseNeededCount = purchaseLines.size,
            totalStockValueMinor = totalValue,
            recentStockIn = stockRepository.getRecentMovementViews(5, MovementType.IN),
            recentStockOut = stockRepository.getRecentMovementViews(5, MovementType.OUT)
        )
    }
}
