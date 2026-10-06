package com.michaeo04.spendlikeamillionaire.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaeo04.spendlikeamillionaire.data.CatalogRepository
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.CartMath
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.Money
import com.michaeo04.spendlikeamillionaire.domain.Person
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import com.michaeo04.spendlikeamillionaire.domain.foldForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortOrder { PRICE_ASC, PRICE_DESC }

data class ItemUi(val item: Item, val quantity: Long, val maxQuantity: Long)

data class ShopUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val language: String = "en",
    val currency: String = "USD",
    val rate: Double? = 1.0,
    val balanceCents: Long = 0,
    val spentCents: Long = 0,
    val percentSpent: Double = 0.0,
    val cartLineCount: Int = 0,
    val items: List<ItemUi> = emptyList(),
    val query: String = "",
    val category: Category? = null,
    val sort: SortOrder = SortOrder.PRICE_ASC,
)

class ShopViewModel(
    private val catalog: CatalogRepository,
    private val people: PeopleRepository,
    private val fx: FxRepository,
    private val settingsStore: SettingsStore,
    private val cartStore: CartStore,
) : ViewModel() {

    private data class Filters(
        val query: String = "",
        val category: Category? = null,
        val sort: SortOrder = SortOrder.PRICE_ASC,
    )

    private class Loaded(val items: List<Item>, val people: List<Person>, val rates: Map<String, Double>) {
        val byId: Map<String, Item> = items.associateBy { it.id }
    }

    private val filters = MutableStateFlow(Filters())
    private val loaded = MutableStateFlow<Loaded?>(null)
    private val writeLock = Mutex()

    val state: StateFlow<ShopUiState> = combine(
        loaded,
        settingsStore.settings,
        cartStore.cart,
        filters,
    ) { data, settings, cart, f ->
        if (data == null) return@combine ShopUiState(loading = true, language = settings.language)
        val person = data.people.firstOrNull { it.id == settings.personId }
        val balance = person?.let { Money.usdToCents(it.netWorthUsd) } ?: 0L
        val spent = CartMath.total(cart, data.byId)
        val query = foldForSearch(f.query.trim())
        val visible = if (person == null) emptyList() else data.items
            .asSequence()
            .filter { f.category == null || it.category == f.category }
            .filter {
                query.isEmpty() ||
                    foldForSearch(it.name.get(settings.language)).contains(query) ||
                    foldForSearch(it.name.en).contains(query)
            }
            .sortedWith(
                when (f.sort) {
                    SortOrder.PRICE_ASC -> compareBy<Item> { it.priceCents }.thenBy { it.id }
                    SortOrder.PRICE_DESC -> compareByDescending<Item> { it.priceCents }.thenBy { it.id }
                },
            )
            .map { item ->
                val quantity = cart.lines[item.id]?.coerceAtLeast(0) ?: 0L
                val others = spent - minOf(spent, CartMath.lineCost(item, quantity))
                ItemUi(item, quantity, Money.maxQuantity(balance - others, item.priceCents))
            }
            .toList()
        ShopUiState(
            loading = false,
            person = person,
            language = settings.language,
            currency = settings.currency,
            rate = data.rates[settings.currency],
            balanceCents = balance,
            spentCents = spent,
            percentSpent = Money.percentSpent(spent, balance),
            cartLineCount = cart.lines.count { (id, q) -> q > 0 && id in data.byId },
            items = visible,
            query = f.query,
            category = f.category,
            sort = f.sort,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShopUiState())

    init {
        viewModelScope.launch {
            loaded.value = Loaded(catalog.items(), people.people(), fx.rates())
            cleanUpStoredCart()
        }
    }

    fun setQuery(query: String) = filters.update { it.copy(query = query) }

    fun setCategory(category: Category?) = filters.update { it.copy(category = category) }

    fun setSort(sort: SortOrder) = filters.update { it.copy(sort = sort) }

    fun setQuantity(itemId: String, quantity: Long) = updateQuantity(itemId) { quantity }

    /** Adds [delta] to the committed quantity, so quick repeated taps on +/- always add up. */
    fun changeQuantity(itemId: String, delta: Long) = updateQuantity(itemId) { current ->
        if (delta > 0 && current > Long.MAX_VALUE - delta) Long.MAX_VALUE else current + delta
    }

    private fun updateQuantity(itemId: String, requested: (current: Long) -> Long) {
        viewModelScope.launch {
            writeLock.withLock {
                val data = loaded.value ?: return@withLock
                val item = data.byId[itemId] ?: return@withLock
                val person = currentPerson(data) ?: return@withLock
                val cart = cartStore.cart.first()
                val next = CartMath.setQuantity(
                    cart, item, requested(cart.lines[itemId] ?: 0L), Money.usdToCents(person.netWorthUsd), data.byId,
                )
                if (next != cart) cartStore.save(next)
            }
        }
    }

    private suspend fun currentPerson(data: Loaded): Person? {
        val id = settingsStore.settings.first().personId
        return data.people.firstOrNull { it.id == id }
    }

    /** Drops lines for items that no longer exist and trims a cart that exceeds the balance. */
    private suspend fun cleanUpStoredCart() = writeLock.withLock {
        val data = loaded.value ?: return@withLock
        val person = currentPerson(data) ?: return@withLock
        val cart: Cart = cartStore.cart.first()
        val clean = CartMath.sanitize(cart, data.byId, Money.usdToCents(person.netWorthUsd))
        if (clean != cart) cartStore.save(clean)
    }
}
