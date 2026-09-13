package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_count_items",
    foreignKeys = [
        ForeignKey(entity = StockCountEntity::class, parentColumns = ["id"], childColumns = ["stockCountId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("stockCountId"), Index("productId")]
)
data class StockCountItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockCountId: Long,
    val productId: Long,
    val systemQuantity: Double,
    val countedQuantity: Double? = null,
    val isSample: Boolean = false
)
