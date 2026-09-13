package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * currentStock / avgCostPerUnitMinor are denormalized running totals kept in sync by
 * StockRepository every time a movement, batch, PO receipt or count adjustment happens.
 * They exist so lists and the dashboard can be read with a single query instead of
 * aggregating stock_movements/stock_batches on every render.
 */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["preferredSupplierId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("preferredSupplierId"), Index(value = ["barcode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long?,
    val unit: String, // StockUnit.name
    val currentStock: Double = 0.0,
    val minStock: Double = 0.0,
    val targetStock: Double = 0.0,
    val avgCostPerUnitMinor: Long = 0,
    val lastCostPerUnitMinor: Long = 0,
    val lastPurchaseDate: Long? = null,
    val preferredSupplierId: Long? = null,
    val barcode: String? = null,
    val notes: String? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
