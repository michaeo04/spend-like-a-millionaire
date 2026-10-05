package com.michaeo04.spendlikeamillionaire.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.michaeo04.spendlikeamillionaire.AppContainer
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.ui.cart.CartRoute
import com.michaeo04.spendlikeamillionaire.ui.shop.ShopRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Shop : Route
    @Serializable data object Cart : Route
    @Serializable data object Settings : Route
}

/** Onboarding until the user finished it and has a person selected. */
fun startRoute(settings: Settings): Route =
    if (settings.onboarded && settings.personId != null) Route.Shop else Route.Onboarding

@Composable
fun AppNav(container: AppContainer) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    val loaded = settings ?: run {
        Box(Modifier.fillMaxSize())
        return
    }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startRoute(loaded)) {
        composable<Route.Onboarding> {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            // TEMP until Task 7 replaces this screen with the real onboarding.
            Placeholder("Onboarding") {
                scope.launch {
                    container.settingsStore.update { it.copy(onboarded = true, personId = "p_musk") }
                    nav.navigate(Route.Shop)
                }
            }
        }
        composable<Route.Shop> {
            ShopRoute(
                container = container,
                onOpenCart = { nav.navigate(Route.Cart) },
                onOpenSettings = { nav.navigate(Route.Settings) },
            )
        }
        composable<Route.Cart> { CartRoute(container, onBack = { nav.popBackStack() }) }
        composable<Route.Settings> { Placeholder("Settings") { nav.popBackStack() } }
    }
}

@Composable
private fun Placeholder(title: String, onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onNext) { Text("Next") }
    }
}
