package com.michaeo04.spendlikeamillionaire

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.michaeo04.spendlikeamillionaire.domain.validCurrencyOrDefault
import com.michaeo04.spendlikeamillionaire.platform.AppLanguage
import com.michaeo04.spendlikeamillionaire.ui.nav.AppNav
import com.michaeo04.spendlikeamillionaire.ui.settings.LocaleAction
import com.michaeo04.spendlikeamillionaire.ui.settings.localeSyncAction
import com.michaeo04.spendlikeamillionaire.ui.theme.SpendTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as App).container
        syncLanguage(container)
        setContent {
            SpendTheme {
                AppNav(container)
            }
        }
    }

    /** Keeps saved language/currency valid and in step with the system per-app language (system wins). */
    private fun syncLanguage(container: AppContainer) {
        lifecycleScope.launch {
            val settings = container.settingsStore.settings.first()
            val validCurrency = validCurrencyOrDefault(settings.currency, settings.language, container.fx.rates())
            if (settings.onboarded && validCurrency != settings.currency) {
                container.settingsStore.update { it.copy(currency = validCurrency) }
            }
            val action = localeSyncAction(
                settingsLanguage = settings.language,
                appLocaleLanguage = AppLanguage.current(),
                onboarded = settings.onboarded,
                systemManagesLocale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                deviceLanguage = resources.configuration.locales[0].language,
            )
            when (action) {
                LocaleAction.None -> Unit
                is LocaleAction.ApplyToApp -> AppLanguage.apply(action.language)
                is LocaleAction.AdoptFromApp ->
                    container.settingsStore.update { it.copy(language = action.language) }
            }
        }
    }
}
