package com.tgw.stock.data.repository

import androidx.room.withTransaction
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.local.dao.*

interface MaintenanceRepository {
    /** Deletes every row flagged isSample = true, children before parents. Anything the
     * user has since added referencing a sample row (e.g. a real recipe using a sample
     * ingredient) is not specially protected beyond normal FK cascade behaviour. */
    suspend fun clearSampleData()
}

class DefaultMaintenanceRepository(
    private val db: AppDatabase,
    private val stockCountItemDao: StockCountItemDao,
    private val stockCountDao: StockCountDao,
    private val purchaseOrderItemDao: PurchaseOrderItemDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val wasteRecordDao: WasteRecordDao,
    private val stockMovementDao: StockMovementDao,
    private val stockBatchDao: StockBatchDao,
    private val recipeItemDao: RecipeItemDao,
    private val recipeDao: RecipeDao,
    private val supplierPriceDao: SupplierPriceDao,
    private val supplierProductDao: SupplierProductDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val categoryDao: CategoryDao
) : MaintenanceRepository {

    override suspend fun clearSampleData() {
        db.withTransaction {
            stockCountItemDao.deleteSample()
            stockCountDao.deleteSample()
            purchaseOrderItemDao.deleteSample()
            purchaseOrderDao.deleteSample()
            wasteRecordDao.deleteSample()
            stockMovementDao.deleteSample()
            stockBatchDao.deleteSample()
            recipeItemDao.deleteSample()
            recipeDao.deleteSample()
            supplierPriceDao.deleteSample()
            supplierProductDao.deleteSample()
            productDao.deleteSample()
            supplierDao.deleteSample()
            categoryDao.deleteSample()
        }
    }
}
