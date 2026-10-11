package com.finsim.persistence

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
    return finsimJson.encodeToString(Scenario.serializer(), scenario)
}

fun parseScenario(text: String): Scenario {
    require(text.length <= MAX_FILE_SIZE_BYTES) { "El archivo supera el máximo de 2 MB." }
    val rawJson = try {
        finsimJson.parseToJsonElement(text)
    } catch (e: Exception) {
        throw IllegalArgumentException("El archivo no contiene JSON válido.", e)
    }

    if (rawJson is JsonObject) {
        val versionElem = rawJson["version"]?.jsonPrimitive?.intOrNull
        if (versionElem != null && versionElem != 1) {
            throw IllegalArgumentException("La versión del escenario no es compatible. Se admite la versión 1.")
        }
    }

    val scenario = try {
        finsimJson.decodeFromJsonElement(Scenario.serializer(), rawJson)
    } catch (e: Exception) {
        throw IllegalArgumentException("El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses.", e)
    }

    try {
        scenario.validate()
    } catch (e: Exception) {
        throw IllegalArgumentException("El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses.", e)
    }
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
