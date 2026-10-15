package com.finsim.workspace

import com.finsim.domain.*
import com.finsim.persistence.InMemoryScenarioStorage
import com.finsim.persistence.MAX_FILE_SIZE_BYTES
import com.finsim.persistence.SCENARIO_STORAGE_KEY
import com.finsim.persistence.serializeScenario
import kotlin.test.*

class WorkspaceStoreTest {

    @Test
    fun testInitialStateEmptyStorageLoadsExample() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        assertEquals(exampleScenario.name, store.state.scenario.name)
        assertFalse(store.state.hasPendingDraft)
        assertFalse(store.state.isDirty)
        assertNotNull(store.state.original)
        assertNotNull(store.state.modified)
        assertEquals(exampleScenario.termMonths, store.state.original.rows.size)
    }

    @Test
    fun testInitialStateExistingStorageLoadsSavedScenario() {
        val storage = InMemoryScenarioStorage()
        val customScenario = exampleScenario.copy(name = "Custom Loan", principal = "75000000.00")
        storage.setItem(SCENARIO_STORAGE_KEY, serializeScenario(customScenario))

        val store = WorkspaceStore(storage)

        assertEquals("Custom Loan", store.state.scenario.name)
        assertEquals("75000000.00", store.state.scenario.principal)
        assertFalse(store.state.isDirty)
    }

    @Test
    fun testCorruptStorageFallsBackToExampleSafely() {
        val storage = InMemoryScenarioStorage()
        storage.setItem(SCENARIO_STORAGE_KEY, "{ broken json content")

        val store = WorkspaceStore(storage)

        assertEquals(exampleScenario.name, store.state.scenario.name)
        assertNotNull(store.state.original)
        assertNotNull(store.state.modified)
    }

    @Test
    fun testUnsupportedVersionFallsBackToExampleSafely() {
        val storage = InMemoryScenarioStorage()
        val unsupportedJson = """
            {
              "version": 99,
              "name": "Future Schema",
              "source": "original",
              "principal": "5000.00",
              "startDate": "2026-05",
              "termMonths": 12,
              "rate": { "kind": "monthly", "value": "0.01" },
              "insurance": [],
              "events": []
            }
        """.trimIndent()
        storage.setItem(SCENARIO_STORAGE_KEY, unsupportedJson)

        val store = WorkspaceStore(storage)

        assertEquals(exampleScenario.name, store.state.scenario.name)
    }

    @Test
    fun testPendingDraftBlocksSaveAndExport() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        store.setDraftPending(true)
        assertTrue(store.state.hasPendingDraft)

        // Save must be rejected while draft is pending
        val saveOutcome = store.save()
        assertFalse(saveOutcome.isSuccess)
        assertTrue(saveOutcome.message.contains("Hay cambios de eventos pendientes"))

        // Export must also be rejected
        val exportResult = store.exportJson()
        assertTrue(exportResult.isFailure)

        // Discarding draft unblocks saving
        store.discardDraft()
        assertFalse(store.state.hasPendingDraft)
        val secondSave = store.save()
        assertTrue(secondSave.isSuccess)
    }

    @Test
    fun testSavePersistsToStorageAdapter() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        val updatedScenario = store.state.scenario.copy(name = "Saved Mortgage")
        store.updateConditions(updatedScenario)
        assertTrue(store.state.isDirty)

        val saveOutcome = store.save()
        assertTrue(saveOutcome.isSuccess)
        assertFalse(store.state.isDirty)

        // Verify storage adapter received the serialized data
        val persisted = storage.getItem(SCENARIO_STORAGE_KEY)
        assertNotNull(persisted)
        assertTrue(persisted.contains("Saved Mortgage"))

        // Create a new store using the same adapter to test recovery
        val secondStore = WorkspaceStore(storage)
        assertEquals("Saved Mortgage", secondStore.state.scenario.name)
    }

    @Test
    fun testUpdateConditionsRecalculatesDualSimulation() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        val newConditions = store.state.scenario.copy(
            termMonths = 24,
            principal = "10000000.00"
        )
        val outcome = store.updateConditions(newConditions)
        assertTrue(outcome.isSuccess)
        assertEquals(24, store.state.scenario.termMonths)
        assertEquals(24, store.state.original.rows.size)
        assertEquals(24, store.state.modified.rows.size)
        assertTrue(store.state.isDirty)
    }

    @Test
    fun testApplyEventsRecalculatesModifiedScheduleKeepingOriginalBaselineIntact() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        val extraPayment = LoanEvent.Extra(
            id = "extra-1",
            month = 2,
            amount = "50000000.00"
        )
        val outcome = store.applyEvents(listOf(extraPayment))
        assertTrue(outcome.isSuccess)

        // Baseline (original) must have no events and full payoff schedule
        assertTrue(store.state.original.rows.size >= 36)
        // Modified schedule with prepayment must accelerate payoff
        assertTrue(store.state.modified.rows.size < store.state.original.rows.size)
        assertFalse(store.state.hasPendingDraft)
    }

    @Test
    fun testImportJsonSuccessAndAtomicity() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        val importedJson = """
            {
              "version": 1,
              "name": "Prestamo Importado",
              "source": "original",
              "principal": "20000000.00",
              "startDate": "2026-06",
              "termMonths": 24,
              "rate": { "kind": "monthly", "value": "0.015" },
              "insurance": [],
              "events": []
            }
        """.trimIndent()

        val outcome = store.importJson(importedJson)
        assertTrue(outcome.isSuccess)
        assertEquals("Prestamo Importado", store.state.scenario.name)
        assertEquals(24, store.state.scenario.termMonths)

        // Invalid JSON import should fail and preserve active state intact
        val invalidOutcome = store.importJson("{ broken payload")
        assertFalse(invalidOutcome.isSuccess)
        assertEquals("Prestamo Importado", store.state.scenario.name)
    }

    @Test
    fun testImportJsonRejectsSizeExceeding2MB() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        val oversizedJson = "{\"dummy\": \"" + "a".repeat(MAX_FILE_SIZE_BYTES + 10) + "\"}"
        val outcome = store.importJson(oversizedJson)
        assertFalse(outcome.isSuccess)
        assertTrue(outcome.message.contains("2 MB"))
    }

    @Test
    fun testResetToExample() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        store.updateConditions(store.state.scenario.copy(name = "Changed Name"))
        assertEquals("Changed Name", store.state.scenario.name)

        val outcome = store.resetToExample()
        assertTrue(outcome.isSuccess)
        assertEquals(exampleScenario.name, store.state.scenario.name)
    }

    @Test
    fun testListenerReceivesStateUpdates() {
        val storage = InMemoryScenarioStorage()
        val store = WorkspaceStore(storage)

        var listenerCallCount = 0
        val unsubscribe = store.addListener {
            listenerCallCount++
        }

        store.updateConditions(store.state.scenario.copy(termMonths = 48))
        store.setDraftPending(true)
        store.save() // Rejected, no state change
        unsubscribe()
        store.updateConditions(store.state.scenario.copy(termMonths = 60))

        assertEquals(2, listenerCallCount)
    }
}
