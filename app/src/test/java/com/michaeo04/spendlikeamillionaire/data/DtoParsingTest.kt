package com.michaeo04.spendlikeamillionaire.data

import com.michaeo04.spendlikeamillionaire.domain.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoParsingTest {
    private val validItem = """
        {"id":"coffee","category":"food","priceCents":450,
         "name":{"en":"Coffee","vi":"Cà phê"},"icon":"☕","estimate":false}
    """.trimIndent()

    @Test
    fun parsesValidItem() {
        val items = parseCatalog("[$validItem]")
        assertEquals(1, items.size)
        val item = items.single()
        assertEquals("coffee", item.id)
        assertEquals(Category.FOOD, item.category)
        assertEquals(450L, item.priceCents)
        assertEquals("Cà phê", item.name.get("vi"))
        assertEquals("Coffee", item.name.get("en"))
        assertEquals("☕", item.icon)
        assertEquals(false, item.estimate)
    }

    @Test
    fun parsesOptionalImagePath() {
        val withImage = validItem.replace("\"icon\"", "\"image\":\"images/coffee.webp\",\"icon\"")
        assertEquals("images/coffee.webp", parseCatalog("[$withImage]").single().image)
        assertEquals(null, parseCatalog("[$validItem]").single().image)
        val blank = validItem.replace("\"icon\"", "\"image\":\" \",\"icon\"")
        assertEquals(null, parseCatalog("[$blank]").single().image)
    }

    @Test
    fun parsesImageCreditsAndSkipsBrokenEntries() {
        val json = """[
            {"id":"a","title":"A.jpg","author":"Jane","license":"CC BY 4.0","url":"https://x/a"},
            {"id":"b","title":"B.jpg"},
            {"id":"","title":"C.jpg","author":"Z","license":"CC0","url":"https://x/c"}
        ]"""
        val credits = parseCredits(json)
        assertEquals(listOf("a"), credits.map { it.id })
        assertEquals("Jane", credits.single().author)
        assertTrue(parseCredits("garbage").isEmpty())
    }

    @Test
    fun unknownCategoryIsSkippedNotFatal() {
        val bad = validItem.replace("\"food\"", "\"nonsense\"").replace("coffee", "bad")
        val items = parseCatalog("[$bad,$validItem]")
        assertEquals(listOf("coffee"), items.map { it.id })
    }

    @Test
    fun missingViFallsBackToEnglish() {
        val json = """[{"id":"x","category":"fun","priceCents":100,"name":{"en":"Only EN"},"icon":"🎫"}]"""
        assertEquals("Only EN", parseCatalog(json).single().name.get("vi"))
    }

    @Test
    fun nonPositivePriceIsSkipped() {
        val zero = validItem.replace("450", "0").replace("coffee", "zero")
        val negative = validItem.replace("450", "-5").replace("coffee", "neg")
        assertTrue(parseCatalog("[$zero,$negative]").isEmpty())
    }

    @Test
    fun malformedElementIsSkippedAndInvalidJsonGivesEmptyList() {
        val noId = """{"category":"food","priceCents":1,"name":{"en":"x"},"icon":"x"}"""
        assertEquals(listOf("coffee"), parseCatalog("[$noId,$validItem]").map { it.id })
        assertTrue(parseCatalog("not json").isEmpty())
        assertTrue(parseCatalog("{}").isEmpty())
    }

    @Test
    fun parsesPeopleAndRejectsNonPositiveNetWorth() {
        val ok = """{"id":"a","name":{"en":"A","vi":"A"},"netWorthUsd":100,"source":"Bloomberg","asOf":"2026-09"}"""
        val bad = ok.replace("\"id\":\"a\"", "\"id\":\"b\"").replace("100", "0")
        val people = parsePeople("[$ok,$bad]")
        assertEquals(listOf("a"), people.map { it.id })
        assertEquals("#5B8DEF", people.single().avatarColor)
        assertEquals(100L, people.single().netWorthUsd)
    }

    @Test
    fun fxAlwaysContainsUsdAndDropsBadRates() {
        val rates = parseFx("""{"base":"USD","rates":{"VND":25400.0,"EUR":0.0,"JPY":-1.0}}""")
        assertEquals(1.0, rates["USD"]!!, 0.0)
        assertEquals(25400.0, rates["VND"]!!, 0.0)
        assertTrue("EUR" !in rates)
        assertTrue("JPY" !in rates)
        assertEquals(mapOf("USD" to 1.0), parseFx("garbage"))
    }
}
