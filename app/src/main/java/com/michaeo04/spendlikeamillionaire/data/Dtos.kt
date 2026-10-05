package com.michaeo04.spendlikeamillionaire.data

import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.LocalizedText
import com.michaeo04.spendlikeamillionaire.domain.Person
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

private val json = Json { ignoreUnknownKeys = true }

@Serializable
internal data class NameDto(val en: String, val vi: String? = null) {
    fun toDomain() = LocalizedText(en = en, vi = vi?.takeIf { it.isNotBlank() } ?: en)
}

@Serializable
internal data class ItemDto(
    val id: String,
    val category: String,
    val priceCents: Long,
    val name: NameDto,
    val icon: String,
    val estimate: Boolean = false,
)

@Serializable
internal data class AvatarDto(val color: String = DEFAULT_AVATAR_COLOR)

@Serializable
internal data class PersonDto(
    val id: String,
    val name: NameDto,
    val netWorthUsd: Long,
    val source: String,
    val asOf: String,
    val avatar: AvatarDto = AvatarDto(),
)

@Serializable
internal data class FxDto(val base: String = "USD", val rates: Map<String, Double> = emptyMap())

internal const val DEFAULT_AVATAR_COLOR = "#5B8DEF"

private inline fun <reified T> decodeLenient(text: String, convert: (T) -> Unit) {
    val array = try {
        json.parseToJsonElement(text) as? JsonArray ?: return
    } catch (e: Exception) {
        return
    }
    for (element: JsonElement in array) {
        try {
            convert(json.decodeFromJsonElement<T>(element))
        } catch (e: Exception) {
            // A single bad element must never break the whole list.
        }
    }
}

/** Items with an unknown category, blank id or non-positive price are skipped. */
fun parseCatalog(text: String): List<Item> {
    val out = mutableListOf<Item>()
    decodeLenient<ItemDto>(text) { dto ->
        val category = Category.fromId(dto.category)
        if (category != null && dto.id.isNotBlank() && dto.priceCents > 0) {
            out += Item(dto.id, category, dto.priceCents, dto.name.toDomain(), dto.icon, dto.estimate)
        }
    }
    return out
}

/** People with a non-positive net worth or blank id are skipped. */
fun parsePeople(text: String): List<Person> {
    val out = mutableListOf<Person>()
    decodeLenient<PersonDto>(text) { dto ->
        if (dto.id.isNotBlank() && dto.netWorthUsd > 0) {
            out += Person(dto.id, dto.name.toDomain(), dto.netWorthUsd, dto.source, dto.asOf, dto.avatar.color)
        }
    }
    return out
}

/** Always contains "USD" -> 1.0; non-positive rates are dropped; garbage input yields just USD. */
fun parseFx(text: String): Map<String, Double> {
    val dto = try {
        json.decodeFromString<FxDto>(text)
    } catch (e: Exception) {
        FxDto()
    }
    return dto.rates.filterValues { it > 0.0 && it.isFinite() } + ("USD" to 1.0)
}
