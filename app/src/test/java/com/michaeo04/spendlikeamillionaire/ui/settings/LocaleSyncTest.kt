package com.michaeo04.spendlikeamillionaire.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleSyncTest {
    private fun action(
        saved: String,
        app: String?,
        onboarded: Boolean = true,
        systemManages: Boolean = true,
        device: String = "en",
    ) = localeSyncAction(saved, app, onboarded, systemManages, device)

    @Test
    fun beforeOnboardingNothingIsSynced() {
        assertEquals(LocaleAction.None, action("en", "vi", onboarded = false))
    }

    @Test
    fun matchingLocalesNeedNoAction() {
        assertEquals(LocaleAction.None, action("vi", "vi"))
    }

    @Test
    fun oldAndroidWithoutAppLocaleAppliesTheSavedLanguage() {
        assertEquals(LocaleAction.ApplyToApp("vi"), action("vi", null, systemManages = false))
    }

    @Test
    fun systemPerAppLanguageChangeWinsAndIsAdopted() {
        assertEquals(LocaleAction.AdoptFromApp("en"), action("vi", "en"))
    }

    @Test
    fun systemDefaultChoiceIsHonoredOnAndroid13Plus() {
        // User picked "System default" in system settings: follow the device language.
        assertEquals(LocaleAction.None, action("vi", null, device = "vi"))
        assertEquals(LocaleAction.AdoptFromApp("vi"), action("en", null, device = "vi"))
        assertEquals(LocaleAction.AdoptFromApp("en"), action("vi", null, device = "fr")) // unsupported -> English
    }

    @Test
    fun unsupportedAppLanguageIsIgnored() {
        assertEquals(LocaleAction.None, action("en", "fr"))
    }
}
