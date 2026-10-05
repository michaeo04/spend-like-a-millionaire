package com.michaeo04.spendlikeamillionaire.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Display formatting. Pure JVM so it is unit-testable without Android. */
object Formatting {
    private class Unit(val size: BigDecimal, val en: String, val vi: String)

    // Largest first. Vietnamese counts in "triệu" (1e6), "tỷ" (1e9), "nghìn tỷ" (1e12), ...
    private val UNITS = listOf(
        Unit(BigDecimal("1000000000000000000"), "Qi", " tỷ tỷ"),
        Unit(BigDecimal("1000000000000000"), "Qa", " triệu tỷ"),
        Unit(BigDecimal("1000000000000"), "T", " nghìn tỷ"),
        Unit(BigDecimal("1000000000"), "B", " tỷ"),
        Unit(BigDecimal("1000000"), "M", " triệu"),
    )

    fun percent(p: Double, locale: Locale): String {
        if (p.isNaN() || p <= 0.0) return "0%"
        if (p >= 100.0) return "100%"
        if (p < 1e-6) return localize("<0.000001", locale) + "%"
        val value = BigDecimal(p)
        val rounded = if (p >= 1.0) {
            value.setScale(2, RoundingMode.HALF_UP)
        } else {
            value.round(MathContext(2, RoundingMode.HALF_UP))
        }
        return localize(rounded.stripTrailingZeros().toPlainString(), locale) + "%"
    }

    /**
     * Formats USD cents in [currency] using [rate] (units of currency per 1 USD).
     * Unknown/invalid currency or missing rate falls back to USD. Values of one million major
     * units or more are shown compactly ("$1.5T", "1,5 nghìn tỷ").
     */
    fun money(usdCents: Long, currency: String, rate: Double?, locale: Locale): String {
        val (cur, effectiveRate) = resolve(currency, rate)
        val major = BigDecimal.valueOf(usdCents).movePointLeft(2).multiply(BigDecimal.valueOf(effectiveRate))
        val unit = UNITS.firstOrNull { major.abs() >= it.size }
        if (unit != null) {
            val number = compactNumber(major.divide(unit.size, 2, RoundingMode.HALF_UP), locale)
            val symbol = cur.getSymbol(locale)
            return if (locale.language == "vi") "$number${unit.vi} $symbol" else "$symbol$number${unit.en}"
        }
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = cur
        // setCurrency alone keeps the locale's own fraction digits (e.g. ",00" for VND).
        val digits = cur.defaultFractionDigits.coerceAtLeast(0)
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return format.format(major)
    }

    private fun compactNumber(value: BigDecimal, locale: Locale): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(value)
    }

    private fun resolve(code: String, rate: Double?): Pair<Currency, Double> {
        val usd = Currency.getInstance("USD")
        if (code == "USD") return usd to 1.0
        if (rate == null || rate.isNaN() || rate <= 0.0) return usd to 1.0
        return try {
            Currency.getInstance(code) to rate
        } catch (e: IllegalArgumentException) {
            usd to 1.0
        }
    }

    private fun localize(plain: String, locale: Locale): String =
        plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}
