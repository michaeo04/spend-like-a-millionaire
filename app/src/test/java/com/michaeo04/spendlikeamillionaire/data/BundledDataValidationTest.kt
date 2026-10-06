package com.michaeo04.spendlikeamillionaire.data

import com.michaeo04.spendlikeamillionaire.domain.Category
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled JSON on every build so bad content can never ship. */
class BundledDataValidationTest {
    private fun asset(name: String) = File("src/main/assets/$name").readText()
    private fun array(name: String): JsonArray = Json.parseToJsonElement(asset(name)).jsonArray

    private fun JsonObject.text(vararg path: String): String {
        var node: kotlinx.serialization.json.JsonElement = this
        for (key in path) node = node.jsonObject[key] ?: return ""
        return node.jsonPrimitive.content
    }

    @Test
    fun catalogIsValidAndNothingIsSilentlySkipped() {
        val raw = array("catalog.json")
        assertTrue("catalog must not be empty", raw.isNotEmpty())
        val ids = mutableSetOf<String>()
        val knownCategories = Category.entries.map { it.id }.toSet()
        for (element in raw) {
            val o = element.jsonObject
            val id = o.text("id")
            assertTrue("blank id", id.isNotBlank())
            assertTrue("duplicate id $id", ids.add(id))
            assertTrue("$id: unknown category", o.text("category") in knownCategories)
            assertTrue("$id: price must be positive", o["priceCents"]!!.jsonPrimitive.long > 0)
            assertTrue("$id: missing en name", o.text("name", "en").isNotBlank())
            assertTrue("$id: missing vi name", o.text("name", "vi").isNotBlank())
            assertTrue("$id: missing icon", o.text("icon").isNotBlank())
            // estimate=true is allowed in any category (rough figures show the "≈" badge)
            o["estimate"]?.jsonPrimitive?.boolean
        }
        assertEquals("some catalog items fail to parse", raw.size, parseCatalog(asset("catalog.json")).size)
    }

    @Test
    fun everyReferencedImageExistsIsSmallAndHasALicensedCredit() {
        val items = parseCatalog(asset("catalog.json"))
        val credits = parseCredits(asset("image_credits.json")).associateBy { it.id }
        val withImage = items.filter { it.image != null }
        assertTrue("most items should have a photo, have ${withImage.size}/${items.size}", withImage.size * 10 >= items.size * 8)
        for (item in withImage) {
            val file = File("src/main/assets/${item.image}")
            assertTrue("${item.id}: missing ${item.image}", file.exists())
            assertTrue("${item.id}: image too large (${file.length()} bytes)", file.length() < 80_000)
            val credit = credits[item.id]
            assertTrue("${item.id}: no credit entry", credit != null)
            assertTrue("${item.id}: author missing", credit!!.author.isNotBlank())
            assertTrue(
                "${item.id}: license not allowed: ${credit.license}",
                Regex("^(CC0|CC BY|CC-BY|Public domain|PD)", RegexOption.IGNORE_CASE).containsMatchIn(credit.license) &&
                    !Regex("(NC|ND)", RegexOption.IGNORE_CASE).containsMatchIn(credit.license),
            )
        }
    }

    @Test
    fun catalogNamesAreUniquePerLanguage() {
        val items = parseCatalog(asset("catalog.json"))
        for ((label, names) in listOf("en" to items.map { it.name.en }, "vi" to items.map { it.name.vi })) {
            val duplicates = names.groupBy { it.lowercase() }.filterValues { it.size > 1 }.keys
            assertTrue("duplicate $label names: $duplicates", duplicates.isEmpty())
        }
    }

    @Test
    fun catalogCoversTheWholePriceLadder() {
        val prices = parseCatalog(asset("catalog.json")).map { it.priceCents }
        assertTrue("need at least 300 items, have ${prices.size}", prices.size >= 300)
        assertTrue("cheapest item must be at most \$2", prices.min() <= 200)
        assertTrue("need 20+ items under \$20", prices.count { it < 2_000 } >= 20)
        assertTrue("need 15+ items of \$1B or more", prices.count { it >= 100_000_000_000L } >= 15)
        val richest = parsePeople(asset("people.json")).maxOf { it.netWorthUsd }
        assertTrue("top item should be a meaningful share of the richest fortune", prices.max() / 100 >= richest / 4)
    }

    @Test
    fun peopleAreValid() {
        val raw = array("people.json")
        assertTrue(raw.isNotEmpty())
        val ids = mutableSetOf<String>()
        for (element in raw) {
            val o = element.jsonObject
            val id = o.text("id")
            assertTrue("duplicate person $id", ids.add(id))
            assertTrue("$id: net worth must be positive", o["netWorthUsd"]!!.jsonPrimitive.long > 0)
            assertTrue("$id: asOf must be YYYY-MM", Regex("""\d{4}-\d{2}""").matches(o.text("asOf")))
            assertTrue("$id: source", o.text("source").isNotBlank())
            assertTrue("$id: en name", o.text("name", "en").isNotBlank())
            assertTrue("$id: vi name", o.text("name", "vi").isNotBlank())
            assertTrue("$id: avatar color", Regex("""#[0-9A-Fa-f]{6}""").matches(o.text("avatar", "color")))
            assertTrue("$id: group", o.text("group") in setOf("billionaire", "celebrity"))
        }
        assertEquals(raw.size, parsePeople(asset("people.json")).size)
    }

    @Test
    fun everyPersonPhotoExistsIsSmallAndHasALicensedCredit() {
        val people = parsePeople(asset("people.json"))
        val credits = parseCredits(asset("image_credits.json")).associateBy { it.id }
        val withPhoto = people.filter { it.image != null }
        assertTrue("most people should have a photo, have ${withPhoto.size}/${people.size}", withPhoto.size * 10 >= people.size * 7)
        assertTrue("need both groups", people.map { it.group }.toSet().size == 2)
        for (person in withPhoto) {
            val file = File("src/main/assets/${person.image}")
            assertTrue("${person.id}: missing ${person.image}", file.exists())
            assertTrue("${person.id}: image too large (${file.length()} bytes)", file.length() < 80_000)
            val credit = credits[person.id]
            assertTrue("${person.id}: no credit entry", credit != null)
            assertTrue("${person.id}: author missing", credit!!.author.isNotBlank())
            assertTrue(
                "${person.id}: license not allowed: ${credit.license}",
                Regex("^(CC0|CC BY|CC-BY|Public domain|PD)", RegexOption.IGNORE_CASE).containsMatchIn(credit.license) &&
                    !Regex("(NC|ND)", RegexOption.IGNORE_CASE).containsMatchIn(credit.license),
            )
        }
    }

    @Test
    fun fxHasUsdAndPositiveRates() {
        val rates = parseFx(asset("fx.json"))
        assertEquals(1.0, rates["USD"]!!, 0.0)
        assertTrue(rates.size > 1)
        assertTrue(rates.values.all { it > 0.0 })
    }
}
