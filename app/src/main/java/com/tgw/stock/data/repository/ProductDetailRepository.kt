package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.CategoryDao
import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.domain.StockCalculations

interface ProductDetailRepository {
    suspend fun getDetail(productId: Long): ProductDetail?
}

/** Assembles the non-reactive, joined parts of the product detail screen (everything
 * except batches and price history, which the UI observes as live Flows directly). */
class DefaultProductDetailRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val supplierPriceRepository: SupplierPriceRepository,
    private val recipeRepository: RecipeRepository,
    private val stockRepository: StockRepository
) : ProductDetailRepository {

    override suspend fun getDetail(productId: Long): ProductDetail? {
        val product = productDao.getById(productId) ?: return null
        val category = product.categoryId?.let { categoryDao.getById(it) }
        val bestOffer = supplierPriceRepository.getCheapestOffer(productId)
        val usedIn = recipeRepository.getUsageForProduct(productId)
        val recentMovements = stockRepository.getMovementsForProduct(productId, 20).map { m ->
            MovementView(
                id = m.id,
                productId = m.productId,
                productName = product.name,
                type = m.type,
                quantity = m.quantity,
                unit = product.unit,
                reason = m.reason,
                note = m.note,
                createdAt = m.createdAt
            )
        }

        return ProductDetail(
            product = product,
            categoryName = category?.name,
            stockValueMinor = StockCalculations.stockValue(product.currentStock, product.avgCostPerUnitMinor).cents,
            status = StockCalculations.stockStatus(product.currentStock, product.minStock),
            bestOffer = bestOffer,
            usedIn = usedIn,
            recentMovements = recentMovements
        )
    }
}
