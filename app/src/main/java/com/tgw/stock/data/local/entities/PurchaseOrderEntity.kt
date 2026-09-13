package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "purchase_orders",
    foreignKeys = [
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("supplierId"), Index(value = ["poNumber"], unique = true)]
)
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poNumber: String,
    val supplierId: Long,
    val status: String, // PurchaseOrderStatus.name
    val orderedDate: Long? = null,
    val expectedDate: Long? = null,
    val deliveredDate: Long? = null,
    val notes: String? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
