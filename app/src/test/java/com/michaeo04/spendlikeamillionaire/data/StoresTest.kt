package com.michaeo04.spendlikeamillionaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory DataStore: the real one replaces files with File.renameTo, which cannot overwrite on
 * Windows JVMs, so file-backed multi-write tests only pass on Linux. We test our store logic here
 * and verify real persistence on a device.
 */
private class FakePrefsDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val next = transform(state.value)
        state.value = next
        return next
    }
}

class StoresTest {
    @Test
    fun cartCodecRoundTrips() {
        val cart = Cart(mapOf("a" to 3L, "b" to 12L))
        assertEquals(cart, CartCodec.decode(CartCodec.encode(cart)))
    }

    @Test
    fun cartCodecGarbageOrNullGivesEmptyCart() {
        assertTrue(CartCodec.decode(null).lines.isEmpty())
        assertTrue(CartCodec.decode("not json").lines.isEmpty())
        assertTrue(CartCodec.decode("[1,2]").lines.isEmpty())
    }

    @Test
    fun cartCodecDropsNonPositiveQuantities() {
        val decoded = CartCodec.decode("""{"a":2,"b":0,"c":-4}""")
        assertEquals(mapOf("a" to 2L), decoded.lines)
    }

    @Test
    fun settingsDefaultsThenPersistsUpdates() = runBlocking {
        val store = DataStoreSettingsStore(FakePrefsDataStore())
        assertEquals(Settings(), store.settings.first())
        store.update { it.copy(onboarded = true, personId = "p_musk", currency = "VND", language = "vi") }
        assertEquals(Settings(true, "p_musk", "VND", "vi"), store.settings.first())
    }

    @Test
    fun settingsPersonCanBeClearedBackToNull() = runBlocking {
        val store = DataStoreSettingsStore(FakePrefsDataStore())
        store.update { it.copy(personId = "p_musk") }
        store.update { it.copy(personId = null) }
        assertEquals(null, store.settings.first().personId)
    }

    @Test
    fun cartStorePersistsAndClears() = runBlocking {
        val store = DataStoreCartStore(FakePrefsDataStore())
        assertTrue(store.cart.first().lines.isEmpty())
        store.save(Cart(mapOf("a" to 2L)))
        assertEquals(mapOf("a" to 2L), store.cart.first().lines)
        store.save(Cart())
        assertTrue(store.cart.first().lines.isEmpty())
    }

    @Test
    fun cartStoreIgnoresCorruptStoredValue() = runBlocking {
        val ds = FakePrefsDataStore()
        val store = DataStoreCartStore(ds)
        ds.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                this[androidx.datastore.preferences.core.stringPreferencesKey("cart")] = "###"
            }
        }
        assertTrue(store.cart.first().lines.isEmpty())
    }
}
