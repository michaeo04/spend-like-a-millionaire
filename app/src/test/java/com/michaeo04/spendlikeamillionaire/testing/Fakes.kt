package com.michaeo04.spendlikeamillionaire.testing

import com.michaeo04.spendlikeamillionaire.data.CatalogRepository
import com.michaeo04.spendlikeamillionaire.data.FxRepository
import com.michaeo04.spendlikeamillionaire.data.PeopleRepository
import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.LocalizedText
import com.michaeo04.spendlikeamillionaire.domain.Person
import com.michaeo04.spendlikeamillionaire.domain.PersonGroup
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

fun testItem(
    id: String,
    priceCents: Long,
    en: String = id,
    vi: String = en,
    category: Category = Category.FUN,
    estimate: Boolean = false,
) = Item(id, category, priceCents, LocalizedText(en, vi), "x", estimate)

fun testPerson(
    id: String = "p1",
    netWorthUsd: Long = 1_000,
    en: String = id,
    group: PersonGroup = PersonGroup.BILLIONAIRE,
) = Person(id, LocalizedText(en, en), netWorthUsd, "Test", "2026-09", "#5B8DEF", group)

class FakeCatalog(private val items: List<Item>) : CatalogRepository {
    override suspend fun items() = items
}

class FakePeople(private val people: List<Person>) : PeopleRepository {
    override suspend fun people() = people
}

class FakeFx(private val rates: Map<String, Double> = mapOf("USD" to 1.0)) : FxRepository {
    override suspend fun rates() = rates
}

class FakeSettingsStore(initial: Settings = Settings(onboarded = true, personId = "p1")) : SettingsStore {
    val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state
    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value)
    }
}

class FakeCartStore(initial: Cart = Cart()) : CartStore {
    val state = MutableStateFlow(initial)
    override val cart: Flow<Cart> = state
    override suspend fun save(cart: Cart) {
        state.value = cart
    }
}
