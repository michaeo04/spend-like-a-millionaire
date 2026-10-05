package com.michaeo04.spendlikeamillionaire.domain

import kotlinx.coroutines.flow.Flow

data class Settings(
    val onboarded: Boolean = false,
    val personId: String? = null,
    val currency: String = "USD",
    val language: String = "en",
)

interface SettingsStore {
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

interface CartStore {
    val cart: Flow<Cart>
    suspend fun save(cart: Cart)
}
