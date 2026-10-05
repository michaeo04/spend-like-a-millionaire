package com.michaeo04.spendlikeamillionaire.ui.nav

import com.michaeo04.spendlikeamillionaire.domain.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {
    @Test
    fun firstLaunchStartsOnboarding() {
        assertEquals(Route.Onboarding, startRoute(Settings(onboarded = false)))
    }

    @Test
    fun onboardedWithPersonStartsShop() {
        assertEquals(Route.Shop, startRoute(Settings(onboarded = true, personId = "p_musk")))
    }

    @Test
    fun onboardedButPersonMissingRestartsOnboarding() {
        // e.g. a person was removed from the catalog in an app update
        assertEquals(Route.Onboarding, startRoute(Settings(onboarded = true, personId = null)))
    }
}
