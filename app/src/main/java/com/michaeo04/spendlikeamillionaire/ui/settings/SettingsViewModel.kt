package com.michaeo04.spendlikeamillionaire.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeo04.spendlikeamillionaire.data.CatalogRepository
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.CartMath
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.Money
import com.michaeo04.spendlikeamillionaire.domain.Person
import com.michaeo04.spendlikeamillionaire.domain.SUPPORTED_LANGUAGES
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import com.michaeo04.spendlikeamillionaire.domain.supportedCurrencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val language: String = "en",
    val currency: String = "USD",
    val rates: Map<String, Double> = mapOf("USD" to 1.0),
    val person: Person? = null,
    val people: List<Person> = emptyList(),
    val currencies: List<String> = listOf("USD"),
    val cartTotalCents: Long = 0,
    /** Set when switching would leave the cart larger than the new person's fortune. */
    val pendingPerson: Person? = null,
) {
    val pendingPersonId: String? get() = pendingPerson?.id
}

class SettingsViewModel(
    private val catalog: CatalogRepository,
    private val people: PeopleRepository,
    private val fx: FxRepository,
    private val settingsStore: SettingsStore,
    private val cartStore: CartStore,
) : ViewModel() {

    private class Loaded(val items: Map<String, Item>, val people: List<Person>, val rates: Map<String, Double>) {
        val currencies = supportedCurrencies(rates)
    }

    private val loaded = MutableStateFlow<Loaded?>(null)
    private val pendingId = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        loaded, settingsStore.settings, cartStore.cart, pendingId,
    ) { data, settings, cart, pending ->
        if (data == null) return@combine SettingsUiState(loading = true, language = settings.language)
        SettingsUiState(
            loading = false,
            language = settings.language,
            currency = settings.currency,
            rates = data.rates,
            person = data.people.firstOrNull { it.id == settings.personId },
            people = data.people,
            currencies = data.currencies,
            cartTotalCents = CartMath.total(cart, data.items),
            pendingPerson = data.people.firstOrNull { it.id == pending },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    init {
        viewModelScope.launch {
            loaded.value = Loaded(catalog.items().associateBy { it.id }, people.people(), fx.rates())
        }
    }

    fun setLanguage(language: String) {
        if (language !in SUPPORTED_LANGUAGES) return
        viewModelScope.launch { settingsStore.update { it.copy(language = language) } }
    }

    fun setCurrency(code: String) {
        val data = loaded.value ?: return
        if (code !in data.currencies) return
        viewModelScope.launch { settingsStore.update { it.copy(currency = code) } }
    }

    /** Applies directly unless the cart would no longer fit; then waits for [confirmPersonChange]. */
    fun requestPersonChange(personId: String) {
        val data = loaded.value ?: return
        val target = data.people.firstOrNull { it.id == personId } ?: return
        viewModelScope.launch {
            if (settingsStore.settings.first().personId == personId) return@launch
            val total = CartMath.total(cartStore.cart.first(), data.items)
            if (total > Money.usdToCents(target.netWorthUsd)) {
                pendingId.value = personId
            } else {
                settingsStore.update { it.copy(personId = personId) }
            }
        }
    }

    fun confirmPersonChange() {
        val id = pendingId.value ?: return
        viewModelScope.launch {
            cartStore.save(Cart())
            settingsStore.update { it.copy(personId = id) }
            pendingId.value = null
        }
    }

    fun cancelPersonChange() {
        pendingId.value = null
    }
}
