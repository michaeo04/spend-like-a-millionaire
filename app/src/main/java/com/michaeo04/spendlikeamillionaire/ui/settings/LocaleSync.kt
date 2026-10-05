package com.michaeo04.spendlikeamillionaire.ui.settings

import com.michaeo04.spendlikeamillionaire.domain.SUPPORTED_LANGUAGES
import com.michaeo04.spendlikeamillionaire.domain.supportedLanguageOrDefault

sealed interface LocaleAction {
    data object None : LocaleAction
    data class ApplyToApp(val language: String) : LocaleAction
    data class AdoptFromApp(val language: String) : LocaleAction
}

/**
 * Keeps the saved language and the app's per-app locale in step.
 *
 * - If the user changed the language in system settings (Android 13+), the system choice wins.
 * - A null app locale on Android 13+ means "System default": follow the device language.
 * - On older Android versions a null app locale only means nothing was stored yet, so the saved
 *   language is applied.
 */
fun localeSyncAction(
    settingsLanguage: String,
    appLocaleLanguage: String?,
    onboarded: Boolean,
    systemManagesLocale: Boolean,
    deviceLanguage: String,
): LocaleAction = when {
    !onboarded -> LocaleAction.None
    appLocaleLanguage == null -> if (!systemManagesLocale) {
        LocaleAction.ApplyToApp(settingsLanguage)
    } else {
        val device = supportedLanguageOrDefault(deviceLanguage)
        if (device == settingsLanguage) LocaleAction.None else LocaleAction.AdoptFromApp(device)
    }
    appLocaleLanguage !in SUPPORTED_LANGUAGES -> LocaleAction.None
    appLocaleLanguage == settingsLanguage -> LocaleAction.None
    else -> LocaleAction.AdoptFromApp(appLocaleLanguage)
}
