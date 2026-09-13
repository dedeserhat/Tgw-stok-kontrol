package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.*
import com.tgw.stock.domain.MovementType
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockCountStatus
import com.tgw.stock.domain.StockInReason

interface ReportsRepository {
    suspend fun getReport(fromMillis: Long, toMillis: Long): ReportsData
}

class DefaultReportsRepository(
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val movementDao: StockMovementDao,
    private val batchDao: StockBatchDao,
    private val wasteDao: WasteRecordDao,
    private val supplierProductDao: SupplierProductDao,
    private val supplierPriceDao: SupplierPriceDao,
    private val stockCountDao: StockCountDao,
    private val stockCountItemDao: StockCountItemDao
) : ReportsRepository {

    override suspend fun getReport(fromMillis: Long, toMillis: Long): ReportsData {
        val products = productDao.getAll()
        val productsById = products.associateBy { it.id }
        val suppliersById = supplierDao.getAll().associateBy { it.id }
        val batchesById = batchDao.getAll().associateBy { it.id }
        val movements = movementDao.getBetween(fromMillis, toMillis)

        val currentStockValue = products.sumOf {
            StockCalculations.stockValue(it.currentStock, it.avgCostPerUnitMinor).cents
        }

        val usageByProduct = movements
            .filter { it.type == MovementType.OUT.name }
            .groupBy { it.productId }
            .mapNotNull { (productId, list) ->
                val name = productsById[productId]?.name ?: return@mapNotNull null
                name to list.sumOf { it.quantity }
            }
            .sortedByDescending { it.second }

        val wasteCost = wasteDao.getBetween(fromMillis, toMillis).sumOf { it.totalCostMinor }

        val purchaseMovements = movements.filter {
            it.type == MovementType.IN.name && it.reason == StockInReason.SUPPLIER_DELIVERY.name
        }
        val purchaseSpend = purchaseMovements.sumOf { it.totalValueMinor }

        val supplierSpend = purchaseMovements
            .mapNotNull { m -> m.batchId?.let { batchesById[it]?.supplierId }?.let { it to m.totalValueMinor } }
            .groupBy { it.first }
            .map { (supplierId, list) -> (suppliersById[supplierId]?.companyName ?: "Unknown") to list.sumOf { it.second } }
            .sortedByDescending { it.second }

        val priceChanges = mutableListOf<PriceChangeReportLine>()
        val allLinks = supplierProductDao.getAll()
        for (link in allLinks) {
            val history = supplierPriceDao.getHistory(link.id) // desc by date
            if (history.size < 2) continue
            for (i in 0 until history.size - 1) {
                val newer = history[i]
                val older = history[i + 1]
                if (newer.effectiveDate < fromMillis || newer.effectiveDate > toMillis) continue
                val productName = productsById[link.productId]?.name ?: "Unknown"
                val supplierName = suppliersById[link.supplierId]?.companyName ?: "Unknown"
                priceChanges.add(
                    PriceChangeReportLine(
                        productName = productName,
                        supplierName = supplierName,
                        oldPriceMinor = older.priceMinor,
                        newPriceMinor = newer.priceMinor,
                        changePercent = StockCalculations.priceChangePercent(older.priceMinor, newer.priceMinor),
                        date = newer.effectiveDate
                    )
                )
            }
        }

        val lowStockFrequency = mutableMapOf<String, Int>()
        val completedCounts = stockCountDao.getByStatusBetween(StockCountStatus.COMPLETED.name, fromMillis, toMillis)
        for (count in completedCounts) {
            val items = stockCountItemDao.getForCount(count.id)
            for (item in items) {
                val product = productsById[item.productId] ?: continue
                if (item.systemQuantity < product.minStock) {
                    lowStockFrequency[product.name] = (lowStockFrequency[product.name] ?: 0) + 1
                }
            }
        }

        return ReportsData(
            currentStockValueMinor = currentStockValue,
            stockUsageQuantityByProduct = usageByProduct,
            wasteCostMinor = wasteCost,
            purchaseSpendMinor = purchaseSpend,
            supplierSpend = supplierSpend,
            priceChanges = priceChanges.sortedByDescending { it.date },
            mostUsedIngredients = usageByProduct.take(10),
            lowStockFrequency = lowStockFrequency.entries.map { it.key to it.value }.sortedByDescending { it.second }
        )
    }

}
