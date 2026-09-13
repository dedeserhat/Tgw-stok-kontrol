package com.tgw.stock.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val minOrderMinor: Long? = null,
    val deliveryDays: String? = null, // comma-separated, e.g. "Mon,Wed,Fri"
    val paymentTerms: String? = null,
    val notes: String? = null,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
