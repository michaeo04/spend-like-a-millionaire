package com.michaeo04.spendlikeamillionaire.ui.settings

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    // poor = $1,000 (100_000 cents), rich = $10,000 (1_000_000 cents)
    private val poor = testPerson("poor", 1_000, en = "Poor")
    private val rich = testPerson("rich", 10_000, en = "Rich")
    private val item = testItem("x", 50_000)

    private fun vm(
        personId: String,
        cart: Cart = Cart(),
        settings: FakeSettingsStore = FakeSettingsStore(Settings(true, personId, "USD", "en")),
        cartStore: FakeCartStore = FakeCartStore(cart),
    ) = SettingsViewModel(
        catalog = FakeCatalog(listOf(item)),
        people = FakePeople(listOf(poor, rich)),
        fx = FakeFx(mapOf("USD" to 1.0, "VND" to 25_000.0)),
        settingsStore = settings,
        cartStore = cartStore,
    ) to (settings to cartStore)

    @Test
    fun switchingToRicherPersonAppliesDirectlyAndKeepsCart() = runTest {
        val (vm, stores) = vm("poor", Cart(mapOf("x" to 2L)))
        vm.requestPersonChange("rich")
        assertEquals("rich", stores.first.state.value.personId)
        assertEquals(mapOf("x" to 2L), stores.second.state.value.lines)
        assertNull(vm.state.value.pendingPersonId)
    }

    @Test
    fun switchingToPoorerPersonWhoseBalanceStillFitsTheCartAppliesDirectly() = runTest {
        val (vm, stores) = vm("rich", Cart(mapOf("x" to 1L))) // 50_000 <= 100_000
        vm.requestPersonChange("poor")
        assertEquals("poor", stores.first.state.value.personId)
        assertEquals(mapOf("x" to 1L), stores.second.state.value.lines)
    }

    @Test
    fun switchingToPoorerPersonWithOversizedCartNeedsConfirmation() = runTest {
        val (vm, stores) = vm("rich", Cart(mapOf("x" to 10L))) // 500_000 > 100_000
        vm.requestPersonChange("poor")
        assertEquals("poor", vm.state.value.pendingPersonId)
        assertEquals("rich", stores.first.state.value.personId) // nothing changed yet
        assertEquals(mapOf("x" to 10L), stores.second.state.value.lines)

        vm.confirmPersonChange()
        assertEquals("poor", stores.first.state.value.personId)
        assertTrue(stores.second.state.value.lines.isEmpty())
        assertNull(vm.state.value.pendingPersonId)
    }

    @Test
    fun cancellingKeepsEverythingAsIs() = runTest {
        val (vm, stores) = vm("rich", Cart(mapOf("x" to 10L)))
        vm.requestPersonChange("poor")
        vm.cancelPersonChange()
        assertNull(vm.state.value.pendingPersonId)
        assertEquals("rich", stores.first.state.value.personId)
        assertEquals(mapOf("x" to 10L), stores.second.state.value.lines)
    }

    @Test
    fun selectingTheSamePersonOrAGhostIsANoOp() = runTest {
        val (vm, stores) = vm("rich", Cart(mapOf("x" to 10L)))
        vm.requestPersonChange("rich")
        vm.requestPersonChange("ghost")
        assertEquals("rich", stores.first.state.value.personId)
        assertNull(vm.state.value.pendingPersonId)
    }

    @Test
    fun currencyChangePersistsOnlyForKnownCurrencies() = runTest {
        val (vm, stores) = vm("rich")
        vm.setCurrency("VND")
        assertEquals("VND", stores.first.state.value.currency)
        vm.setCurrency("XYZ")
        assertEquals("VND", stores.first.state.value.currency)
    }

    @Test
    fun languageChangePersists() = runTest {
        val (vm, stores) = vm("rich")
        vm.setLanguage("vi")
        assertEquals("vi", stores.first.state.value.language)
    }
}
