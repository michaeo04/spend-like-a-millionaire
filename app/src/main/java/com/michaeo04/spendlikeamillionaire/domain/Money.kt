package com.michaeo04.spendlikeamillionaire.domain

/** Money math on `Long` USD cents. Never uses floating point for totals. */
object Money {
    /** Largest quantity affordable: floor(remaining / unit); 0 when either side is not positive. */
    fun maxQuantity(remainingCents: Long, unitCents: Long): Long =
        if (remainingCents <= 0 || unitCents <= 0) 0 else remainingCents / unitCents

    /** Throws [ArithmeticException] on overflow. */
    fun lineTotal(unitCents: Long, quantity: Long): Long = Math.multiplyExact(unitCents, quantity)

    /** Whole USD to cents, saturating at [Long.MAX_VALUE]; negative input gives 0. */
    fun usdToCents(usd: Long): Long = when {
        usd <= 0 -> 0
        usd > Long.MAX_VALUE / 100 -> Long.MAX_VALUE
        else -> usd * 100
    }

    fun percentSpent(spentCents: Long, totalCents: Long): Double =
        if (totalCents <= 0) 0.0 else spentCents.toDouble() / totalCents.toDouble() * 100.0

    fun clampQuantity(requested: Long, max: Long): Long = requested.coerceIn(0L, max.coerceAtLeast(0L))
}
