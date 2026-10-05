package com.michaeo04.spendlikeamillionaire.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.Person
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

const val ONBOARDING_LAST_STEP = 2

data class OnboardingUiState(
    val loading: Boolean = true,
    val step: Int = 0,
    val language: String = "en",
    val currency: String = "USD",
    val personId: String? = null,
    val people: List<Person> = emptyList(),
    val currencies: List<String> = listOf("USD"),
    val rates: Map<String, Double> = mapOf("USD" to 1.0),
) {
    val canFinish: Boolean get() = !loading && personId != null && currency in currencies
}

class OnboardingViewModel(
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
            _state.update {
                it.copy(
                    loading = false,
                    people = people.people(),
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

    fun setPerson(id: String) {
        if (_state.value.people.none { it.id == id }) return
        _state.update { it.copy(personId = id) }
    }

    fun next() = _state.update { it.copy(step = minOf(it.step + 1, ONBOARDING_LAST_STEP)) }

    fun back() = _state.update { it.copy(step = maxOf(it.step - 1, 0)) }

    /** Saves the choices; [onDone] runs after they are persisted. */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canFinish) return
        viewModelScope.launch {
            settingsStore.update { Settings(onboarded = true, personId = s.personId, currency = s.currency, language = s.language) }
            onDone()
        }
    }
}
