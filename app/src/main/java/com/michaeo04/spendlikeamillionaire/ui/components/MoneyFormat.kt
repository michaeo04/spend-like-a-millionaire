package com.michaeo04.spendlikeamillionaire.ui.components

import androidx.annotation.StringRes
import com.michaeo04.spendlikeamillionaire.R
import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.domain.Formatting
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bundles currency, rate and locale so composables format USD cents consistently. */
class MoneyFormatter(val currency: String, val rate: Double?, val language: String) {
    val locale: Locale = Locale.forLanguageTag(language)

    fun money(usdCents: Long): String = Formatting.money(usdCents, currency, rate, locale)

    fun percent(p: Double): String = Formatting.percent(p, locale)

    /** "2026-09" -> "Sep 2026" (localized); unparseable input is returned as is. */
    fun month(asOf: String): String = try {
        YearMonth.parse(asOf).format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
    } catch (e: Exception) {
        asOf
    }
}

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.FOOD -> R.string.category_food
    Category.SHOPPING -> R.string.category_shopping
    Category.TECH -> R.string.category_tech
    Category.TRANSPORT -> R.string.category_transport
    Category.HOME -> R.string.category_home
    Category.TRAVEL -> R.string.category_travel
    Category.FUN -> R.string.category_fun
    Category.SPORTS_MUSIC -> R.string.category_sports_music
    Category.MEGA -> R.string.category_mega
}
