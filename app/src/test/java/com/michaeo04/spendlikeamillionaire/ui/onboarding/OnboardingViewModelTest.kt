package com.michaeo04.spendlikeamillionaire.ui.onboarding

import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.testing.FakeFx
import com.michaeo04.spendlikeamillionaire.testing.FakePeople
import com.michaeo04.spendlikeamillionaire.testing.FakeSettingsStore
import com.michaeo04.spendlikeamillionaire.testing.testPerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class OnboardingViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val store = FakeSettingsStore(Settings())

    private fun vm(deviceLanguage: String = "en", settings: FakeSettingsStore = store) = OnboardingViewModel(
        people = FakePeople(listOf(testPerson("p1"), testPerson("p2"))),
        fx = FakeFx(mapOf("USD" to 1.0, "VND" to 25_000.0, "EUR" to 0.9, "NOT-A-CODE" to 3.0)),
        settingsStore = settings,
        deviceLanguage = deviceLanguage,
    )

    @Test
    fun defaultsFollowDeviceLanguage() = runTest {
        assertEquals("en", vm("en").state.value.language)
        assertEquals("USD", vm("en").state.value.currency)
        assertEquals("vi", vm("vi").state.value.language)
        assertEquals("VND", vm("vi").state.value.currency)
        assertEquals("en", vm("fr").state.value.language) // unsupported -> English
    }

    @Test
    fun currencyListContainsOnlyValidCodesUsdFirst() = runTest {
        val currencies = vm().state.value.currencies
        assertEquals("USD", currencies.first())
        assertTrue("VND" in currencies && "EUR" in currencies)
        assertFalse("NOT-A-CODE" in currencies)
    }

    @Test
    fun choosingVietnamesePreselectsVndUntilCurrencyIsChosen() = runTest {
        val vm = vm("en")
        vm.setLanguage("vi")
        assertEquals("VND", vm.state.value.currency)
        vm.setCurrency("EUR")
        vm.setLanguage("en")
        assertEquals("EUR", vm.state.value.currency)
    }

    @Test
    fun stepsAreClampedToTheValidRange() = runTest {
        val vm = vm()
        vm.back()
        assertEquals(0, vm.state.value.step)
        repeat(10) { vm.next() }
        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun cannotFinishWithoutAPerson() = runTest {
        val vm = vm()
        assertFalse(vm.state.value.canFinish)
        var done = false
        vm.finish { done = true }
        assertFalse(done)
        assertFalse(store.state.value.onboarded)
    }

    @Test
    fun finishPersistsTheChoices() = runTest {
        val vm = vm()
        vm.setLanguage("vi")
        vm.setCurrency("EUR")
        vm.setPerson("p2")
        var done = false
        vm.finish { done = true }
        assertTrue(done)
        assertEquals(Settings(onboarded = true, personId = "p2", currency = "EUR", language = "vi"), store.settings.first())
    }

    @Test
    fun unknownPersonIdCannotBeSelected() = runTest {
        val vm = vm()
        vm.setPerson("ghost")
        assertEquals(null, vm.state.value.personId)
        assertFalse(vm.state.value.canFinish)
    }

    @Test
    fun unknownCurrencyIsIgnored() = runTest {
        val vm = vm()
        vm.setCurrency("XYZ")
        assertEquals("USD", vm.state.value.currency)
    }
}
