package com.michaeo04.spendlikeamillionaire.ui.nav

import com.michaeo04.spendlikeamillionaire.domain.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {
    private val known = setOf("p_musk", "p_bezos")

    @Test
    fun firstLaunchStartsOnboarding() {
        assertEquals(Route.Onboarding, startRoute(Settings(onboarded = false), known))
    }

    @Test
    fun onboardedWithKnownPersonStartsShop() {
        assertEquals(Route.Shop, startRoute(Settings(onboarded = true, personId = "p_musk"), known))
    }

    @Test
    fun onboardedButPersonMissingRestartsOnboarding() {
        assertEquals(Route.Onboarding, startRoute(Settings(onboarded = true, personId = null), known))
    }

    @Test
    fun savedPersonRemovedFromTheAppAfterAnUpdateRestartsOnboarding() {
        assertEquals(Route.Onboarding, startRoute(Settings(onboarded = true, personId = "p_gone"), known))
    }
}
