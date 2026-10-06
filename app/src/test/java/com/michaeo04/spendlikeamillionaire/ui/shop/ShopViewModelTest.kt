package com.michaeo04.spendlikeamillionaire.ui.shop

import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.Category
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.testing.FakeCartStore
import com.michaeo04.spendlikeamillionaire.testing.FakeCatalog
import com.michaeo04.spendlikeamillionaire.testing.FakeFx
import com.michaeo04.spendlikeamillionaire.testing.FakePeople
import com.michaeo04.spendlikeamillionaire.testing.FakeSettingsStore
import com.michaeo04.spendlikeamillionaire.testing.testItem
import com.michaeo04.spendlikeamillionaire.testing.testPerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val cheap = testItem("cheap", 10_000, en = "Coffee", vi = "Cà phê", category = Category.FOOD)
    private val mid = testItem("mid", 30_000, en = "Sneakers", vi = "Giày", category = Category.SHOPPING)
    private val pricey = testItem("pricey", 90_000, en = "Phone", vi = "Điện thoại", category = Category.TECH)

    private fun vm(
        items: List<com.michaeo04.spendlikeamillionaire.domain.Item> = listOf(pricey, cheap, mid),
        netWorthUsd: Long = 1_000, // = 100_000 cents
        settings: Settings = Settings(onboarded = true, personId = "p1", currency = "USD", language = "en"),
        cart: Cart = Cart(),
        cartStore: FakeCartStore = FakeCartStore(cart),
    ) = ShopViewModel(
        catalog = FakeCatalog(items),
        people = FakePeople(listOf(testPerson("p1", netWorthUsd))),
        fx = FakeFx(mapOf("USD" to 1.0, "VND" to 25_000.0)),
        settingsStore = FakeSettingsStore(settings),
        cartStore = cartStore,
    )

    @Test
    fun sortsByPriceAscendingThenDescending() = runTest {
        val vm = vm()
        assertEquals(listOf("cheap", "mid", "pricey"), vm.state.value.items.map { it.item.id })
        vm.setSort(SortOrder.PRICE_DESC)
        assertEquals(listOf("pricey", "mid", "cheap"), vm.state.value.items.map { it.item.id })
    }

    @Test
    fun filtersByCategory() = runTest {
        val vm = vm()
        vm.setCategory(Category.TECH)
        assertEquals(listOf("pricey"), vm.state.value.items.map { it.item.id })
        vm.setCategory(null)
        assertEquals(3, vm.state.value.items.size)
    }

    @Test
    fun searchIsCaseAndAccentInsensitiveAndAlsoMatchesEnglish() = runTest {
        val vm = vm(settings = Settings(true, "p1", "USD", "vi"))
        vm.setQuery("CA PHE")
        assertEquals(listOf("cheap"), vm.state.value.items.map { it.item.id })
        vm.setQuery("sneak") // English name while UI language is Vietnamese
        assertEquals(listOf("mid"), vm.state.value.items.map { it.item.id })
        vm.setQuery("dien thoai")
        assertEquals(listOf("pricey"), vm.state.value.items.map { it.item.id })
    }

    @Test
    fun balanceIsNetWorthInCentsAndExposesCurrency() = runTest {
        val vm = vm(settings = Settings(true, "p1", "VND", "en"))
        assertEquals(100_000L, vm.state.value.balanceCents)
        assertEquals("VND", vm.state.value.currency)
        assertEquals(25_000.0, vm.state.value.rate!!, 0.0)
    }

    @Test
    fun setQuantityIsClampedToAffordableAndPersisted() = runTest {
        val store = FakeCartStore()
        val vm = vm(cartStore = store)
        vm.setQuantity("mid", 99) // 100_000 / 30_000 = 3
        assertEquals(mapOf("mid" to 3L), store.state.value.lines)
        assertEquals(3L, vm.state.value.items.first { it.item.id == "mid" }.quantity)
    }

    @Test
    fun setQuantityNegativeOrZeroNeverThrowsAndRemovesLine() = runTest {
        val store = FakeCartStore(Cart(mapOf("mid" to 2L)))
        val vm = vm(cartStore = store)
        vm.setQuantity("mid", -7)
        assertTrue(store.state.value.lines.isEmpty())
    }

    @Test
    fun changeQuantityAppliesDeltasOnTopOfTheCommittedQuantity() = runTest {
        val store = FakeCartStore()
        val vm = vm(cartStore = store)
        vm.changeQuantity("cheap", 1)
        vm.changeQuantity("cheap", 1) // two quick taps on "+" must add up
        assertEquals(2L, store.state.value.lines["cheap"])
        vm.changeQuantity("cheap", -1)
        assertEquals(1L, store.state.value.lines["cheap"])
        vm.changeQuantity("cheap", -5)
        assertTrue(store.state.value.lines.isEmpty())
    }

    @Test
    fun changeQuantityNeverExceedsWhatIsAffordable() = runTest {
        val store = FakeCartStore()
        val vm = vm(cartStore = store)
        vm.changeQuantity("pricey", 5) // only 1 affordable (90_000 of 100_000)
        assertEquals(1L, store.state.value.lines["pricey"])
    }

    @Test
    fun theMaxQuantityShownToTheDialogIsExactlyWhatSetQuantityAccepts() = runTest {
        val store = FakeCartStore()
        val vm = vm(cartStore = store)
        val max = vm.state.value.items.first { it.item.id == "mid" }.maxQuantity
        vm.setQuantity("mid", max)
        assertEquals(mapOf("mid" to 3L), store.state.value.lines)
        assertEquals(90_000L, vm.state.value.spentCents)
        assertEquals(0L, vm.state.value.items.first { it.item.id == "mid" }.maxQuantity - 3L)
    }

    @Test
    fun maxQuantityAccountsForOtherLines() = runTest {
        val vm = vm(cart = Cart(mapOf("pricey" to 1L)))
        // 100_000 - 90_000 = 10_000 left -> 1 cheap, 0 mid
        assertEquals(1L, vm.state.value.items.first { it.item.id == "cheap" }.maxQuantity)
        assertEquals(0L, vm.state.value.items.first { it.item.id == "mid" }.maxQuantity)
    }

    @Test
    fun percentSpentReflectsTheCart() = runTest {
        val vm = vm(cart = Cart(mapOf("cheap" to 5L))) // 50_000 of 100_000
        assertEquals(50.0, vm.state.value.percentSpent, 1e-9)
    }

    @Test
    fun storedCartWithUnknownItemIsCleanedUp() = runTest {
        val store = FakeCartStore(Cart(mapOf("cheap" to 1L, "removed_in_update" to 5L)))
        vm(cartStore = store)
        assertEquals(mapOf("cheap" to 1L), store.state.value.lines)
    }

    @Test
    fun storedCartLargerThanBalanceIsTrimmed() = runTest {
        val store = FakeCartStore(Cart(mapOf("pricey" to 5L)))
        val vm = vm(cartStore = store)
        assertTrue(vm.state.value.spentCents <= vm.state.value.balanceCents)
        assertEquals(1L, store.state.value.lines["pricey"])
    }

    @Test
    fun missingPersonYieldsEmptyShopWithoutCrashing() = runTest {
        val vm = vm(settings = Settings(onboarded = true, personId = "ghost"))
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(0L, vm.state.value.balanceCents)
        assertFalse(vm.state.value.loading)
    }
}
