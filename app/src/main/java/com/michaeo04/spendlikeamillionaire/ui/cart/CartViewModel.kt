package com.michaeo04.spendlikeamillionaire.ui.cart

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
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartLineUi(val item: Item, val quantity: Long, val lineTotalCents: Long)

data class CartUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val language: String = "en",
    val currency: String = "USD",
    val rate: Double? = 1.0,
    val balanceCents: Long = 0,
    val totalCents: Long = 0,
    val remainingCents: Long = 0,
    val percentSpent: Double = 0.0,
    val lines: List<CartLineUi> = emptyList(),
)

class CartViewModel(
    private val catalog: CatalogRepository,
    private val people: PeopleRepository,
    private val fx: FxRepository,
    private val settingsStore: SettingsStore,
    private val cartStore: CartStore,
) : ViewModel() {

    private class Loaded(val items: Map<String, Item>, val people: List<Person>, val rates: Map<String, Double>)

    private val loaded = MutableStateFlow<Loaded?>(null)

    val state: StateFlow<CartUiState> = combine(loaded, settingsStore.settings, cartStore.cart) { data, settings, cart ->
        if (data == null) return@combine CartUiState(loading = true, language = settings.language)
        val person = data.people.firstOrNull { it.id == settings.personId }
        val balance = person?.let { Money.usdToCents(it.netWorthUsd) } ?: 0L
        val total = CartMath.total(cart, data.items)
        val lines = cart.lines
            .mapNotNull { (id, qty) ->
                val item = data.items[id]
                if (item == null || qty <= 0) null else CartLineUi(item, qty, CartMath.lineCost(item, qty))
            }
            .sortedWith(compareByDescending<CartLineUi> { it.lineTotalCents }.thenBy { it.item.id })
        CartUiState(
            loading = false,
            person = person,
            language = settings.language,
            currency = settings.currency,
            rate = data.rates[settings.currency],
            balanceCents = balance,
            totalCents = total,
            remainingCents = (balance - total).coerceAtLeast(0),
            percentSpent = Money.percentSpent(total, balance),
            lines = lines,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CartUiState())

    init {
        viewModelScope.launch {
            loaded.value = Loaded(catalog.items().associateBy { it.id }, people.people(), fx.rates())
        }
    }

    fun clearCart() {
        viewModelScope.launch { cartStore.save(Cart()) }
    }

    fun removeLine(itemId: String) {
        viewModelScope.launch {
            val cart = cartStore.cart.first()
            cartStore.save(Cart(cart.lines - itemId))
        }
    }
}
