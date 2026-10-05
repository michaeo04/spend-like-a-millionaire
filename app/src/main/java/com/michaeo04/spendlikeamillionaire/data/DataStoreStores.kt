package com.michaeo04.spendlikeamillionaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.michaeo04.spendlikeamillionaire.domain.Cart
import com.michaeo04.spendlikeamillionaire.domain.CartStore
import com.michaeo04.spendlikeamillionaire.domain.Settings
import com.michaeo04.spendlikeamillionaire.domain.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Cart <-> JSON string. Corrupt data never throws: it yields an empty cart. */
object CartCodec {
    fun encode(cart: Cart): String =
        JsonObject(cart.lines.mapValues { JsonPrimitive(it.value) }).toString()

    fun decode(text: String?): Cart {
        if (text.isNullOrBlank()) return Cart()
        return try {
            val obj = Json.parseToJsonElement(text) as? JsonObject ?: return Cart()
            Cart(
                obj.mapNotNull { (id, value) ->
                    val qty = (value as? JsonPrimitive)?.longOrNull
                    if (qty != null && qty > 0) id to qty else null
                }.toMap(),
            )
        } catch (e: Exception) {
            Cart()
        }
    }
}

class DataStoreSettingsStore(private val dataStore: DataStore<Preferences>) : SettingsStore {
    private val onboarded = booleanPreferencesKey("onboarded")
    private val person = stringPreferencesKey("person_id")
    private val currency = stringPreferencesKey("currency")
    private val language = stringPreferencesKey("language")

    override val settings: Flow<Settings> = dataStore.data.map { read(it) }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val next = transform(read(prefs))
            prefs[onboarded] = next.onboarded
            if (next.personId == null) prefs.remove(person) else prefs[person] = next.personId
            prefs[currency] = next.currency
            prefs[language] = next.language
        }
    }

    private fun read(prefs: Preferences): Settings {
        val defaults = Settings()
        return Settings(
            onboarded = prefs[onboarded] ?: defaults.onboarded,
            personId = prefs[person],
            currency = prefs[currency] ?: defaults.currency,
            language = prefs[language] ?: defaults.language,
        )
    }
}

class DataStoreCartStore(private val dataStore: DataStore<Preferences>) : CartStore {
    private val key = stringPreferencesKey("cart")

    override val cart: Flow<Cart> = dataStore.data.map { CartCodec.decode(it[key]) }

    override suspend fun save(cart: Cart) {
        dataStore.edit { prefs ->
            if (cart.lines.isEmpty()) prefs.remove(key) else prefs[key] = CartCodec.encode(cart)
        }
    }
}
