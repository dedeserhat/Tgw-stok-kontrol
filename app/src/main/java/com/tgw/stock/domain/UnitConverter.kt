package com.tgw.stock.domain

/**
 * Converts between units that belong to the same measurement family (weight: kg/g,
 * volume: L/ml). Every product tracks stock in a single "base unit" (Product.unit).
 * When staff enter a purchase package size or a recipe quantity in a different but
 * compatible unit (e.g. buying in L while the product's base unit is ml), this
 * converts it to the base unit so all stored quantities stay consistent.
 *
 * Countable units (piece/box/pack/tray/bottle/bag) have no fixed conversion between
 * each other - they only convert 1:1 to themselves.
 */
object UnitConverter {

    private val weightFamily = setOf(StockUnit.KG, StockUnit.G)
    private val volumeFamily = setOf(StockUnit.L, StockUnit.ML)

    /** Base units per 1 unit of `unit`, within its family (kg=1000g, L=1000ml). */
    private fun baseFactor(unit: StockUnit): Double = when (unit) {
        StockUnit.KG -> 1000.0
        StockUnit.G -> 1.0
        StockUnit.L -> 1000.0
        StockUnit.ML -> 1.0
        else -> 1.0
    }

    fun areCompatible(a: StockUnit, b: StockUnit): Boolean =
        a == b || (a in weightFamily && b in weightFamily) || (a in volumeFamily && b in volumeFamily)

    /** Converts [quantity] expressed in [from] into an equivalent quantity expressed in [to].
     * Returns null if the two units aren't in the same family (e.g. kg -> piece). */
    fun convert(quantity: Double, from: StockUnit, to: StockUnit): Double? {
        if (from == to) return quantity
        if (!areCompatible(from, to)) return null
        val inSmallestUnit = quantity * baseFactor(from)
        return inSmallestUnit / baseFactor(to)
    }

    fun compatibleUnitsFor(baseUnit: StockUnit): List<StockUnit> = when {
        baseUnit in weightFamily -> listOf(StockUnit.KG, StockUnit.G)
        baseUnit in volumeFamily -> listOf(StockUnit.L, StockUnit.ML)
        else -> listOf(baseUnit)
    }
}
