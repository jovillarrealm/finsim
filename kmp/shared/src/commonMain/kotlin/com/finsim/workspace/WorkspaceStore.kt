package com.finsim.workspace

import com.finsim.core.FinsimLog
import com.finsim.domain.LoanEvent
import com.finsim.domain.LoanResult
import com.finsim.domain.Scenario
import com.finsim.domain.exampleScenario
import com.finsim.engine.simulateLoan
import com.finsim.persistence.InMemoryScenarioStorage
import com.finsim.persistence.MAX_FILE_SIZE_BYTES
import com.finsim.persistence.SCENARIO_STORAGE_KEY
import com.finsim.persistence.ScenarioStorage
import com.finsim.persistence.parseScenario
import com.finsim.persistence.serializeScenario

/**
 * Snapshot of the active workspace state.
 *
 * Invariants:
 * - [original] is always the zero-event baseline simulation of [scenario] (`scenario.copy(events = emptyList())`).
 * - [modified] is always the active simulation of [scenario] with its current events.
 * - [original] and [modified] are computed atomically and are never out of sync.
 * - [hasPendingDraft] indicates whether the caller has unapplied batch edits staged in the editor.
 * - [isDirty] indicates whether the current scenario has unsaved modifications relative to storage.
 */
data class WorkspaceState(
    val scenario: Scenario,
    val original: LoanResult,
    val modified: LoanResult,
    val hasPendingDraft: Boolean = false,
    val isDirty: Boolean = false
)

/**
 * Result outcome of operations performed on [WorkspaceStore].
 */
sealed interface WorkspaceOutcome {
    val isSuccess: Boolean
    val message: String

    data class Success(override val message: String) : WorkspaceOutcome {
        override val isSuccess: Boolean = true
    }

    data class Failure(val error: String, val cause: Throwable? = null) : WorkspaceOutcome {
        override val isSuccess: Boolean = false
        override val message: String get() = error
    }
}

/**
 * Deep module encapsulating scenario workspace management, dual simulation execution,
 * JSON serialization, 2MB payload guard, dirty draft tracking, and platform storage dispatch.
 *
 * Dependency category: local-substitutable (satisfies the [ScenarioStorage] seam via
 * platform-specific adapters: SharedPreferences on Android, localStorage on Wasm/Web,
 * and InMemoryScenarioStorage for testing).
 */
class WorkspaceStore(
    private val storage: ScenarioStorage = InMemoryScenarioStorage(),
    private val fallbackScenario: Scenario = exampleScenario
) {
    var state: WorkspaceState
        private set

    private val listeners = mutableListOf<(WorkspaceState) -> Unit>()

    init {
        state = initializeFromStorage()
    }

    /**
     * Subscribes a listener to receive state updates.
     * Returns an unsubscription lambda.
     */
    fun addListener(listener: (WorkspaceState) -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    private fun updateState(newState: WorkspaceState) {
        state = newState
        listeners.forEach { it(newState) }
    }

    private fun initializeFromStorage(): WorkspaceState {
        return try {
            val raw = storage.getItem(SCENARIO_STORAGE_KEY)
            if (raw != null) {
                val parsed = parseScenario(raw)
                val (orig, mod) = executeDualSimulation(parsed)
                FinsimLog.storage.i { "WorkspaceStore: escenario '${parsed.name}' cargado desde almacenamiento local." }
                WorkspaceState(
                    scenario = parsed,
                    original = orig,
                    modified = mod,
                    hasPendingDraft = false,
                    isDirty = false
                )
            } else {
                FinsimLog.storage.i { "WorkspaceStore: sin datos guardados, iniciando con escenario de ejemplo." }
                createInitialState(fallbackScenario)
            }
        } catch (e: Exception) {
            FinsimLog.storage.w { "WorkspaceStore: error al leer almacenamiento local (${e.message}), recuperando con escenario de ejemplo." }
            createInitialState(fallbackScenario)
        }
    }

    private fun createInitialState(scen: Scenario): WorkspaceState {
        val (orig, mod) = executeDualSimulation(scen)
        return WorkspaceState(
            scenario = scen,
            original = orig,
            modified = mod,
            hasPendingDraft = false,
            isDirty = false
        )
    }

    private fun executeDualSimulation(scenario: Scenario): Pair<LoanResult, LoanResult> {
        val origScenario = scenario.copy(events = emptyList())
        val originalResult = simulateLoan(origScenario)
        val modifiedResult = simulateLoan(scenario)
        return Pair(originalResult, modifiedResult)
    }

    /**
     * Persists the current committed scenario into the [ScenarioStorage] adapter.
     * Enforces the invariant that pending drafts must be applied or discarded before saving.
     */
    fun save(): WorkspaceOutcome {
        if (state.hasPendingDraft) {
            FinsimLog.storage.w { "WorkspaceStore.save() rechazado: hay borrador de eventos pendiente." }
            return WorkspaceOutcome.Failure(
                "Hay cambios de eventos pendientes. Aplica o descarta el lote antes de guardar."
            )
        }

        return try {
            val text = serializeScenario(state.scenario)
            if (text.length > MAX_FILE_SIZE_BYTES) {
                return WorkspaceOutcome.Failure("El archivo supera el máximo de 2 MB.")
            }
            storage.setItem(SCENARIO_STORAGE_KEY, text)
            updateState(state.copy(isDirty = false))
            FinsimLog.storage.i { "WorkspaceStore: escenario '${state.scenario.name}' guardado exitosamente." }
            WorkspaceOutcome.Success("Escenario guardado en este dispositivo.")
        } catch (e: Exception) {
            FinsimLog.storage.e(e) { "WorkspaceStore: error al guardar escenario '${state.scenario.name}'" }
            WorkspaceOutcome.Failure(
                "No se pudo guardar el escenario en este dispositivo. Exporta una copia JSON.",
                e
            )
        }
    }

    /**
     * Serializes the current scenario to JSON for file export.
     * Fails if there is an uncommitted pending draft.
     */
    fun exportJson(): Result<String> {
        if (state.hasPendingDraft) {
            FinsimLog.storage.w { "WorkspaceStore.exportJson() rechazado: borrador pendiente." }
            return Result.failure(
                IllegalStateException("Hay cambios de eventos pendientes. Aplica o descarta el lote antes de guardar.")
            )
        }

        return try {
            val json = serializeScenario(state.scenario)
            if (json.length > MAX_FILE_SIZE_BYTES) {
                Result.failure(IllegalArgumentException("El archivo supera el máximo de 2 MB."))
            } else {
                FinsimLog.storage.i { "WorkspaceStore: JSON exportado (${json.length} caracteres)" }
                Result.success(json)
            }
        } catch (e: Exception) {
            FinsimLog.storage.e(e) { "WorkspaceStore: error al exportar JSON" }
            Result.failure(e)
        }
    }

    /**
     * Imports and validates a JSON scenario payload, checking the 2MB size guard,
     * schema version 1, and domain constraints before atomically updating the state.
     */
    fun importJson(text: String): WorkspaceOutcome {
        if (text.length > MAX_FILE_SIZE_BYTES) {
            return WorkspaceOutcome.Failure("El archivo supera el máximo de 2 MB.")
        }

        return try {
            val imported = parseScenario(text)
            val (orig, mod) = executeDualSimulation(imported)
            updateState(
                WorkspaceState(
                    scenario = imported,
                    original = orig,
                    modified = mod,
                    hasPendingDraft = false,
                    isDirty = true
                )
            )
            FinsimLog.storage.i { "WorkspaceStore: escenario '${imported.name}' importado correctamente." }
            WorkspaceOutcome.Success("Escenario importado correctamente.")
        } catch (e: Exception) {
            FinsimLog.storage.e(e) { "WorkspaceStore: fallo al importar JSON" }
            WorkspaceOutcome.Failure(e.message ?: "Error al importar el archivo JSON.", e)
        }
    }

    /**
     * Updates loan conditions (principal, term, rates, insurances) and atomically
     * recalculates both baseline and modified amortization schedules.
     */
    fun updateConditions(newScenario: Scenario): WorkspaceOutcome {
        return try {
            newScenario.validate()
            val (orig, mod) = executeDualSimulation(newScenario)
            updateState(
                state.copy(
                    scenario = newScenario,
                    original = orig,
                    modified = mod,
                    isDirty = true
                )
            )
            FinsimLog.storage.i { "WorkspaceStore: condiciones actualizadas para '${newScenario.name}'." }
            WorkspaceOutcome.Success("Condiciones actualizadas.")
        } catch (e: Exception) {
            FinsimLog.storage.e(e) { "WorkspaceStore: validación falló al actualizar condiciones." }
            WorkspaceOutcome.Failure(e.message ?: "No se pudo calcular.", e)
        }
    }

    /**
     * Applies a validated batch of loan events (prepayments, missed payments, catch-ups)
     * and recalculates the modified schedule, preserving the zero-event baseline.
     */
    fun applyEvents(newEvents: List<LoanEvent>): WorkspaceOutcome {
        return try {
            val updatedScenario = state.scenario.copy(events = newEvents)
            updatedScenario.validate()
            val (orig, mod) = executeDualSimulation(updatedScenario)
            updateState(
                state.copy(
                    scenario = updatedScenario,
                    original = orig,
                    modified = mod,
                    hasPendingDraft = false,
                    isDirty = true
                )
            )
            FinsimLog.storage.i { "WorkspaceStore: ${newEvents.size} eventos aplicados a '${updatedScenario.name}'." }
            WorkspaceOutcome.Success("Eventos aplicados.")
        } catch (e: Exception) {
            FinsimLog.storage.e(e) { "WorkspaceStore: error al aplicar eventos." }
            WorkspaceOutcome.Failure(e.message ?: "Error al aplicar eventos.", e)
        }
    }

    /**
     * Sets whether unapplied draft event edits are pending in the editor.
     */
    fun setDraftPending(pending: Boolean) {
        if (state.hasPendingDraft != pending) {
            updateState(state.copy(hasPendingDraft = pending))
        }
    }

    /**
     * Discards any pending draft flag without changing the committed scenario.
     */
    fun discardDraft() {
        setDraftPending(false)
    }

    /**
     * Resets the active workspace to the default example scenario.
     */
    fun resetToExample(): WorkspaceOutcome {
        val (orig, mod) = executeDualSimulation(fallbackScenario)
        updateState(
            WorkspaceState(
                scenario = fallbackScenario,
                original = orig,
                modified = mod,
                hasPendingDraft = false,
                isDirty = true
            )
        )
        FinsimLog.storage.i { "WorkspaceStore: restablecido a escenario de ejemplo." }
        return WorkspaceOutcome.Success("Ejemplo cargado.")
    }

    /**
     * Reloads scenario from the storage adapter, safely falling back to the example scenario on failure.
     */
    fun reloadFromStorage(): WorkspaceOutcome {
        val newState = initializeFromStorage()
        updateState(newState)
        return WorkspaceOutcome.Success("Escenario recargado desde almacenamiento local.")
    }
}
