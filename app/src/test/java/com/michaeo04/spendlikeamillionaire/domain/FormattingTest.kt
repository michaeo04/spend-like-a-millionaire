package com.michaeo04.spendlikeamillionaire.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormattingTest {
    private val en = Locale.US
    private val vi = Locale.forLanguageTag("vi-VN")

    @Test
    fun percentZeroAndFull() {
        assertEquals("0%", Formatting.percent(0.0, en))
        assertEquals("100%", Formatting.percent(100.0, en))
        assertEquals("100%", Formatting.percent(150.0, en))
    }

    @Test
    fun percentAboveOneUsesTwoDecimalsTrimmed() {
        assertEquals("12.35%", Formatting.percent(12.3456, en))
        assertEquals("5%", Formatting.percent(5.0, en))
        assertEquals("1.5%", Formatting.percent(1.5, en))
    }

    @Test
    fun percentBelowOneKeepsTwoSignificantDigits() {
        assertEquals("0.5%", Formatting.percent(0.5, en))
        assertEquals("0.0004%", Formatting.percent(0.0004, en))
        assertEquals("0.00012%", Formatting.percent(0.000123, en))
    }

    @Test
    fun percentUsesLocaleDecimalSeparator() {
        assertEquals("0,5%", Formatting.percent(0.5, vi))
    }

    @Test
    fun percentBelowOneMillionthIsShownAsLessThan() {
        assertEquals("<0.000001%", Formatting.percent(1e-300, en))
    }

    @Test
    fun percentOfNanOrNegativeIsZero() {
        assertEquals("0%", Formatting.percent(Double.NaN, en))
        assertEquals("0%", Formatting.percent(-3.0, en))
    }

    @Test
    fun moneySmallUsd() {
        assertEquals("$4.50", Formatting.money(450, "USD", 1.0, en))
    }

    @Test
    fun moneyCompactMillionsBillionsTrillions() {
        assertEquals("$1.23M", Formatting.money(123_000_000L, "USD", 1.0, en))
        assertEquals("$914B", Formatting.money(91_400_000_000_000L, "USD", 1.0, en))
        assertEquals("$1.5T", Formatting.money(150_000_000_000_000L, "USD", 1.0, en))
    }

    @Test
    fun moneyCompactVietnameseUnits() {
        val s = Formatting.money(150_000_000_000_000L, "USD", 1.0, vi)
        assertTrue(s, s.contains("1,5") && s.contains("nghìn tỷ"))
        val b = Formatting.money(91_400_000_000_000L - 91_000_000_000_000L + 100_000_000_000L, "USD", 1.0, vi)
        assertTrue(b, b.contains("tỷ"))
    }

    @Test
    fun moneyWithoutRateFallsBackToUsd() {
        assertEquals("$4.50", Formatting.money(450, "VND", null, en))
    }

    @Test
    fun moneyInvalidCurrencyCodeFallsBackToUsd() {
        assertEquals("$4.50", Formatting.money(450, "NOT-A-CODE", 2.0, en))
    }

    @Test
    fun moneyZeroDecimalCurrency() {
        val s = Formatting.money(100, "VND", 25400.0, vi)
        assertTrue(s, s.contains("25.400"))
    }

    @Test
    fun moneyNeverThrowsForExtremeValues() {
        Formatting.money(Long.MAX_VALUE, "VND", 25400.0, vi)
        Formatting.money(0, "USD", 1.0, en)
        Formatting.money(-1, "USD", 1.0, en)
    }
}
