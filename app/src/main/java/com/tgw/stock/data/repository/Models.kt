package com.tgw.stock.data.repository

import com.tgw.stock.domain.StockStatus

/** Cross-entity DTOs returned by repository "compute" functions. These join data from
 * more than one table, so they're assembled with plain suspend queries rather than
 * reactive Room Flows (joining several invalidation-tracked Flows reactively adds a
 * lot of complexity for data that only needs to refresh after an explicit user action). */

data class SupplierOffer(
    val supplierProductId: Long,
    val supplierId: Long,
    val supplierName: String,
    val packageDescription: String,
    val packageQuantity: Double,
    val latestPriceMinor: Long,
    val pricePerBaseUnitMinor: Long,
    val priceDate: Long
)

data class ProductListItem(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val categoryName: String?,
    val unit: String,
    val currentStock: Double,
    val minStock: Double,
    val targetStock: Double,
    val stockValueMinor: Long,
    val status: StockStatus,
    val barcode: String?
)

data class RecipeUsage(
    val recipeId: Long,
    val recipeName: String,
    val quantity: Double
)

data class MovementView(
    val id: Long,
    val productId: Long,
    val productName: String,
    val type: String,
    val quantity: Double,
    val unit: String,
    val reason: String,
    val note: String?,
    val createdAt: Long
)

/** Batches and price history are observed reactively by the UI directly from
 * StockRepository/SupplierPriceRepository, so they aren't duplicated here. */
data class ProductDetail(
    val product: com.tgw.stock.data.local.entities.ProductEntity,
    val categoryName: String?,
    val stockValueMinor: Long,
    val status: StockStatus,
    val bestOffer: SupplierOffer?,
    val usedIn: List<RecipeUsage>,
    val recentMovements: List<MovementView>
)

data class RecipeCostSummary(
    val recipeId: Long,
    val recipeName: String,
    val sellPriceMinor: Long,
    val costMinor: Long,
    val foodCostPercent: Double,
    val profitMinor: Long,
    val portionsAvailable: Int,
    val missingIngredients: List<String>
)

data class PurchaseListLine(
    val productId: Long,
    val productName: String,
    val unit: String,
    val currentStock: Double,
    val targetStock: Double,
    val quantityToBuy: Double,
    val bestOffer: SupplierOffer?,
    val estimatedCostMinor: Long
)

data class SupplierPurchaseGroup(
    val supplierId: Long,
    val supplierName: String,
    val lines: List<PurchaseListLine>,
    val totalCostMinor: Long
)

data class DashboardData(
    val totalProducts: Int,
    val criticalCount: Int,
    val outOfStockCount: Int,
    val expiringSoonCount: Int,
    val purchaseNeededCount: Int,
    val totalStockValueMinor: Long,
    val recentStockIn: List<MovementView>,
    val recentStockOut: List<MovementView>
)

data class ReportsData(
    val currentStockValueMinor: Long,
    val stockUsageQuantityByProduct: List<Pair<String, Double>>,
    val wasteCostMinor: Long,
    val purchaseSpendMinor: Long,
    val supplierSpend: List<Pair<String, Long>>,
    val priceChanges: List<PriceChangeReportLine>,
    val mostUsedIngredients: List<Pair<String, Double>>,
    val lowStockFrequency: List<Pair<String, Int>>
)

data class PriceChangeReportLine(
    val productName: String,
    val supplierName: String,
    val oldPriceMinor: Long,
    val newPriceMinor: Long,
    val changePercent: Double,
    val date: Long
)
