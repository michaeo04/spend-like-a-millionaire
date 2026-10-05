package com.michaeo04.spendlikeamillionaire.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Thin wrapper over AppCompat per-app locales. */
object AppLanguage {
    /** Language currently applied to the app, or null when following the system. */
    fun current(): String? {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) null else locales[0]?.language
    }

    fun apply(language: String) {
        if (current() != language) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
    }
}
