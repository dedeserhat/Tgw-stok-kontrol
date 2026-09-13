package com.tgw.stock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tgw.stock.data.local.dao.*
import com.tgw.stock.data.local.entities.*

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        SupplierEntity::class,
        SupplierProductEntity::class,
        SupplierPriceEntity::class,
        RecipeEntity::class,
        RecipeItemEntity::class,
        StockBatchEntity::class,
        StockMovementEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        StockCountEntity::class,
        StockCountItemEntity::class,
        WasteRecordEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierProductDao(): SupplierProductDao
    abstract fun supplierPriceDao(): SupplierPriceDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeItemDao(): RecipeItemDao
    abstract fun stockBatchDao(): StockBatchDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun purchaseOrderItemDao(): PurchaseOrderItemDao
    abstract fun stockCountDao(): StockCountDao
    abstract fun stockCountItemDao(): StockCountItemDao
    abstract fun wasteRecordDao(): WasteRecordDao

    companion object {
        const val DATABASE_NAME = "tgw_stock.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()

        /** Used by backup/restore to close and reopen the DB against the same file path. */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
