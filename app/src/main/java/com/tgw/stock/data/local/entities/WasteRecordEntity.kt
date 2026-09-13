package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "waste_records",
    foreignKeys = [
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("productId"), Index("recordedDate")]
)
data class WasteRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val quantity: Double,
    val unitCostMinor: Long,
    val totalCostMinor: Long,
    val reason: String, // WasteReason.name
    val note: String? = null,
    val relatedMovementId: Long? = null,
    val isSample: Boolean = false,
    val recordedDate: Long = System.currentTimeMillis()
)
