package com.michaeo04.spendlikeamillionaire.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleSyncTest {
    @Test
    fun beforeOnboardingNothingIsSynced() {
        assertEquals(LocaleAction.None, localeSyncAction("en", "vi", onboarded = false))
    }

    @Test
    fun matchingLocalesNeedNoAction() {
        assertEquals(LocaleAction.None, localeSyncAction("vi", "vi", onboarded = true))
    }

    @Test
    fun noAppLocaleYetMeansApplyTheSavedLanguage() {
        assertEquals(LocaleAction.ApplyToApp("vi"), localeSyncAction("vi", null, onboarded = true))
    }

    @Test
    fun systemPerAppLanguageChangeWinsAndIsAdopted() {
        assertEquals(LocaleAction.AdoptFromApp("en"), localeSyncAction("vi", "en", onboarded = true))
    }

    @Test
    fun unsupportedAppLanguageIsIgnored() {
        assertEquals(LocaleAction.None, localeSyncAction("en", "fr", onboarded = true))
    }
}
