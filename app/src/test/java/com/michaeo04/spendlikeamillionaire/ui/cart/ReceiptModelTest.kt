package com.michaeo04.spendlikeamillionaire.ui.cart

import com.michaeo04.spendlikeamillionaire.testing.testItem
import com.michaeo04.spendlikeamillionaire.testing.testPerson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptModelTest {
    private val strings = ReceiptStrings(
        title = "RECEIPT",
        totalLabel = "Total",
        remainingLabel = "Left",
        percentLabel = "of fortune",
        footer = "app",
        more = { n -> "+ $n more" },
    )

    private fun state(lineCount: Int) = CartUiState(
        loading = false,
        person = testPerson("p1", 1_000, en = "Rich Person"),
        language = "en",
        currency = "USD",
        rate = 1.0,
        balanceCents = 100_000,
        totalCents = lineCount * 1_000L,
        remainingCents = 100_000 - lineCount * 1_000L,
        percentSpent = lineCount * 1.0,
        lines = (1..lineCount).map { CartLineUi(testItem("i$it", 1_000, en = "Item $it"), 1, 1_000) },
    )

    @Test
    fun shortCartShowsAllLinesWithoutMore() {
        val model = buildReceipt(state(3), strings)
        assertEquals(3, model.lines.size)
        assertEquals(0, model.moreCount)
        assertEquals("Rich Person", model.personName)
    }

    @Test
    fun longCartIsTruncatedAndCountsTheRest() {
        val model = buildReceipt(state(12), strings, maxLines = 8)
        assertEquals(8, model.lines.size)
        assertEquals(4, model.moreCount)
        assertEquals("+ 4 more", model.moreText)
    }

    @Test
    fun totalsAreFormatted() {
        val model = buildReceipt(state(5), strings)
        assertEquals("$50.00", model.total)
        assertEquals("$950.00", model.remaining)
        assertEquals("5%", model.percent)
    }

    @Test
    fun lineLabelContainsNameAndQuantity() {
        val line = buildReceipt(state(1), strings).lines.single()
        assertTrue(line.label, line.label.contains("Item 1") && line.label.contains("×"))
        assertEquals("$10.00", line.amount)
    }
}
