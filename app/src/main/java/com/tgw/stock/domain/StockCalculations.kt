package com.tgw.stock.domain

import kotlin.math.floor
import kotlin.math.max

/** Pure calculation helpers. Kept free of Room/entity types so they're easy to reason
 * about and reuse from both repositories and view models. */
object StockCalculations {

    fun stockStatus(currentStock: Double, minStock: Double): StockStatus = when {
        currentStock <= 0.0 -> StockStatus.OUT_OF_STOCK
        currentStock < minStock * 0.5 -> StockStatus.CRITICAL
        currentStock < minStock -> StockStatus.LOW
        else -> StockStatus.NORMAL
    }

    fun stockValue(currentStock: Double, avgCostPerUnitMinor: Long): Money =
        Money(Math.round(currentStock * avgCostPerUnitMinor))

    /** Weighted-average cost per base unit after receiving a new batch. */
    fun newAverageCost(
        existingStock: Double,
        existingAvgCostMinor: Long,
        incomingQuantity: Double,
        incomingUnitCostMinor: Long
    ): Long {
        val existingValue = existingStock * existingAvgCostMinor
        val incomingValue = incomingQuantity * incomingUnitCostMinor
        val totalQty = existingStock + incomingQuantity
        if (totalQty <= 0.0) return incomingUnitCostMinor
        return Math.round((existingValue + incomingValue) / totalQty)
    }

    /** Cost of one recipe unit, given each ingredient's quantity and the product's avg cost/base unit. */
    fun recipeCost(ingredientCosts: List<Pair<Double, Long>>): Money {
        val totalCents = ingredientCosts.sumOf { (qty, unitCostMinor) -> Math.round(qty * unitCostMinor) }
        return Money(totalCents)
    }

    fun foodCostPercent(recipeCost: Money, sellPrice: Money): Double {
        if (sellPrice.cents <= 0) return 0.0
        return (recipeCost.cents.toDouble() / sellPrice.cents.toDouble()) * 100.0
    }

    fun estimatedProfit(recipeCost: Money, sellPrice: Money): Money = sellPrice - recipeCost

    /** Max whole portions makeable across all ingredients given current stock. */
    fun portionsAvailable(ingredientNeeds: List<Pair<Double, Double>>): Int {
        // each pair = (quantityNeededPerPortion, currentStockOfThatIngredient)
        if (ingredientNeeds.isEmpty()) return 0
        var maxPortions = Int.MAX_VALUE
        for ((neededPerPortion, currentStock) in ingredientNeeds) {
            if (neededPerPortion <= 0.0) continue
            val possible = floor(currentStock / neededPerPortion).toInt()
            maxPortions = minOf(maxPortions, max(possible, 0))
        }
        return if (maxPortions == Int.MAX_VALUE) 0 else maxPortions
    }

    fun quantityToPurchase(currentStock: Double, targetStock: Double): Double =
        max(targetStock - currentStock, 0.0)

    fun estimatedPurchaseCost(quantity: Double, unitCostMinor: Long): Money =
        Money(Math.round(quantity * unitCostMinor))

    /** Percentage price change between an old and new package price (positive = increase). */
    fun priceChangePercent(oldPriceMinor: Long, newPriceMinor: Long): Double {
        if (oldPriceMinor <= 0) return 0.0
        return ((newPriceMinor - oldPriceMinor).toDouble() / oldPriceMinor.toDouble()) * 100.0
    }

    fun wasteCost(quantity: Double, unitCostMinor: Long): Money =
        Money(Math.round(quantity * unitCostMinor))

    /** Price per base unit for a supplier package (e.g. €18.90 for a 10kg bag -> €1.89/kg). */
    fun pricePerBaseUnit(packagePriceMinor: Long, packageQuantity: Double): Long {
        if (packageQuantity <= 0.0) return 0
        return Math.round(packagePriceMinor / packageQuantity)
    }
}
