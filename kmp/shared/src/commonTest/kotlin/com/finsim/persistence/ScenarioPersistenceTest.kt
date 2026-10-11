package com.finsim.persistence

import com.finsim.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ScenarioPersistenceTest {

    @Test
    fun testSerializationAndParsing() {
        val json = serializeScenario(exampleScenario)
        val parsed = parseScenario(json)
        assertEquals(exampleScenario.name, parsed.name)
        assertEquals(exampleScenario.principal, parsed.principal)
        assertEquals(exampleScenario.startDate, parsed.startDate)
        assertEquals(exampleScenario.termMonths, parsed.termMonths)
        assertEquals(exampleScenario.currency, parsed.currency)
    }

    @Test
    fun testDefaultCurrencyCopWhenOmitted() {
        val legacyJson = """
            {
              "version": 1,
              "name": "Legacy",
              "source": "original",
              "principal": "5000.00",
              "startDate": "2026-05",
              "termMonths": 12,
              "rate": { "kind": "monthly", "value": "0.01" },
              "insurance": [],
              "events": []
            }
        """.trimIndent()
        val parsed = parseScenario(legacyJson)
        assertEquals(Currency.COP, parsed.currency)
    }

    @Test
    fun testRejectUnsupportedVersion() {
        val invalidVersion = """
            {
              "version": 2,
              "name": "V2",
              "source": "original",
              "principal": "5000.00",
              "startDate": "2026-05",
              "termMonths": 12,
              "rate": { "kind": "monthly", "value": "0.01" },
              "insurance": [],
              "events": []
            }
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            parseScenario(invalidVersion)
        }
    }

    @Test
    fun testRejectInvalidJson() {
        assertFailsWith<IllegalArgumentException> {
            parseScenario("{ bad json")
        }
    }

    @Test
    fun testStorageSaveAndLoad() {
        val storage = InMemoryScenarioStorage()
        saveScenario(exampleScenario, storage)
        val loaded = loadScenario(storage)
        assertNotNull(loaded)
        assertEquals(exampleScenario.name, loaded.name)
    }
}
