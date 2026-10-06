package com.michaeo04.spendlikeamillionaire.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrenciesTest {
    private val rates = mapOf("USD" to 1.0, "VND" to 25_000.0, "EUR" to 0.9, "NOT-A-CODE" to 2.0)

    @Test
    fun knownCurrencyIsKept() {
        assertEquals("EUR", validCurrencyOrDefault("EUR", "en", rates))
    }

    @Test
    fun currencyMissingFromTheRateTableFallsBackToTheLanguageDefault() {
        assertEquals("VND", validCurrencyOrDefault("JPY", "vi", rates))
        assertEquals("USD", validCurrencyOrDefault("JPY", "en", rates))
    }

    @Test
    fun invalidCurrencyCodeFallsBack() {
        assertEquals("USD", validCurrencyOrDefault("NOT-A-CODE", "en", rates))
    }
}
