package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only price history for a supplier_product. Never update or delete a row here;
 * inserting a new one with a later effectiveDate is how a price change is recorded so
 * price-increase trends stay visible.
 */
@Entity(
    tableName = "supplier_prices",
    foreignKeys = [
        ForeignKey(
            entity = SupplierProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierProductId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("supplierProductId")]
)
data class SupplierPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierProductId: Long,
    val priceMinor: Long, // price for the whole package
    val effectiveDate: Long,
    val isSample: Boolean = false
)
