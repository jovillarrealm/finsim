# 02: Auditoría Mensual de Conciliación desde Extracto PDF (Tier 1)

**What to build:** En la pestaña "Conciliación", el usuario puede presionar `[Importar extracto]` para cargar un PDF bancario mensual. El extractor identifica los cobros efectivos de ese período (número o fecha de mes, capital amortizado, intereses cobrados, seguros facturados y saldo final resultante). Tras validar o ajustar los valores en la Hoja de Revisión, al pulsar `[Confirmar e importar]`, se inyectan directamente en el módulo de conciliación (`reconcileRow`), desplegando de inmediato la tarjeta de diferencias entre los cobros del banco y el cronograma simulado.

**Blocked by:** 01: Ingesta de Escenario vía Texto Digital y Selector Regulatorio (Tier 1)

**Status:** completed

- [x] Extensión del extractor Tier 1 para identificar campos específicos de extracto mensual (`StatementRow`: capital, interés, seguro, saldo de cierre).
- [x] Botón de `[Importar extracto]` integrado en la tarjeta de `ReconciliationView.kt`.
- [x] Adaptación de `ExtractionReviewSheet` para modo conciliación (resaltando cobros del banco vs cuota del mes seleccionado).
- [x] Inyección automática del `StatementRow` confirmado en la función `reconcileRow(stmt, original.rows)`.
- [x] Pruebas unitarias que validen la extracción de filas de extracto y cálculo de diferencias pasando con `./gradlew :shared:jvmTest`.
