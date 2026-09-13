package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A received lot of a product, tracked separately so FEFO consumption and expiry
 * warnings are possible. quantityRemaining is decremented as stock-out movements
 * consume it; a batch is exhausted (but kept, for history) once it hits 0. */
@Entity(
    tableName = "stock_batches",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("productId"), Index("supplierId"), Index("expiryDate")]
)
data class StockBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantityOriginal: Double,
    val quantityRemaining: Double,
    val unitCostMinor: Long,
    val expiryDate: Long? = null,
    val receivedDate: Long = System.currentTimeMillis(),
    val supplierId: Long? = null,
    val isSample: Boolean = false
)
