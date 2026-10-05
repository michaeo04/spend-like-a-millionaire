package com.michaeo04.spendlikeamillionaire.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test
    fun maxQuantityIsFloorOfRemainingOverPrice() {
        assertEquals(3L, Money.maxQuantity(remainingCents = 1000, unitCents = 300))
    }

    @Test
    fun maxQuantityIsZeroForNonPositiveInputs() {
        assertEquals(0L, Money.maxQuantity(0, 300))
        assertEquals(0L, Money.maxQuantity(-50, 300))
        assertEquals(0L, Money.maxQuantity(1000, 0))
        assertEquals(0L, Money.maxQuantity(1000, -1))
    }

    @Test
    fun maxQuantityHandlesLongMax() {
        assertEquals(Long.MAX_VALUE, Money.maxQuantity(Long.MAX_VALUE, 1))
    }

    @Test
    fun lineTotalMultiplies() {
        assertEquals(900L, Money.lineTotal(300, 3))
    }

    @Test
    fun lineTotalOverflowThrows() {
        assertThrows(ArithmeticException::class.java) { Money.lineTotal(Long.MAX_VALUE, 2) }
    }

    @Test
    fun clampQuantityKeepsValueInsideZeroToMax() {
        assertEquals(0L, Money.clampQuantity(-5, 10))
        assertEquals(10L, Money.clampQuantity(99, 10))
        assertEquals(7L, Money.clampQuantity(7, 10))
        assertEquals(0L, Money.clampQuantity(7, -3))
    }

    @Test
    fun usdToCentsMultipliesBy100AndSaturatesInsteadOfOverflowing() {
        assertEquals(94_200_000_000_000L, Money.usdToCents(942_000_000_000L))
        assertEquals(Long.MAX_VALUE, Money.usdToCents(Long.MAX_VALUE))
        assertEquals(0L, Money.usdToCents(-5))
    }

    @Test
    fun percentSpentIsZeroWhenTotalIsNotPositive() {
        assertEquals(0.0, Money.percentSpent(0, 0), 0.0)
        assertEquals(0.0, Money.percentSpent(10, -1), 0.0)
    }

    @Test
    fun percentSpentIsRatioTimesHundred() {
        assertEquals(25.0, Money.percentSpent(250, 1000), 1e-9)
    }
}
