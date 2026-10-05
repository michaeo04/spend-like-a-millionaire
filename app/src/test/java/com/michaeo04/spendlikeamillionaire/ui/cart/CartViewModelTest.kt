package com.michaeo04.spendlikeamillionaire.ui.cart

import com.michaeo04.spendlikeamillionaire.domain.Cart
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val a = testItem("a", 10_000)
    private val b = testItem("b", 30_000)

    private fun vm(cart: Cart, store: FakeCartStore = FakeCartStore(cart), personId: String? = "p1") = CartViewModel(
        catalog = FakeCatalog(listOf(a, b)),
        people = FakePeople(listOf(testPerson("p1", 1_000))), // 100_000 cents
        fx = FakeFx(),
        settingsStore = FakeSettingsStore(Settings(onboarded = true, personId = personId)),
        cartStore = store,
    )

    @Test
    fun linesAreSortedByLineTotalDescending() = runTest {
        val s = vm(Cart(mapOf("a" to 2L, "b" to 1L))).state.value // a=20_000, b=30_000
        assertEquals(listOf("b", "a"), s.lines.map { it.item.id })
        assertEquals(listOf(30_000L, 20_000L), s.lines.map { it.lineTotalCents })
    }

    @Test
    fun totalsRemainingAndPercent() = runTest {
        val s = vm(Cart(mapOf("a" to 2L, "b" to 1L))).state.value
        assertEquals(50_000L, s.totalCents)
        assertEquals(100_000L, s.balanceCents)
        assertEquals(50_000L, s.remainingCents)
        assertEquals(50.0, s.percentSpent, 1e-9)
    }

    @Test
    fun emptyCartHasFullBalanceLeft() = runTest {
        val s = vm(Cart()).state.value
        assertTrue(s.lines.isEmpty())
        assertEquals(0L, s.totalCents)
        assertEquals(100_000L, s.remainingCents)
    }

    @Test
    fun unknownItemsInTheStoredCartAreIgnored() = runTest {
        val s = vm(Cart(mapOf("a" to 1L, "ghost" to 9L))).state.value
        assertEquals(listOf("a"), s.lines.map { it.item.id })
        assertEquals(10_000L, s.totalCents)
    }

    @Test
    fun clearCartEmptiesTheStore() = runTest {
        val store = FakeCartStore(Cart(mapOf("a" to 2L)))
        vm(Cart(), store).clearCart()
        assertTrue(store.state.value.lines.isEmpty())
    }

    @Test
    fun removeLineRemovesOnlyThatLine() = runTest {
        val store = FakeCartStore(Cart(mapOf("a" to 2L, "b" to 1L)))
        vm(Cart(), store).removeLine("a")
        assertEquals(mapOf("b" to 1L), store.state.value.lines)
    }

    @Test
    fun missingPersonGivesZeroBalanceAndNoCrash() = runTest {
        val s = vm(Cart(mapOf("a" to 1L)), personId = "ghost").state.value
        assertEquals(0L, s.balanceCents)
        assertEquals(0L, s.remainingCents)
    }
}
