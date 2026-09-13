package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Links a supplier to a product with the package size that supplier sells it in
 * (e.g. "10 kg bucket"). packageQuantity is expressed in the product's base unit so
 * price-per-base-unit can be computed as latest price / packageQuantity.
 */
@Entity(
    tableName = "supplier_products",
    foreignKeys = [
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("supplierId"), Index("productId")]
)
data class SupplierProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val productId: Long,
    val packageDescription: String,
    val packageQuantity: Double,
    val isSample: Boolean = false
)
