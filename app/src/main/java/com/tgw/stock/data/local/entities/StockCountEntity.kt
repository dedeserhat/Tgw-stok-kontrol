package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_counts")
data class StockCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val countDate: Long = System.currentTimeMillis(),
    val status: String, // StockCountStatus.name
    val notes: String? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
