package com.michaeo04.spendlikeamillionaire.data

import com.michaeo04.spendlikeamillionaire.domain.Person
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.coroutines.cancellation.CancellationException

/** A remotely supplied net worth for one person. */
data class Override(val usd: Long, val source: String, val asOf: String)

/** Source of net-worth overrides (Firebase Remote Config in production, nothing in tests/CI). */
interface NetWorthOverrides {
    /** Currently cached overrides; must be cheap and never block on the network. */
    suspend fun current(): Map<String, Override>

    /** Fetches fresh values in the background; they apply from the next launch. */
    suspend fun refresh() = Unit
}

object NoOverrides : NetWorthOverrides {
    override suspend fun current(): Map<String, Override> = emptyMap()
}

/** Anything above this is treated as bad data (10 trillion USD). */
const val MAX_NET_WORTH_USD = 10_000_000_000_000L

private val YEAR_MONTH = Regex("""\d{4}-\d{2}""")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class OverrideDto(val usd: Long, val source: String, val asOf: String)

/**
 * Parses `{"<personId>":{"usd":..,"source":"..","asOf":"YYYY-MM"}}`. Invalid entries (bad types,
 * non-positive or absurd net worth, blank source, malformed date) are skipped; garbage input yields
 * an empty map.
 */
fun parseOverrides(text: String): Map<String, Override> {
    val root = try {
        json.parseToJsonElement(text) as? JsonObject ?: return emptyMap()
    } catch (e: Exception) {
        return emptyMap()
    }
    val out = LinkedHashMap<String, Override>()
    for ((id, element) in root) {
        val dto = try {
            json.decodeFromJsonElement<OverrideDto>(element)
        } catch (e: Exception) {
            continue
        }
        val valid = dto.usd in 1..MAX_NET_WORTH_USD &&
            dto.source.isNotBlank() &&
            YEAR_MONTH.matches(dto.asOf)
        if (valid) out[id] = Override(dto.usd, dto.source, dto.asOf)
    }
    return out
}

/**
 * Bundled people with valid remote overrides applied on top; any failure keeps the bundled data.
 * The result is computed once per process so every screen sees the same net worth even if Remote
 * Config activates new values mid-session (they apply from the next launch).
 */
class OverlayPeopleRepository(
    private val base: PeopleRepository,
    private val overrides: NetWorthOverrides,
) : PeopleRepository {
    private val lock = Mutex()

    @Volatile private var snapshot: List<Person>? = null

    override suspend fun people(): List<Person> =
        snapshot ?: lock.withLock { snapshot ?: compute().also { snapshot = it } }

    private suspend fun compute(): List<Person> {
        val bundled = base.people()
        val remote = try {
            overrides.current()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyMap()
        }
        return bundled.map { person ->
            remote[person.id]?.let { person.copy(netWorthUsd = it.usd, source = it.source, asOf = it.asOf) } ?: person
        }
    }
}
