package com.michaeo04.spendlikeamillionaire.data

import com.michaeo04.spendlikeamillionaire.testing.FakePeople
import com.michaeo04.spendlikeamillionaire.testing.testPerson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPeopleRepositoryTest {
    private val base = FakePeople(listOf(testPerson("a", 100, "A"), testPerson("b", 200, "B")))

    private fun overrides(json: String) = object : NetWorthOverrides {
        override suspend fun current() = parseOverrides(json)
    }

    private fun repo(json: String) = OverlayPeopleRepository(base, overrides(json))

    private fun people(json: String) = runBlocking { repo(json).people() }

    @Test
    fun validOverrideReplacesNetWorthSourceAndDate() {
        val result = people("""{"a":{"usd":555,"source":"Bloomberg","asOf":"2026-11"}}""")
        val a = result.first { it.id == "a" }
        assertEquals(555L, a.netWorthUsd)
        assertEquals("Bloomberg", a.source)
        assertEquals("2026-11", a.asOf)
        assertEquals(200L, result.first { it.id == "b" }.netWorthUsd) // untouched
    }

    @Test
    fun malformedJsonFallsBackToBundledValues() {
        listOf("not json", "[]", "", "{\"a\":5}", "{\"a\":{\"usd\":\"x\"}}").forEach { bad ->
            assertEquals(listOf(100L, 200L), people(bad).map { it.netWorthUsd })
        }
    }

    @Test
    fun zeroNegativeAndAbsurdNetWorthsAreIgnored() {
        val zero = people("""{"a":{"usd":0,"source":"X","asOf":"2026-11"}}""")
        val negative = people("""{"a":{"usd":-5,"source":"X","asOf":"2026-11"}}""")
        val absurd = people("""{"a":{"usd":9000000000000000000,"source":"X","asOf":"2026-11"}}""")
        listOf(zero, negative, absurd).forEach { assertEquals(100L, it.first { p -> p.id == "a" }.netWorthUsd) }
    }

    @Test
    fun unknownPersonIdIsIgnoredAndNotAdded() {
        val result = people("""{"ghost":{"usd":5,"source":"X","asOf":"2026-11"}}""")
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun badSourceOrDateInvalidatesThatOverride() {
        assertEquals(100L, people("""{"a":{"usd":5,"source":"","asOf":"2026-11"}}""").first().netWorthUsd)
        assertEquals(100L, people("""{"a":{"usd":5,"source":"X","asOf":"Nov 2026"}}""").first().netWorthUsd)
    }

    @Test
    fun overridesThatThrowFallBackToBase() {
        val throwing = object : NetWorthOverrides {
            override suspend fun current(): Map<String, Override> = error("network down")
        }
        val result = runBlocking { OverlayPeopleRepository(base, throwing).people() }
        assertEquals(listOf(100L, 200L), result.map { it.netWorthUsd })
    }

    @Test
    fun noOverridesLeavesBaseUntouched() {
        val result = runBlocking { OverlayPeopleRepository(base, NoOverrides).people() }
        assertTrue(result.map { it.netWorthUsd } == listOf(100L, 200L))
    }
}
