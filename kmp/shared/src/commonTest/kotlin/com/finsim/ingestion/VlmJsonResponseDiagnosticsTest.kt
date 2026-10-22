package com.finsim.ingestion

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.ExperimentalSerializationApi
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

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun responseSchemaDefinesItsClosedPayloadAndInsuranceObjects() {
        val schema = Json.parseToJsonElement(VlmJsonGrammar.JSON_SCHEMA_PROMPT).jsonObject
        val payloadDescriptor = VlmExtractionPayload.serializer().descriptor
        val payloadProperties = schema.getValue("properties").jsonObject
        val payloadFieldNames = (0 until payloadDescriptor.elementsCount)
            .map(payloadDescriptor::getElementName)
            .toSet()
        val requiredPayloadFields = schema.getValue("required").jsonArray
            .map { it.jsonPrimitive.content }

        assertEquals(payloadFieldNames, payloadProperties.keys)
        assertEquals(setOf("principal", "ratePercentage"), requiredPayloadFields.toSet())
        assertTrue(requiredPayloadFields.all(payloadProperties::containsKey))
        assertEquals(JsonPrimitive(false), schema["additionalProperties"])

        val insuranceItemSchema = schema["properties"]!!.jsonObject
            .getValue("insurances").jsonObject
            .getValue("items").jsonObject
        val insuranceDescriptor = VlmInsuranceJson.serializer().descriptor
        val insuranceProperties = insuranceItemSchema.getValue("properties").jsonObject
        val insuranceFieldNames = (0 until insuranceDescriptor.elementsCount)
            .map(insuranceDescriptor::getElementName)
            .toSet()
        val requiredInsuranceFields = insuranceItemSchema.getValue("required").jsonArray
            .map { it.jsonPrimitive.content }

        assertEquals(insuranceFieldNames, insuranceProperties.keys)
        assertEquals(setOf("name"), requiredInsuranceFields.toSet())
        assertTrue(requiredInsuranceFields.all(insuranceProperties::containsKey))
        assertEquals(JsonPrimitive(false), insuranceItemSchema["additionalProperties"])
    }
}
