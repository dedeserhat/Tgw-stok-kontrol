package com.tgw.stock.di

import android.content.Context
import com.tgw.stock.data.backup.BackupManager
import com.tgw.stock.data.csv.CsvExporter
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.repository.*
import com.tgw.stock.data.seed.SampleDataSeeder

/**
 * Hand-rolled dependency container (no Hilt) exposed from the Application class.
 * Everything is interface-typed so swapping the Room-backed implementations for
 * Supabase-backed ones later only means changing what gets constructed here -
 * ViewModels and UI never depend on the concrete Room classes.
 */
class AppContainer(context: Context) {

    private val db = AppDatabase.getInstance(context)

    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val supplierDao = db.supplierDao()
    private val supplierProductDao = db.supplierProductDao()
    private val supplierPriceDao = db.supplierPriceDao()
    private val recipeDao = db.recipeDao()
    private val recipeItemDao = db.recipeItemDao()
    private val stockBatchDao = db.stockBatchDao()
    private val stockMovementDao = db.stockMovementDao()
    private val purchaseOrderDao = db.purchaseOrderDao()
    private val purchaseOrderItemDao = db.purchaseOrderItemDao()
    private val stockCountDao = db.stockCountDao()
    private val stockCountItemDao = db.stockCountItemDao()
    private val wasteRecordDao = db.wasteRecordDao()

    val categoryRepository: CategoryRepository = RoomCategoryRepository(categoryDao)
    val productRepository: ProductRepository = RoomProductRepository(productDao)
    val supplierRepository: SupplierRepository = RoomSupplierRepository(supplierDao)
    val supplierPriceRepository: SupplierPriceRepository =
        RoomSupplierPriceRepository(supplierProductDao, supplierPriceDao, supplierDao, productDao)
    val recipeRepository: RecipeRepository = RoomRecipeRepository(recipeDao, recipeItemDao, productDao)
    val stockRepository: StockRepository = RoomStockRepository(db, productDao, stockBatchDao, stockMovementDao)
    val purchaseRepository: PurchaseRepository =
        RoomPurchaseRepository(db, productDao, purchaseOrderDao, purchaseOrderItemDao, supplierPriceRepository, stockRepository)
    val stockCountRepository: StockCountRepository =
        RoomStockCountRepository(db, productDao, stockCountDao, stockCountItemDao, stockRepository)
    val wasteRepository: WasteRepository = RoomWasteRepository(db, productDao, wasteRecordDao, stockRepository)
    val dashboardRepository: DashboardRepository = DefaultDashboardRepository(productDao, stockRepository, purchaseRepository)
    val productDetailRepository: ProductDetailRepository =
        DefaultProductDetailRepository(productDao, categoryDao, supplierPriceRepository, recipeRepository, stockRepository)
    val reportsRepository: ReportsRepository = DefaultReportsRepository(
        productDao, supplierDao, stockMovementDao, stockBatchDao, wasteRecordDao,
        supplierProductDao, supplierPriceDao, stockCountDao, stockCountItemDao
    )
    val maintenanceRepository: MaintenanceRepository = DefaultMaintenanceRepository(
        db, stockCountItemDao, stockCountDao, purchaseOrderItemDao, purchaseOrderDao,
        wasteRecordDao, stockMovementDao, stockBatchDao, recipeItemDao, recipeDao,
        supplierPriceDao, supplierProductDao, productDao, supplierDao, categoryDao
    )

    val backupManager = BackupManager(context)
    val csvExporter = CsvExporter(
        context, categoryDao, productDao, supplierDao, supplierProductDao, supplierPriceDao,
        recipeDao, recipeItemDao, stockMovementDao, purchaseOrderDao, purchaseOrderItemDao, wasteRecordDao
    )

    val sampleDataSeeder = SampleDataSeeder(
        productDao, categoryRepository, supplierRepository, supplierPriceRepository,
        recipeRepository, stockRepository, wasteRepository, purchaseOrderDao, purchaseOrderItemDao
    )
}
