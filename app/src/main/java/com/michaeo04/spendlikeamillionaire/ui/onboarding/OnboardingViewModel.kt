package com.michaeo04.spendlikeamillionaire.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeo04.spendlikeamillionaire.data.CatalogRepository
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.Person
import com.michaeo04.spendlikeamillionaire.domain.PersonGroup
import com.michaeo04.spendlikeamillionaire.domain.SUPPORTED_LANGUAGES
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import com.michaeo04.spendlikeamillionaire.domain.defaultCurrencyFor
import com.michaeo04.spendlikeamillionaire.domain.supportedCurrencies
import com.michaeo04.spendlikeamillionaire.domain.supportedLanguageOrDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Steps: 0 welcome, 1 language, 2 currency, 3 person. */
const val ONBOARDING_LAST_STEP = 3

/** Items shown on the welcome screen, in this order (those missing from the catalog are skipped). */
private val FEATURED_IDS = listOf(
    "burger", "earbuds", "electric_car", "private_jet", "mansion",
    "superyacht", "rocket_launch", "old_trafford", "moon_flyby", "space_station",
)

data class OnboardingUiState(
    val loading: Boolean = true,
    val step: Int = 0,
    val language: String = "en",
    val currency: String = "USD",
    val personId: String? = null,
    val people: List<Person> = emptyList(),
    val currencies: List<String> = listOf("USD"),
    val rates: Map<String, Double> = mapOf("USD" to 1.0),
    val featured: List<Item> = emptyList(),
    /** null = show everyone; otherwise only this group is listed on the person step. */
    val personGroup: PersonGroup? = null,
    /** Price of a Big Mac in USD cents, used for the "= N Big Macs" fun fact. */
    val bigMacCents: Long? = null,
) {
    val canFinish: Boolean get() = !loading && personId != null && currency in currencies
    val visiblePeople: List<Person> get() = people.filter { personGroup == null || it.group == personGroup }
}

class OnboardingViewModel(
    private val catalog: CatalogRepository,
    private val people: PeopleRepository,
    private val fx: FxRepository,
    private val settingsStore: SettingsStore,
    deviceLanguage: String,
) : ViewModel() {

    private var currencyTouched = false
    private val _state = MutableStateFlow(OnboardingUiState(language = supportedLanguageOrDefault(deviceLanguage)))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rates = fx.rates()
            val currencies = supportedCurrencies(rates)
            val items = catalog.items().associateBy { it.id }
            _state.update {
                it.copy(
                    loading = false,
                    featured = FEATURED_IDS.mapNotNull(items::get),
                    bigMacCents = items["burger"]?.priceCents,
                    people = people.people()
                        .sortedWith(compareBy<Person> { p -> p.group.ordinal }.thenByDescending { p -> p.netWorthUsd }),
                    currencies = currencies,
                    rates = rates,
                    currency = defaultCurrencyFor(it.language, currencies),
                )
            }
        }
    }

    fun setLanguage(language: String) {
        if (language !in SUPPORTED_LANGUAGES) return
        _state.update {
            it.copy(
                language = language,
                currency = if (currencyTouched) it.currency else defaultCurrencyFor(language, it.currencies),
            )
        }
    }

    fun setCurrency(code: String) {
        if (code !in _state.value.currencies) return
        currencyTouched = true
        _state.update { it.copy(currency = code) }
    }

    /** Filters the person list; a selected person the new filter hides is deselected so no choice stays invisible. */
    fun setPersonGroup(group: PersonGroup?) = _state.update { s ->
        val selected = s.people.firstOrNull { it.id == s.personId }
        val stillVisible = selected == null || group == null || selected.group == group
        s.copy(personGroup = group, personId = if (stillVisible) s.personId else null)
    }

    fun setPerson(id: String) {
        if (_state.value.people.none { it.id == id }) return
        _state.update { it.copy(personId = id) }
    }

    fun next() = _state.update { it.copy(step = minOf(it.step + 1, ONBOARDING_LAST_STEP)) }

    fun back() = _state.update { it.copy(step = maxOf(it.step - 1, 0)) }

    private var finishing = false

    /** Saves the choices; [onDone] runs once, after they are persisted (double taps are ignored). */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canFinish || finishing) return
        finishing = true
        viewModelScope.launch {
            settingsStore.update { Settings(onboarded = true, personId = s.personId, currency = s.currency, language = s.language) }
            onDone()
        }
    }
}
