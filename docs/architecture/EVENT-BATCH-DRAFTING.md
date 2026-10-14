# Arquitectura: Módulo Profundo de Borrador de Eventos por Lotes (EventDraftBatch)

## 1. Contexto y Problema

El brief de producto (`docs/product-brief.md`) y las convenciones financieras (`docs/financial-conventions.md`) exigen que los eventos de simulación (abonos extraordinarios a capital, abonos recurrentes, omisiones de cuota y pagos de recuperación) puedan editarse en múltiples meses antes de recalcular el escenario modificado.

Antes de esta extracción, la vista de edición mantenía estructuras mutables (`EditableEventItem`), ejecutaba comparaciones lineales $O(N)$ en cada recomposición para verificar si existían cambios pendientes (`hasPending`), generaba identificadores no deterministas mediante concatenación ad-hoc de cadenas, y realizaba conversiones manuales a objetos de dominio `LoanEvent` dispersas en la lógica de presentación. Esta dispersión impedía verificar el comportamiento de borrador fuera del entorno visual y acoplaba la coherencia del lote al ciclo de vida de la vista.

## 2. Definición del Módulo Profundo: `EventDraftBatch`

El módulo `EventDraftBatch` es un módulo profundo ubicado en el paquete `com.finsim.domain` (`kmp/shared`). Presenta una interfaz compacta e inmutable que oculta una implementación densa encargada de la preparación de borradores, la modificación, el seguimiento de cambios pendientes en tiempo constante $O(1)$, la asignación determinista de identificadores y la validación previa a la confirmación de dominio.

### Clasificación de Dependencias
- **Categoría**: *In-process* (cálculo puro, estado en memoria, sin E/S).
- **Estrategia**: El módulo se prueba directamente a través de su interfaz sin requerir adaptadores de prueba (*mocks*) ni puertos de transporte.

---

## 3. Interfaz del Módulo

> *«La interfaz es la superficie de prueba.»*

### 3.1 Tipos de la Interfaz

```kotlin
package com.finsim.domain

enum class EventDraftKind {
    Extra,
    Recurring,
    Missed,
    Catchup;

    val displayName: String
    val wireKind: String
}

data class EventDraftItem(
    val id: String,
    val kind: EventDraftKind,
    val month: Int,
    val amount: String = "100.00",
    val endMonth: Int = month,
    val every: Int = 1
)

enum class DraftField {
    Month,
    Amount,
    EndMonth,
    Every,
    General
}

data class DraftValidationError(
    val eventId: String,
    val field: DraftField,
    val message: String
)

sealed class BatchCommitResult {
    data class Success(val events: List<LoanEvent>) : BatchCommitResult()
    data class Failure(val errors: List<DraftValidationError>) : BatchCommitResult() {
        val summaryMessage: String
    }
}
```

### 3.2 Operaciones y Firmas

```kotlin
class EventDraftBatch {
    // Lectura en tiempo constante O(1)
    val isDirty: Boolean
    val pendingChangeCount: Int
    val items: List<EventDraftItem>
    val size: Int
    val isEmpty: Boolean

    fun getItem(id: String): EventDraftItem?

    // Transformaciones inmutables de borrador
    fun add(
        month: Int,
        kind: EventDraftKind = EventDraftKind.Extra,
        amount: String = "100.00",
        endMonth: Int = month,
        every: Int = 1
    ): EventDraftBatch

    fun updateMonth(id: String, month: Int): EventDraftBatch
    fun updateKind(id: String, kind: EventDraftKind): EventDraftBatch
    fun updateAmount(id: String, amount: String): EventDraftBatch
    fun updateRecurring(id: String, endMonth: Int, every: Int): EventDraftBatch
    fun updateItem(id: String, transform: (EventDraftItem) -> EventDraftItem): EventDraftBatch

    fun remove(id: String): EventDraftBatch
    fun discard(): EventDraftBatch

    // Confirmación y validación de dominio
    fun commit(): BatchCommitResult

    companion object {
        fun from(events: List<LoanEvent>): EventDraftBatch
        fun empty(): EventDraftBatch
    }
}
```

### 3.3 Invariantes del Módulo
1. **Invariante de Línea Base**: Una instancia creada mediante `EventDraftBatch.from(events)` garantiza `isDirty == false` y `pendingChangeCount == 0`.
2. **Invariante de Identificadores Deterministas**: Las adiciones mediante `add(...)` asignan identificadores únicos monótonos con formato `ev_draft_{seq}_m{month}` derivados de una secuencia interna que previene colisiones con eventos preexistentes y eliminados.
3. **Invariante de Reversión en $O(1)$**: Si un elemento existente se modifica y posteriormente se restaura a su valor idéntico de la línea base, la marca de modificación se desaloja del conjunto contable, restaurando `isDirty == false` en tiempo constante $O(1)$.
4. **Invariante de Descarte**: `discard()` restablece el estado del lote a la línea base en tiempo $O(1)$.
5. **Invariante de Ordenación**: `commit()` entrega la colección de eventos `LoanEvent` estrictamente ordenada cronológicamente por mes e identificador.

### 3.4 Modos de Error
`commit()` nunca propaga excepciones no controladas hacia el llamador. Retorna `BatchCommitResult.Failure` conteniendo errores estructurados:
- `Month`: Fuera del rango admitido `1..MAX_MONTHS` (1.200 meses).
- `Amount`: Importe en blanco, no numérico, menor o igual a cero, o con más de dos decimales (violación del estándar de importes monetarios de `docs/financial-conventions.md`).
- `EndMonth`: En abonos recurrentes, `endMonth < month` o fuera de `1..MAX_MONTHS`.
- `Every`: Frecuencia de recurrencia menor a 1 periodo.
- `General`: Duplicidad de identificadores.

---

## 4. Lo que la Implementación Oculta tras la Costura (Seam)

Detrás de la costura de su interfaz, la implementación oculta:
1. **Contabilidad de Suciedad en $O(1)$**: Mantiene tres conjuntos disjuntos (`addedIds`, `removedBaselineIds`, `modifiedBaselineIds`) y un mapa de referencia indexado de la línea base (`baselineMap`). Cada mutación evalúa la igualdad estructural con la línea base y ajusta los conjuntos contables sin recorrer ni proyectar la lista completa de eventos.
2. **Generador Determinista de Secuencias**: Calcula la semilla inicial analizando los sufijos numéricos existentes y mantiene un contador monótono inmutable para evitar colisiones entre adiciones y eliminaciones sucesivas.
3. **Traducción y Mapeo Polimórfico**: Oculta la conversión bidireccional entre la jerarquía polimórfica sellada de `LoanEvent` (`Extra`, `Recurring`, `Missed`, `Catchup`) y los borradores visuales uniformes.
4. **Validación de Precisión Decimal**: Verifica que las cadenas monetarias respeten la regla de redondeo y escala financiera (máximo dos posiciones decimales) antes de invocar la aritmética de `Decimal`.

---

## 5. Ejemplos de Uso

### 5.1 En Pruebas Unitarias Sin Interfaz Gráfica (Headless)

```kotlin
@Test
fun `verificacion de ciclo de vida del borrador`() {
    val baseline = listOf(LoanEvent.Extra(id = "ev1", month = 3, amount = "500.00"))
    val batch = EventDraftBatch.from(baseline)

    // Modificación y dirty tracking O(1)
    val modified = batch.updateAmount("ev1", "600.00")
    assertTrue(modified.isDirty)

    // Reversión exacta
    val reverted = modified.updateAmount("ev1", "500.00")
    assertFalse(reverted.isDirty)

    // Adición con ID determinista
    val added = reverted.add(month = 7, kind = EventDraftKind.Missed)
    assertTrue(added.isDirty)

    // Confirmación validada
    val result = added.commit()
    assertTrue(result is BatchCommitResult.Success)
    assertEquals(2, result.events.size)
}
```

### 5.2 En la Vista Presentacional Compose (`EventEditor.kt`)

```kotlin
@Composable
fun EventEditor(
    currency: Currency,
    selectedMonth: Int,
    currentEvents: List<LoanEvent>,
    onApplyEvents: (List<LoanEvent>) -> Boolean,
    onPendingChange: (Boolean) -> Unit
) {
    var batch by remember(currentEvents) {
        mutableStateOf(EventDraftBatch.from(currentEvents))
    }

    // Consulta directa O(1) sin bucles de diferencias
    val hasPending = batch.isDirty

    LaunchedEffect(hasPending) {
        onPendingChange(hasPending)
    }

    // Acciones delegadas limpiamente a la interfaz del módulo
    Button(onClick = { batch = batch.add(month = selectedMonth) }) {
        Text("Añadir mes")
    }

    Button(
        onClick = {
            when (val result = batch.commit()) {
                is BatchCommitResult.Success -> onApplyEvents(result.events)
                is BatchCommitResult.Failure -> showError(result.summaryMessage)
            }
        },
        enabled = hasPending
    ) {
        Text("Aplicar cambios")
    }

    OutlinedButton(
        onClick = { batch = batch.discard() },
        enabled = hasPending
    ) {
        Text("Descartar cambios")
    }
}
```

---

## 6. Estrategia de Dependencias y Adaptadores

- **Sin Adaptador de Simulación**: Al pertenecer a la categoría *in-process*, el módulo no requiere adaptadores simulados (*mocks*) ni puertos abstractos. La clase concreta `EventDraftBatch` es la única implementación necesaria.
- **Costura Limpia entre Presentación y Dominio**: La vista Compose consume el módulo mediante parámetros inmutables y llamadas directas. La presentación no contiene reglas de negocio ni validaciones aritméticas, adquiriendo el rol de un adaptador de interfaz de usuario.
- **Portabilidad Multiplataforma**: Al estar ubicado en `:shared` (`commonMain`), el módulo funciona idénticamente en JVM, Android y Wasm/JS sin alterar su interfaz.

---

## 7. Balance y Compromisos de Diseño (Trade-offs)

### Dónde el Apalancamiento (Leverage) es Alto
1. **Eliminación de Bucles de Renderizado**: Reemplaza el diff lineal $O(N)$ en cada recomposición de Compose por una lectura de propiedad booleana en $O(1)$.
2. **Localidad de Invariantes Financieras**: La regla de dos decimales, la positividad de importes y los límites temporales residen exclusivamente en `commit()`.
3. **Superficie de Pruebas Unificada**: Las pruebas del ciclo de vida de los borradores se ejecutan en milisegundos en la JVM a través de `:shared:jvmTest`, sin necesidad de instrumentación de Compose o emuladores de Android.

### Dónde el Apalancamiento es Delicado o Fino
1. **Inmutabilidad en Colecciones**: Cada adición o eliminación produce una copia inmutable de la lista de elementos (`items + newItem`). Para lotes normales de simulación (de 1 a 50 eventos), el costo de asignación es insignificante en comparación con la reactividad de Compose, pero para lotes masivos hipotéticos (> 10.000 eventos) requeriría estructuras persistentes optimizadas.
2. **Edición Parcial de Cadenas**: Durante la captura en cajas de texto, importes temporales no válidos (por ejemplo, una cadena vacía mientras el usuario escribe) se preservan como cadenas en el borrador; la validación estricta se pospone deliberadamente hasta el momento de `commit()`.
