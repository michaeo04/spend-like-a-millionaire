package com.michaeo04.spendlikeamillionaire.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartMathTest {
    private fun item(id: String, price: Long) =
        Item(id, Category.FUN, price, LocalizedText(id, id), "x", false)

    private val a = item("a", 30_000)
    private val b = item("b", 25_000)
    private val catalog = listOf(a, b).associateBy { it.id }

    @Test
    fun setQuantityWithinBalance() {
        val cart = CartMath.setQuantity(Cart(), a, 3, balanceCents = 100_000, items = catalog)
        assertEquals(mapOf("a" to 3L), cart.lines)
    }

    @Test
    fun setQuantityAboveAffordableIsClamped() {
        val cart = CartMath.setQuantity(Cart(), a, 99, balanceCents = 100_000, items = catalog)
        assertEquals(3L, cart.lines["a"])
    }

    @Test
    fun setQuantityNegativeOrZeroRemovesLine() {
        val start = Cart(mapOf("a" to 2L))
        assertTrue(CartMath.setQuantity(start, a, -1, 100_000, catalog).lines.isEmpty())
        assertTrue(CartMath.setQuantity(start, a, 0, 100_000, catalog).lines.isEmpty())
    }

    @Test
    fun itemsShareTheBalance() {
        val withA = CartMath.setQuantity(Cart(), a, 2, 100_000, catalog)
        val withB = CartMath.setQuantity(withA, b, 5, 100_000, catalog)
        assertEquals(1L, withB.lines["b"])
        assertEquals(85_000L, CartMath.total(withB, catalog))
    }

    @Test
    fun itemMoreExpensiveThanBalanceIsNotAdded() {
        val cart = CartMath.setQuantity(Cart(), a, 1, balanceCents = 10_000, items = catalog)
        assertTrue(cart.lines.isEmpty())
    }

    @Test
    fun raisingAnExistingLineCountsOnlyOtherLines() {
        val start = Cart(mapOf("a" to 1L, "b" to 1L))
        val cart = CartMath.setQuantity(start, a, 3, balanceCents = 100_000, items = catalog)
        assertEquals(1L, cart.lines["b"])
        assertEquals(2L, cart.lines["a"])
    }

    @Test
    fun totalIgnoresUnknownIdsAndNonPositiveQuantities() {
        val cart = Cart(mapOf("a" to 2L, "ghost" to 5L, "b" to 0L))
        assertEquals(60_000L, CartMath.total(cart, catalog))
    }

    @Test
    fun sanitizeDropsUnknownIds() {
        val cart = Cart(mapOf("a" to 1L, "removed_in_new_version" to 4L))
        assertEquals(mapOf("a" to 1L), CartMath.sanitize(cart, catalog, 1_000_000).lines)
    }

    @Test
    fun sanitizeTrimsCartThatExceedsSmallerBalance() {
        val cart = Cart(mapOf("a" to 3L))
        val result = CartMath.sanitize(cart, catalog, balanceCents = 50_000)
        assertEquals(1L, result.lines["a"])
        assertTrue(CartMath.total(result, catalog) <= 50_000)
    }

    @Test
    fun sanitizeNeverLeavesNegativeRemainingAcrossManyLines() {
        val cart = Cart(mapOf("a" to 2L, "b" to 4L))
        val balance = 70_000L
        val result = CartMath.sanitize(cart, catalog, balance)
        assertTrue(CartMath.total(result, catalog) <= balance)
    }

    @Test
    fun sanitizeDropsNonPositiveQuantities() {
        val cart = Cart(mapOf("a" to 0L, "b" to -2L))
        assertTrue(CartMath.sanitize(cart, catalog, 1_000_000).lines.isEmpty())
    }
}
