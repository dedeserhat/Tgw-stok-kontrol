package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sellPriceMinor: Long = 0,
    val notes: String? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
