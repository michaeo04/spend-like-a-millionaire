package com.michaeo04.spendlikeamillionaire.domain

import java.util.Currency

val SUPPORTED_LANGUAGES = listOf("en", "vi")

/** Language we actually support for a device language tag; anything else falls back to English. */
fun supportedLanguageOrDefault(language: String): String =
    if (language in SUPPORTED_LANGUAGES) language else "en"

/** Valid ISO currency codes from an FX table: USD first, then alphabetical. */
fun supportedCurrencies(rates: Map<String, Double>): List<String> {
    val valid = rates.keys.filter { code ->
        runCatching { Currency.getInstance(code) }.isSuccess
    }
    return (valid - "USD").sorted().let { listOf("USD") + it }
}

fun defaultCurrencyFor(language: String, available: List<String>): String =
    if (language == "vi" && "VND" in available) "VND" else "USD"
