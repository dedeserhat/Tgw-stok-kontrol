package com.tgw.stock.ui.common

import com.tgw.stock.domain.StockUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

fun Long.formatDate(): String = dateFormat.format(Date(this))
fun Long.formatDateTime(): String = dateTimeFormat.format(Date(this))

fun Long.centsToEuro(): String {
    val amount = this / 100.0
    return String.format(Locale.US, "€%,.2f", amount)
}

/** Trims trailing zeros for a friendlier quantity display: 3.0 -> "3", 3.50 -> "3.5". */
fun Double.formatQuantity(): String {
    return if (abs(this - this.toLong()) < 0.001) {
        this.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", this).trimEnd('0').trimEnd('.')
    }
}

fun formatQuantityWithUnit(quantity: Double, unit: String): String {
    val unitLabel = StockUnit.fromNameSafe(unit).label
    return "${quantity.formatQuantity()} $unitLabel"
}

fun Double.formatPercent(): String = String.format(Locale.US, "%.1f%%", this)
