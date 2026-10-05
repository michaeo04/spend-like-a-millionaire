package com.michaeo04.spendlikeamillionaire.ui.settings

import com.michaeo04.spendlikeamillionaire.domain.SUPPORTED_LANGUAGES

sealed interface LocaleAction {
    data object None : LocaleAction
    data class ApplyToApp(val language: String) : LocaleAction
    data class AdoptFromApp(val language: String) : LocaleAction
}

/**
 * Keeps the saved language and the app's per-app locale in step. If the user changed the language
 * from system settings (Android 13+), the system choice wins.
 */
fun localeSyncAction(settingsLanguage: String, appLocaleLanguage: String?, onboarded: Boolean): LocaleAction = when {
    !onboarded -> LocaleAction.None
    appLocaleLanguage == null -> LocaleAction.ApplyToApp(settingsLanguage)
    appLocaleLanguage !in SUPPORTED_LANGUAGES -> LocaleAction.None
    appLocaleLanguage == settingsLanguage -> LocaleAction.None
    else -> LocaleAction.AdoptFromApp(appLocaleLanguage)
}
