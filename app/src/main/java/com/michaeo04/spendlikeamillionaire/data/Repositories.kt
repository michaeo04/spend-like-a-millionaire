package com.michaeo04.spendlikeamillionaire.data

import android.content.Context
import com.michaeo04.spendlikeamillionaire.domain.Item
import com.michaeo04.spendlikeamillionaire.domain.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CatalogRepository {
    suspend fun items(): List<Item>
}

interface PeopleRepository {
    suspend fun people(): List<Person>
}

interface FxRepository {
    /** Units of currency per 1 USD; always contains "USD" -> 1.0. */
    suspend fun rates(): Map<String, Double>
}

/** Reads a bundled asset once and caches the parsed result. */
private class CachedAsset<T>(private val read: () -> String, private val parse: (String) -> T) {
    @Volatile private var cached: T? = null

    suspend fun get(): T = cached ?: withContext(Dispatchers.IO) {
        parse(read()).also { cached = it }
    }
}

private fun Context.readAsset(name: String): () -> String = {
    assets.open(name).bufferedReader().use { it.readText() }
}

interface CreditsRepository {
    suspend fun credits(): List<ImageCredit>
}

class AssetCreditsRepository(context: Context) : CreditsRepository {
    private val asset = CachedAsset(context.readAsset("image_credits.json"), ::parseCredits)
    override suspend fun credits(): List<ImageCredit> = asset.get()
}

class AssetCatalogRepository(context: Context) : CatalogRepository {
    private val asset = CachedAsset(context.readAsset("catalog.json"), ::parseCatalog)
    override suspend fun items(): List<Item> = asset.get()
}

class AssetPeopleRepository(context: Context) : PeopleRepository {
    private val asset = CachedAsset(context.readAsset("people.json"), ::parsePeople)
    override suspend fun people(): List<Person> = asset.get()
}

class AssetFxRepository(context: Context) : FxRepository {
    private val asset = CachedAsset(context.readAsset("fx.json"), ::parseFx)
    override suspend fun rates(): Map<String, Double> = asset.get()
}
