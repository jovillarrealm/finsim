# Consolidación del Módulo WorkspaceStore

## 1. Visión general y fundamento de diseño

Este documento formaliza la **Dirección 2 de la revisión arquitectónica**: la consolidación de un **módulo profundo** denominado `WorkspaceStore` en `:shared` (`com.finsim.workspace`), absorbiendo la coordinación de persistencia, sincronización de simulación dual, límites de tamaño, verificación de versiones y seguimiento de borradores pendientes de eventos.

Bajo los principios de diseño de módulos profundos:
- Un **módulo profundo** presenta una **interfaz pequeña** con una **implementación rica y profunda**, otorgando **alto apalancamiento** a los llamadores y **localidad estricta** a los mantenedores.
- La costura de almacenamiento (`ScenarioStorage`) pertenece a la categoría de dependencias **local-substitutable**.
- Se cumple el principio: *"Un adaptador significa una costura hipotética. Dos adaptadores significan una costura real"*. En Finsim existen dos adaptadores de producción reales (`AndroidScenarioStorage` vía `SharedPreferences` y `WasmScenarioStorage` vía `localStorage`) más un adaptador de prueba en memoria (`InMemoryScenarioStorage`).

---

## 2. Diagnóstico del estado anterior (Módulos superficiales y complejidad dispersa)

Antes de la consolidación:
1. `ScenarioPersistence.kt` era un módulo superficial: exponía directamente `serializeScenario`, `parseScenario`, `saveScenario`, `loadScenario`, `finsimJson`, `SCENARIO_STORAGE_KEY` y `MAX_FILE_SIZE_BYTES`.
2. El llamador de presentación (`App.kt` en `:composeApp`) se veía forzado a asumir responsabilidades de orquestación ajenas a la UI:
   - **Simulación dual manual**: coordinar la copia del escenario sin eventos (`scenario.copy(events = emptyList())`) y disparar manualmente `simulateLoan` dos veces (para obtener la pista base y la pista modificada).
   - **Gestión de fallos de inicialización**: interceptar excepciones en `loadScenario(storage)` y arbitrar la recuperación manual cargando `exampleScenario`.
   - **Serialización y validación de 2 MB**: invocar la serialización y validar el resultado antes de despachar eventos a la costura de almacenamiento.
   - **Seguimiento de borrador sucio (`pendingEvents`)**: mantener una bandera booleana separada para bloquear el guardado o la exportación si el usuario tenía ediciones de eventos por lotes sin aplicar.
   - **Manejo de errores vs. avisos**: gestionar bloques try/catch repetitivos para cada acción del usuario (guardar, exportar, aplicar eventos, actualizar condiciones).

Al aplicar la **prueba de eliminación**: si eliminábamos esa lógica de `App.kt`, la complejidad reaparecía inmediatamente en cualquier otro llamador (pruebas automatizadas sin cabeza, herramientas CLI o pantallas alternativas). El comportamiento pertenecía a un módulo profundo del dominio.

---

## 3. Especificación de la Interfaz del Módulo `WorkspaceStore`

### Tipos y Estructuras

```kotlin
package com.finsim.workspace

/**
 * Instantánea inmutable del estado del espacio de trabajo.
 */
data class WorkspaceState(
    val scenario: Scenario,
    val original: LoanResult,
    val modified: LoanResult,
    val hasPendingDraft: Boolean = false,
    val isDirty: Boolean = false
)

/**
 * Resultado estructurado de las operaciones sobre WorkspaceStore.
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
```

### Métodos del Módulo

```kotlin
class WorkspaceStore(
    private val storage: ScenarioStorage = InMemoryScenarioStorage(),
    private val fallbackScenario: Scenario = exampleScenario
) {
    val state: WorkspaceState

    fun addListener(listener: (WorkspaceState) -> Unit): () -> Unit

    fun save(): WorkspaceOutcome
    fun exportJson(): Result<String>
    fun importJson(text: String): WorkspaceOutcome
    fun updateConditions(newScenario: Scenario): WorkspaceOutcome
    fun applyEvents(newEvents: List<LoanEvent>): WorkspaceOutcome
    fun setDraftPending(pending: Boolean)
    fun discardDraft()
    fun resetToExample(): WorkspaceOutcome
    fun reloadFromStorage(): WorkspaceOutcome
}
```

### Invariantes del Módulo
1. **Invariante de Sincronización de Simulación Dual**:
   `state.original` es siempre y estrictamente la simulación base sin eventos (`scenario.copy(events = emptyList())`), y `state.modified` es la simulación del escenario con todos los eventos aplicados. Se calculan atómicamente y nunca pueden desfasarse.
2. **Invariante de Límite de 2 MB**:
   Cualquier carga útil serializada o importada se valida contra `MAX_FILE_SIZE_BYTES = 2_000_000` bytes. Cualquier violación es rechazada sin alterar el estado activo.
3. **Invariante de Versión de Esquema**:
   Solo se admite `version == 1`. Cualquier versión incompatible en almacenamiento o importación es rechazada con un mensaje de dominio explicativo.
4. **Invariante de Protección de Borrador Pendiente**:
   Si `hasPendingDraft == true` (el usuario tiene un lote de eventos sin aplicar en el editor), `save()` y `exportJson()` son bloqueados categóricamente para evitar sobrescribir el almacenamiento local con datos incompletos.
5. **Invariante de Recuperación ante Fallos (Tolerancia a Desastres)**:
   Si el adaptador de almacenamiento contiene JSON corrupto, versión no soportada o falla de I/O al inicializar, `WorkspaceStore` se recupera automáticamente cargando `fallbackScenario` (`exampleScenario`) y emite una advertencia en el log, garantizando que la aplicación nunca colapse al arrancar.
6. **Invariante de Atomicidad Transaccional**:
   Si una operación de importación o actualización de condiciones falla por validación financiera (p. ej. capital negativo, tasas imposibles o meses > 1.200), el estado previo del espacio de trabajo se conserva intacto.

### Restricciones de Ordenamiento
- Edición por lotes: `setDraftPending(true)` coloca el almacén en estado de borrador pendiente.
- Para guardar o exportar, el llamador debe invocar `applyEvents(events)` o `discardDraft()`.

### Modos de Error
- `WorkspaceOutcome.Failure` describe el motivo exacto en español acorde a las convenciones financieras (`docs/financial-conventions.md` y `docs/product-brief.md`):
  - Borrador pendiente: *"Hay cambios de eventos pendientes. Aplica o descarta el lote antes de guardar."*
  - Error de almacenamiento: *"No se pudo guardar el escenario en este dispositivo. Exporta una copia JSON."*
  - Exceso de tamaño: *"El archivo supera el máximo de 2 MB."*
  - Versión inválida: *"La versión del escenario no es compatible. Se admite la versión 1."*
  - Datos inválidos: *"El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses."*

---

## 4. Qué oculta la implementación detrás de la costura

El módulo absorbe de forma privada dentro de su implementación:
- La configuración del serializador JSON multiidioma (`finsimJson` con discriminador `"kind"`, sangrado legible y tolerancia a claves desconocidas).
- La validación del límite de tamaño de 2.000.000 bytes.
- La inspección sintáctica del árbol JSON y la comprobación de versión de esquema 1 antes de la deserialización tipada.
- La ejecución en tándem de `simulateLoan(origScenario)` y `simulateLoan(scenario)`.
- El manejo defensivo de excepciones de almacenamiento en plataformas móviles o navegadores (p. ej. cuotas excedidas en `localStorage` o bloqueos en `SharedPreferences`).
- El registro estructurado de eventos de almacenamiento y auditoría con Touchlab Kermit (`FinsimLog.storage`).

---

## 5. Estrategia de Dependencias y Adaptadores en la Costura

- **Categoría de Dependencia**: `local-substitutable`. El almacenamiento de escenarios es local al dispositivo y cuenta con un sustituto en memoria idéntico para pruebas unitarias.
- **Costura interna**: la interfaz `ScenarioStorage`:
  ```kotlin
  interface ScenarioStorage {
      fun getItem(key: String): String?
      fun setItem(key: String, value: String)
  }
  ```
- **Adaptadores**:
  1. `AndroidScenarioStorage`: adaptador de producción en `androidMain`, respaldado por Android `SharedPreferences` (`Context.MODE_PRIVATE`).
  2. `WasmScenarioStorage`: adaptador de producción en `wasmJsMain`, respaldado por el navegador web (`window.localStorage`).
  3. `InMemoryScenarioStorage`: adaptador sustituto para pruebas y ejecución sin cabeza en `commonMain`/`commonTest`, respaldado por `mutableMapOf<String, String>()`.

Al existir dos adaptadores de producción reales más un adaptador de prueba, la costura está plenamente justificada y no constituye indirección innecesaria.

---

## 6. Ejemplos de Uso

### Uso desde la interfaz de usuario (`App.kt` en Compose Multiplatform)
```kotlin
@Composable
fun App(
    storage: ScenarioStorage = remember { InMemoryScenarioStorage() },
    store: WorkspaceStore = remember(storage) { WorkspaceStore(storage) }
) {
    var workspace by remember(store) { mutableStateOf(store.state) }

    // Guardado directo con retroalimentación automática
    Button(
        onClick = {
            val outcome = store.save()
            workspace = store.state
            when (outcome) {
                is WorkspaceOutcome.Success -> { noticeMessage = outcome.message }
                is WorkspaceOutcome.Failure -> { errorMessage = outcome.error }
            }
        },
        enabled = !workspace.hasPendingDraft
    ) {
        Text("Guardar")
    }

    // Aplicación de lote de eventos de amortización
    EventEditor(
        currentEvents = workspace.scenario.events,
        onApplyEvents = { newEvents ->
            val outcome = store.applyEvents(newEvents)
            workspace = store.state
            outcome.isSuccess
        },
        onPendingChange = { pending ->
            store.setDraftPending(pending)
            workspace = store.state
        }
    )
}
```

### Uso desde pruebas sin cabeza (`WorkspaceStoreTest.kt`)
```kotlin
@Test
fun testHeadlessSimulationAndDraftGating() {
    val storage = InMemoryScenarioStorage()
    val store = WorkspaceStore(storage)

    // Modificar condiciones y verificar cálculo dual automático
    store.updateConditions(store.state.scenario.copy(termMonths = 24))
    assertEquals(24, store.state.original.rows.size)
    assertEquals(24, store.state.modified.rows.size)

    // Bloqueo de persistencia mientras hay borrador pendiente
    store.setDraftPending(true)
    val saveOutcome = store.save()
    assertFalse(saveOutcome.isSuccess)

    // Aplicación de eventos extraordinarios y desbloqueo
    val extraPayment = LoanEvent.Extra(id = "p1", month = 2, amount = "50000000.00")
    store.applyEvents(listOf(extraPayment))
    assertTrue(store.save().isSuccess)
    
    // Recuperación exacta desde almacenamiento
    val secondStore = WorkspaceStore(storage)
    assertEquals(store.state.scenario.name, secondStore.state.scenario.name)
}
```

---

## 7. Análisis de Compromisos (Trade-offs)

### Dónde el apalancamiento es alto
- **Coordinación de simulación dual**: ningún llamador necesita acordarse de despojar los eventos para simular la línea base; ambas proyecciones se entregan calculadas y atómicamente sincronizadas.
- **Protección de datos y límites**: la regla de 2 MB y la versión 1 se garantizan en un solo lugar, impidiendo que datos corruptos se propaguen a la memoria activa.
- **Facilidad de pruebas**: el flujo completo de vida de un escenario se prueba sin necesidad de montar Compose ni emular un dispositivo móvil.
- **Localidad**: cualquier evolución en el formato de serialización o política de caché de cálculos solo modifica `WorkspaceStore`.

### Dónde el apalancamiento es delgado (y se mantuvo deliberadamente fuera)
- **Estado de navegación visual**: la pestaña seleccionada (`selectedTab`: préstamo, conversor, conciliación), el mes bajo foco (`selectedMonth`) y el modo de visualización de tabla (`tableMode`: original vs modificado) son puramente presentacionales y se mantuvieron en la UI para no contaminar el almacén con preocupaciones de representación gráfica.

---

## 8. Verificación Técnica

- Verificado con `./gradlew :shared:jvmTest` (35 pruebas unitarias aprobadas, 0 fallos).
- Verificado con `./gradlew :composeApp:compileCommonMainKotlinMetadata` (código fuente común de Compose compila sin advertencias).
- Verificado con `./gradlew :composeApp:compileDebugKotlinAndroid` (compilación de Android pasando exitosamente).
- Verificado con `./gradlew :composeApp:compileKotlinWasmJs` (compilación de destino Web WasmJs pasando exitosamente).
