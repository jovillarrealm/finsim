package com.finsim.ingestion

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VlmJsonResponseDiagnosticsTest {
    @Test
    fun inspectionReportsParseAndSchemaShapeWithoutResponseValues() {
        val inspection = VlmJsonGrammar.inspectResponse(
            """{"bankName":"Synthetic Bank","principal":"987654321","ratePercentage":"7.25","privateNote":"synthetic-only"}"""
        )

        assertTrue(inspection.syntaxAccepted)
        assertEquals("object", inspection.rootKind)
        assertEquals(listOf("bankName", "principal", "ratePercentage"), inspection.knownKeys)
        assertEquals(1, inspection.unknownKeyCount)
        assertEquals(
            setOf("bankName", "principal", "ratePercentage"),
            inspection.nonEmptyKnownKeys.toSet()
        )
        assertFalse(inspection.toString().contains("Synthetic Bank"))
        assertFalse(inspection.toString().contains("987654321"))
        assertFalse(inspection.toString().contains("7.25"))
        assertFalse(inspection.toString().contains("synthetic-only"))
    }

    @Test
    fun inspectionDistinguishesInvalidJsonFromAnEmptyObject() {
        val invalid = VlmJsonGrammar.inspectResponse("{")
        val empty = VlmJsonGrammar.inspectResponse("{}")
        val lenientPrimitive = VlmJsonGrammar.inspectResponse("not-json")

        assertFalse(invalid.syntaxAccepted)
        assertEquals("invalid", invalid.rootKind)
        assertTrue(empty.syntaxAccepted)
        assertEquals("object", empty.rootKind)
        assertTrue(empty.knownKeys.isEmpty())
        assertTrue(empty.nonEmptyKnownKeys.isEmpty())
        assertTrue(lenientPrimitive.syntaxAccepted)
        assertEquals("primitive", lenientPrimitive.rootKind)
        assertTrue(lenientPrimitive.knownKeys.isEmpty())
        assertTrue(lenientPrimitive.nonEmptyKnownKeys.isEmpty())
    }
}
