package com.tgw.stock.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Money is always stored/passed as integer minor units (cents) to avoid floating point
 * rounding errors. All arithmetic happens on Longs; BigDecimal is only used at the
 * formatting boundary.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(factor: Double): Money = Money(Math.round(cents * factor))
    operator fun div(divisor: Double): Money =
        if (divisor == 0.0) Money(0) else Money(Math.round(cents / divisor))

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    fun toBigDecimal(): BigDecimal = BigDecimal(cents).divide(BigDecimal(100))

    fun format(): String = String.format(Locale.US, "€%,.2f", toBigDecimal())

    fun formatPlain(): String = String.format(Locale.US, "%.2f", toBigDecimal())

    companion object {
        val ZERO = Money(0)

        fun fromBigDecimal(value: BigDecimal): Money =
            Money(value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong())

        fun fromDouble(value: Double): Money = fromBigDecimal(BigDecimal.valueOf(value))

        /** Parses a user-typed decimal string like "18.90" into Money, or null if invalid. */
        fun parseOrNull(text: String): Money? {
            val normalized = text.trim().replace(",", ".")
            if (normalized.isEmpty()) return null
            val value = normalized.toBigDecimalOrNull() ?: return null
            if (value.signum() < 0) return null
            return fromBigDecimal(value)
        }
    }
}

fun String.toBigDecimalOrNull(): BigDecimal? = try {
    BigDecimal(this)
} catch (e: NumberFormatException) {
    null
}
