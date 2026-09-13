package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_order_items",
    foreignKeys = [
        ForeignKey(entity = PurchaseOrderEntity::class, parentColumns = ["id"], childColumns = ["purchaseOrderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("purchaseOrderId"), Index("productId")]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseOrderId: Long,
    val productId: Long,
    val quantity: Double, // in product base unit
    val unitCostMinor: Long,
    val receivedQuantity: Double = 0.0,
    val isSample: Boolean = false
)
