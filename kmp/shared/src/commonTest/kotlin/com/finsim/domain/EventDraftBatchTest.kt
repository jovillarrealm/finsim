package com.finsim.domain

import kotlin.test.*

class EventDraftBatchTest {

    @Test
    fun `baseline batch initializes with isDirty false and exact items`() {
        val initialEvents = listOf(
            LoanEvent.Extra(id = "ev1", month = 3, amount = "500.00"),
            LoanEvent.Missed(id = "ev2", month = 6)
        )
        val batch = EventDraftBatch.from(initialEvents)

        assertFalse(batch.isDirty)
        assertEquals(0, batch.pendingChangeCount)
        assertEquals(2, batch.size)
        assertEquals("ev1", batch.items[0].id)
        assertEquals(EventDraftKind.Extra, batch.items[0].kind)
        assertEquals("500.00", batch.items[0].amount)
        assertEquals("ev2", batch.items[1].id)
        assertEquals(EventDraftKind.Missed, batch.items[1].kind)
    }

    @Test
    fun `adding events allocates deterministic unique IDs and marks dirty in O(1)`() {
        val batch = EventDraftBatch.empty()
        val added1 = batch.add(month = 5, kind = EventDraftKind.Extra, amount = "200.00")

        assertTrue(added1.isDirty)
        assertEquals(1, added1.pendingChangeCount)
        assertEquals(1, added1.size)
        assertTrue(added1.items[0].id.startsWith("ev_draft_"))
        assertTrue(added1.items[0].id.endsWith("_m5"))

        val added2 = added1.add(month = 10, kind = EventDraftKind.Catchup, amount = "300.00")
        assertEquals(2, added2.size)
        assertNotEquals(added2.items[0].id, added2.items[1].id)
        assertEquals(2, added2.pendingChangeCount)
    }

    @Test
    fun `modifying an existing item marks batch dirty and reverting it restores clean state in O(1)`() {
        val initialEvents = listOf(
            LoanEvent.Extra(id = "ev1", month = 4, amount = "150.00")
        )
        val batch = EventDraftBatch.from(initialEvents)
        assertFalse(batch.isDirty)

        // Modify amount
        val modified = batch.updateAmount("ev1", "250.00")
        assertTrue(modified.isDirty)
        assertEquals(1, modified.pendingChangeCount)
        assertEquals("250.00", modified.items[0].amount)

        // Revert back to original amount
        val reverted = modified.updateAmount("ev1", "150.00")
        assertFalse(reverted.isDirty)
        assertEquals(0, reverted.pendingChangeCount)
    }

    @Test
    fun `adding then removing the same item clears dirty tracking in O(1)`() {
        val initialEvents = listOf(
            LoanEvent.Extra(id = "ev1", month = 2, amount = "100.00")
        )
        val batch = EventDraftBatch.from(initialEvents)

        val added = batch.add(month = 8, kind = EventDraftKind.Missed)
        assertTrue(added.isDirty)
        assertEquals(2, added.size)

        val addedId = added.items.last().id
        val removed = added.remove(addedId)

        assertFalse(removed.isDirty)
        assertEquals(0, removed.pendingChangeCount)
        assertEquals(1, removed.size)
        assertEquals("ev1", removed.items[0].id)
    }

    @Test
    fun `removing baseline item marks dirty and discard restores baseline in O(1)`() {
        val initialEvents = listOf(
            LoanEvent.Extra(id = "ev1", month = 2, amount = "100.00"),
            LoanEvent.Catchup(id = "ev2", month = 5, amount = "200.00")
        )
        val batch = EventDraftBatch.from(initialEvents)

        val removed = batch.remove("ev1")
        assertTrue(removed.isDirty)
        assertEquals(1, removed.size)
        assertEquals("ev2", removed.items[0].id)

        // Discard restores original baseline
        val discarded = removed.discard()
        assertFalse(discarded.isDirty)
        assertEquals(2, discarded.size)
        assertEquals("ev1", discarded.items[0].id)
        assertEquals("ev2", discarded.items[1].id)
    }

    @Test
    fun `commit successfully validates and maps events in chronological order`() {
        val batch = EventDraftBatch.empty()
            .add(month = 12, kind = EventDraftKind.Catchup, amount = "500.00")
            .add(month = 3, kind = EventDraftKind.Extra, amount = "100.50")
            .add(month = 6, kind = EventDraftKind.Missed)
            .add(month = 8, kind = EventDraftKind.Recurring, amount = "50.00", endMonth = 10, every = 1)

        val result = batch.commit()
        assertTrue(result is BatchCommitResult.Success)

        val events = result.events
        assertEquals(4, events.size)
        // Ordered chronologically by month
        assertEquals(3, events[0].month)
        assertTrue(events[0] is LoanEvent.Extra)
        assertEquals("100.50", (events[0] as LoanEvent.Extra).amount)

        assertEquals(6, events[1].month)
        assertTrue(events[1] is LoanEvent.Missed)

        assertEquals(8, events[2].month)
        assertTrue(events[2] is LoanEvent.Recurring)
        assertEquals(10, (events[2] as LoanEvent.Recurring).endMonth)

        assertEquals(12, events[3].month)
        assertTrue(events[3] is LoanEvent.Catchup)
    }

    @Test
    fun `commit rejects invalid months and reports structured errors`() {
        val batch = EventDraftBatch.empty()
            .add(month = 0, kind = EventDraftKind.Extra, amount = "100.00")
            .add(month = 1201, kind = EventDraftKind.Missed)

        val result = batch.commit()
        assertTrue(result is BatchCommitResult.Failure)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.all { it.field == DraftField.Month })
    }

    @Test
    fun `commit rejects monetary amounts with more than two decimal places`() {
        val batch = EventDraftBatch.empty()
            .add(month = 5, kind = EventDraftKind.Extra, amount = "100.555")

        val result = batch.commit()
        assertTrue(result is BatchCommitResult.Failure)
        val error = result.errors.first()
        assertEquals(DraftField.Amount, error.field)
        assertTrue(error.message.contains("dos decimales"))
    }

    @Test
    fun `commit rejects non-positive or non-numeric monetary amounts`() {
        val batchZero = EventDraftBatch.empty()
            .add(month = 5, kind = EventDraftKind.Extra, amount = "0.00")
        val resultZero = batchZero.commit()
        assertTrue(resultZero is BatchCommitResult.Failure)
        assertTrue(resultZero.errors.first().message.contains("mayor que cero"))

        val batchInvalid = EventDraftBatch.empty()
            .add(month = 5, kind = EventDraftKind.Extra, amount = "abc")
        val resultInvalid = batchInvalid.commit()
        assertTrue(resultInvalid is BatchCommitResult.Failure)
        assertTrue(resultInvalid.errors.first().message.contains("decimal"))
    }

    @Test
    fun `commit rejects recurring event when endMonth is before initial month`() {
        val batch = EventDraftBatch.empty()
            .add(month = 10, kind = EventDraftKind.Recurring, amount = "50.00", endMonth = 5, every = 1)

        val result = batch.commit()
        assertTrue(result is BatchCommitResult.Failure)
        val error = result.errors.first()
        assertEquals(DraftField.EndMonth, error.field)
        assertTrue(error.message.contains("igual o posterior"))
    }
}
