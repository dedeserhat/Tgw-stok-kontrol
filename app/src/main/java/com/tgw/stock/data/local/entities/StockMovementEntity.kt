package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Immutable audit log of every stock change, whatever caused it. quantity is always
 * stored positive; `type` says whether it increased or decreased currentStock. */
@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = StockBatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("productId"), Index("batchId"), Index("createdAt")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val batchId: Long? = null,
    val type: String, // MovementType.name
    val quantity: Double,
    val reason: String, // StockInReason/StockOutReason name, or "ADJUSTMENT"
    val note: String? = null,
    val unitCostMinor: Long = 0,
    val totalValueMinor: Long = 0,
    val relatedPurchaseOrderId: Long? = null,
    val relatedStockCountId: Long? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
