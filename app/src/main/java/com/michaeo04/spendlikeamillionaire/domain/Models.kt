package com.michaeo04.spendlikeamillionaire.domain

enum class Category(val id: String) {
    FOOD("food"),
    SHOPPING("shopping"),
    TECH("tech"),
    TRANSPORT("transport"),
    HOME("home"),
    TRAVEL("travel"),
    FUN("fun"),
    SPORTS_MUSIC("sports_music"),
    MEGA("mega");

    companion object {
        fun fromId(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}

data class LocalizedText(val en: String, val vi: String) {
    /** Falls back to English when the requested language is missing or blank. */
    fun get(lang: String): String = if (lang == "vi" && vi.isNotBlank()) vi else en
}

/** Prices are USD cents. [estimate] marks named valuations (clubs, stadiums, bands). */
data class Item(
    val id: String,
    val category: Category,
    val priceCents: Long,
    val name: LocalizedText,
    val icon: String,
    val estimate: Boolean,
)

data class Person(
    val id: String,
    val name: LocalizedText,
    val netWorthUsd: Long,
    val source: String,
    val asOf: String,
    val avatarColor: String,
)
