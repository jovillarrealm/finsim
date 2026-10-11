package com.finsim.persistence

import com.finsim.core.FinsimLog
import com.finsim.domain.Currency
import com.finsim.domain.Scenario
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

const val SCENARIO_STORAGE_KEY = "finsim.scenario"
const val MAX_FILE_SIZE_BYTES = 2_000_000 // 2 MB

val finsimJson = Json {
    classDiscriminator = "kind"
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

fun serializeScenario(scenario: Scenario): String {
    scenario.validate()
    val serialized = finsimJson.encodeToString(Scenario.serializer(), scenario)
    FinsimLog.storage.d { "Escenario serializado a JSON (${serialized.length} bytes)" }
    return serialized
}

fun parseScenario(text: String): Scenario {
    FinsimLog.storage.i { "Intentando parsear escenario JSON (${text.length} bytes)" }
    if (text.length > MAX_FILE_SIZE_BYTES) {
        FinsimLog.storage.w { "El payload JSON excede el límite de 2 MB (${text.length} bytes)" }
        throw IllegalArgumentException("El archivo supera el máximo de 2 MB.")
    }
    val rawJson = try {
        finsimJson.parseToJsonElement(text)
    } catch (e: Exception) {
        FinsimLog.storage.e(e) { "Error al analizar la estructura sintáctica de JSON" }
        throw IllegalArgumentException("El archivo no contiene JSON válido.", e)
    }

    if (rawJson is JsonObject) {
        val versionElem = rawJson["version"]?.jsonPrimitive?.intOrNull
        if (versionElem != null && versionElem != 1) {
            FinsimLog.storage.w { "Versión no soportada de escenario: $versionElem (esperada: 1)" }
            throw IllegalArgumentException("La versión del escenario no es compatible. Se admite la versión 1.")
        }
    }

    val scenario = try {
        finsimJson.decodeFromJsonElement(Scenario.serializer(), rawJson)
    } catch (e: Exception) {
        FinsimLog.storage.e(e) { "Error al deserializar campos del escenario" }
        throw IllegalArgumentException("El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses.", e)
    }

    try {
        scenario.validate()
    } catch (e: Exception) {
        FinsimLog.storage.e(e) { "Validación de negocio del escenario falló" }
        throw IllegalArgumentException("El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses.", e)
    }
    FinsimLog.storage.i { "Escenario cargado y validado exitosamente: moneda=${scenario.currency}, capital=${scenario.principal}, plazo=${scenario.termMonths}m" }
    return scenario
}

interface ScenarioStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
}

class InMemoryScenarioStorage : ScenarioStorage {
    private val map = mutableMapOf<String, String>()
    override fun getItem(key: String): String? = map[key]
    override fun setItem(key: String, value: String) {
        map[key] = value
    }
}

fun saveScenario(scenario: Scenario, storage: ScenarioStorage) {
    val text = serializeScenario(scenario)
    try {
        storage.setItem(SCENARIO_STORAGE_KEY, text)
    } catch (e: Exception) {
        throw IllegalStateException("No se pudo guardar el escenario en este dispositivo. Exporta una copia JSON.", e)
    }
}

fun loadScenario(storage: ScenarioStorage): Scenario? {
    val text = try {
        storage.getItem(SCENARIO_STORAGE_KEY)
    } catch (e: Exception) {
        throw IllegalStateException("No se pudo acceder al escenario guardado en este dispositivo.", e)
    } ?: return null
    return parseScenario(text)
}
