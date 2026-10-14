package com.finsim.domain

import com.finsim.core.Decimal

/**
 * Discriminante para los tipos de eventos financieros admitidos en el borrador de lote.
 */
enum class EventDraftKind {
    Extra,
    Recurring,
    Missed,
    Catchup;

    val displayName: String
        get() = when (this) {
            Extra -> "Abono a capital"
            Recurring -> "Abono recurrente"
            Missed -> "Omitir cuota"
            Catchup -> "Recuperación"
        }

    val wireKind: String
        get() = when (this) {
            Extra -> "extra"
            Recurring -> "recurring"
            Missed -> "missed"
            Catchup -> "catchup"
        }

    companion object {
        fun fromWire(kind: String): EventDraftKind = when (kind.trim().lowercase()) {
            "extra" -> Extra
            "recurring" -> Recurring
            "missed" -> Missed
            "catchup" -> Catchup
            else -> Extra
        }

        fun fromDomain(event: LoanEvent): EventDraftKind = when (event) {
            is LoanEvent.Extra -> Extra
            is LoanEvent.Recurring -> Recurring
            is LoanEvent.Missed -> Missed
            is LoanEvent.Catchup -> Catchup
        }
    }
}

/**
 * Elemento inmutable de borrador de evento dentro del lote.
 */
data class EventDraftItem(
    val id: String,
    val kind: EventDraftKind,
    val month: Int,
    val amount: String = "100.00",
    val endMonth: Int = month,
    val every: Int = 1
) {
    companion object {
        fun fromDomain(event: LoanEvent): EventDraftItem = when (event) {
            is LoanEvent.Extra -> EventDraftItem(
                id = event.id,
                kind = EventDraftKind.Extra,
                month = event.month,
                amount = event.amount,
                endMonth = event.month,
                every = 1
            )
            is LoanEvent.Recurring -> EventDraftItem(
                id = event.id,
                kind = EventDraftKind.Recurring,
                month = event.month,
                amount = event.amount,
                endMonth = event.endMonth,
                every = event.every
            )
            is LoanEvent.Missed -> EventDraftItem(
                id = event.id,
                kind = EventDraftKind.Missed,
                month = event.month,
                amount = "0.00",
                endMonth = event.month,
                every = 1
            )
            is LoanEvent.Catchup -> EventDraftItem(
                id = event.id,
                kind = EventDraftKind.Catchup,
                month = event.month,
                amount = event.amount,
                endMonth = event.month,
                every = 1
            )
        }
    }
}

/**
 * Campo de borrador asociado a un error de validación.
 */
enum class DraftField {
    Month,
    Amount,
    EndMonth,
    Every,
    General
}

/**
 * Error de validación estructurado en la confirmación del lote de eventos.
 */
data class DraftValidationError(
    val eventId: String,
    val field: DraftField,
    val message: String
)

/**
 * Resultado sellado de la confirmación y validación del lote de eventos.
 */
sealed class BatchCommitResult {
    data class Success(val events: List<LoanEvent>) : BatchCommitResult()
    data class Failure(val errors: List<DraftValidationError>) : BatchCommitResult() {
        val summaryMessage: String
            get() = errors.firstOrNull()?.message ?: "Error al validar lote de eventos."
    }
}

/**
 * Módulo profundo de gestión de borrador por lotes de eventos (EventDraftBatch).
 *
 * Encapsula la preparación de modificaciones, adición, eliminación, seguimiento
 * de suciedad (dirty tracking) en tiempo constante O(1), generación determinista
 * de identificadores y validación según las convenciones financieras de la aplicación.
 */
class EventDraftBatch private constructor(
    private val baseline: List<LoanEvent>,
    private val baselineMap: Map<String, EventDraftItem>,
    val items: List<EventDraftItem>,
    private val addedIds: Set<String>,
    private val removedBaselineIds: Set<String>,
    private val modifiedBaselineIds: Set<String>,
    private val nextIdSequence: Int
) {

    /**
     * Indicador de cambios pendientes respecto a la línea base en tiempo O(1).
     */
    val isDirty: Boolean
        get() = addedIds.isNotEmpty() || removedBaselineIds.isNotEmpty() || modifiedBaselineIds.isNotEmpty()

    /**
     * Número total de operaciones pendientes (adiciones, eliminaciones y modificaciones).
     */
    val pendingChangeCount: Int
        get() = addedIds.size + removedBaselineIds.size + modifiedBaselineIds.size

    val size: Int
        get() = items.size

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun getItem(id: String): EventDraftItem? = items.find { it.id == id }

    /**
     * Añade un nuevo evento en borrador con identificador determinista libre de colisiones.
     */
    fun add(
        month: Int,
        kind: EventDraftKind = EventDraftKind.Extra,
        amount: String = "100.00",
        endMonth: Int = month,
        every: Int = 1
    ): EventDraftBatch {
        val generatedId = "ev_draft_${nextIdSequence}_m$month"
        val newItem = EventDraftItem(
            id = generatedId,
            kind = kind,
            month = month,
            amount = amount,
            endMonth = endMonth,
            every = every
        )
        return EventDraftBatch(
            baseline = baseline,
            baselineMap = baselineMap,
            items = items + newItem,
            addedIds = addedIds + generatedId,
            removedBaselineIds = removedBaselineIds,
            modifiedBaselineIds = modifiedBaselineIds,
            nextIdSequence = nextIdSequence + 1
        )
    }

    /**
     * Actualiza un elemento existente aplicando la función transformadora.
     */
    fun updateItem(id: String, transform: (EventDraftItem) -> EventDraftItem): EventDraftBatch {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return this

        val currentItem = items[index]
        val updatedItem = transform(currentItem)
        if (updatedItem == currentItem) return this

        val newItems = items.toMutableList()
        newItems[index] = updatedItem

        val isAdded = id in addedIds
        val newModified = if (isAdded) {
            modifiedBaselineIds
        } else {
            val original = baselineMap[id]
            if (original != null && original == updatedItem) {
                modifiedBaselineIds - id
            } else {
                modifiedBaselineIds + id
            }
        }

        return EventDraftBatch(
            baseline = baseline,
            baselineMap = baselineMap,
            items = newItems,
            addedIds = addedIds,
            removedBaselineIds = removedBaselineIds,
            modifiedBaselineIds = newModified,
            nextIdSequence = nextIdSequence
        )
    }

    fun updateMonth(id: String, month: Int): EventDraftBatch =
        updateItem(id) { it.copy(month = month) }

    fun updateKind(id: String, kind: EventDraftKind): EventDraftBatch =
        updateItem(id) { it.copy(kind = kind) }

    fun updateAmount(id: String, amount: String): EventDraftBatch =
        updateItem(id) { it.copy(amount = amount) }

    fun updateRecurring(id: String, endMonth: Int, every: Int): EventDraftBatch =
        updateItem(id) { it.copy(endMonth = endMonth, every = every) }

    /**
     * Elimina un evento del borrador.
     */
    fun remove(id: String): EventDraftBatch {
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return this

        val newItems = items.filter { it.id != id }

        val newAdded = addedIds - id
        val newModified = modifiedBaselineIds - id
        val newRemoved = if (id in baselineMap) {
            removedBaselineIds + id
        } else {
            removedBaselineIds
        }

        return EventDraftBatch(
            baseline = baseline,
            baselineMap = baselineMap,
            items = newItems,
            addedIds = newAdded,
            removedBaselineIds = newRemoved,
            modifiedBaselineIds = newModified,
            nextIdSequence = nextIdSequence
        )
    }

    /**
     * Descarta todos los borradores y restaura la línea base en O(1).
     */
    fun discard(): EventDraftBatch = from(baseline)

    /**
     * Valida y confirma el lote convirtiendo los borradores en eventos de dominio LoanEvent.
     */
    fun commit(): BatchCommitResult {
        val errors = mutableListOf<DraftValidationError>()

        items.forEach { item ->
            // Invariante de mes: 1..1200
            if (item.month !in 1..MAX_MONTHS) {
                errors.add(
                    DraftValidationError(
                        eventId = item.id,
                        field = DraftField.Month,
                        message = "El mes del evento (${item.month}) debe estar entre 1 y $MAX_MONTHS."
                    )
                )
            }

            // Invariante de importe para eventos monetarios
            if (item.kind != EventDraftKind.Missed) {
                val amtStr = item.amount.trim()
                if (amtStr.isEmpty()) {
                    errors.add(
                        DraftValidationError(
                            eventId = item.id,
                            field = DraftField.Amount,
                            message = "El importe no puede estar vacío."
                        )
                    )
                } else {
                    // Validar máximo dos decimales conforme a convenciones financieras
                    val dotIndex = amtStr.indexOf('.')
                    val decimalPlaces = if (dotIndex >= 0) amtStr.length - dotIndex - 1 else 0
                    if (decimalPlaces > 2) {
                        errors.add(
                            DraftValidationError(
                                eventId = item.id,
                                field = DraftField.Amount,
                                message = "Los importes deben tener como máximo dos decimales."
                            )
                        )
                    } else {
                        try {
                            val parsed = Decimal.parse(amtStr)
                            if (!parsed.isPositive) {
                                errors.add(
                                    DraftValidationError(
                                        eventId = item.id,
                                        field = DraftField.Amount,
                                        message = "El importe debe ser mayor que cero."
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            errors.add(
                                DraftValidationError(
                                    eventId = item.id,
                                    field = DraftField.Amount,
                                    message = "El importe no tiene un formato decimal numérico válido."
                                )
                            )
                        }
                    }
                }
            }

            // Invariantes específicas de abono recurrente
            if (item.kind == EventDraftKind.Recurring) {
                if (item.endMonth !in 1..MAX_MONTHS) {
                    errors.add(
                        DraftValidationError(
                            eventId = item.id,
                            field = DraftField.EndMonth,
                            message = "El mes final (${item.endMonth}) debe estar entre 1 y $MAX_MONTHS."
                        )
                    )
                }
                if (item.endMonth < item.month) {
                    errors.add(
                        DraftValidationError(
                            eventId = item.id,
                            field = DraftField.EndMonth,
                            message = "El mes final (${item.endMonth}) debe ser igual o posterior al inicial (${item.month})."
                        )
                    )
                }
                if (item.every !in 1..MAX_MONTHS) {
                    errors.add(
                        DraftValidationError(
                            eventId = item.id,
                            field = DraftField.Every,
                            message = "La frecuencia debe ser de al menos 1 mes."
                        )
                    )
                }
            }
        }

        // Invariante de identificadores únicos
        val uniqueIds = items.map { it.id }.toSet()
        if (uniqueIds.size != items.size) {
            errors.add(
                DraftValidationError(
                    eventId = "",
                    field = DraftField.General,
                    message = "Los identificadores de eventos no deben repetirse."
                )
            )
        }

        if (errors.isNotEmpty()) {
            return BatchCommitResult.Failure(errors)
        }

        val domainEvents = items.map { item ->
            when (item.kind) {
                EventDraftKind.Extra -> LoanEvent.Extra(
                    id = item.id,
                    month = item.month,
                    amount = item.amount.trim()
                )
                EventDraftKind.Recurring -> LoanEvent.Recurring(
                    id = item.id,
                    month = item.month,
                    endMonth = item.endMonth,
                    every = item.every,
                    amount = item.amount.trim()
                )
                EventDraftKind.Missed -> LoanEvent.Missed(
                    id = item.id,
                    month = item.month
                )
                EventDraftKind.Catchup -> LoanEvent.Catchup(
                    id = item.id,
                    month = item.month,
                    amount = item.amount.trim()
                )
            }
        }.sortedWith(compareBy({ it.month }, { it.id }))

        return BatchCommitResult.Success(domainEvents)
    }

    companion object {
        /**
         * Inicializa un lote de eventos a partir de la lista de eventos de dominio establecida.
         */
        fun from(events: List<LoanEvent>): EventDraftBatch {
            val mappedItems = events.map { EventDraftItem.fromDomain(it) }
            val baseMap = mappedItems.associateBy { it.id }

            // Calcular secuencia base para evitar colisiones con IDs existentes
            var maxSeq = 0
            val regexSeq = Regex("""\d+""")
            events.forEach { ev ->
                regexSeq.findAll(ev.id).forEach { match ->
                    match.value.toIntOrNull()?.let { num ->
                        if (num > maxSeq) maxSeq = num
                    }
                }
            }
            val initialSeq = maxOf(events.size, maxSeq) + 1

            return EventDraftBatch(
                baseline = events,
                baselineMap = baseMap,
                items = mappedItems,
                addedIds = emptySet(),
                removedBaselineIds = emptySet(),
                modifiedBaselineIds = emptySet(),
                nextIdSequence = initialSeq
            )
        }

        fun empty(): EventDraftBatch = from(emptyList())
    }
}
