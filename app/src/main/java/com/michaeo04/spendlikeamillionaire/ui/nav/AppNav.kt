package com.michaeo04.spendlikeamillionaire.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.platform.AppLanguage
import com.michaeo04.spendlikeamillionaire.ui.cart.CartRoute
import com.michaeo04.spendlikeamillionaire.ui.onboarding.OnboardingRoute
import com.michaeo04.spendlikeamillionaire.ui.settings.CreditsRoute
import com.michaeo04.spendlikeamillionaire.ui.settings.SettingsRoute
import com.michaeo04.spendlikeamillionaire.ui.shop.ShopRoute
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Shop : Route
    @Serializable data object Cart : Route
    @Serializable data object Settings : Route
    @Serializable data object Credits : Route
}

/** Onboarding until the user finished it and still has a valid person (it may vanish in an update). */
fun startRoute(settings: Settings, knownPersonIds: Set<String>): Route =
    if (settings.onboarded && settings.personId != null && settings.personId in knownPersonIds) {
        Route.Shop
    } else {
        Route.Onboarding
    }

@Composable
fun AppNav(container: AppContainer) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    val knownPersonIds by produceState<Set<String>?>(initialValue = null) {
        value = container.people.people().map { it.id }.toSet()
    }
    val loaded = settings
    val personIds = knownPersonIds
    if (loaded == null || personIds == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    val deviceLanguage = LocalConfiguration.current.locales[0].language
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startRoute(loaded, personIds)) {
        composable<Route.Onboarding> {
            OnboardingRoute(
                container = container,
                deviceLanguage = deviceLanguage,
                onLanguageChosen = AppLanguage::apply,
                onDone = {
                    nav.navigate(Route.Shop) { popUpTo(Route.Onboarding) { inclusive = true } }
                },
            )
        }
        composable<Route.Shop> {
            ShopRoute(
                container = container,
                onOpenCart = { nav.navigate(Route.Cart) },
                onOpenSettings = { nav.navigate(Route.Settings) },
            )
        }
        composable<Route.Cart> { CartRoute(container, onBack = { nav.popBackStack() }) }
        composable<Route.Settings> {
            SettingsRoute(
                container = container,
                onLanguageChosen = AppLanguage::apply,
                onOpenCredits = { nav.navigate(Route.Credits) },
                onBack = { nav.popBackStack() },
            )
        }
        composable<Route.Credits> { CreditsRoute(container, onBack = { nav.popBackStack() }) }
    }
}
