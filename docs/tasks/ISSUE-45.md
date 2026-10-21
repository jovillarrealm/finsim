# Issue #45 — corpus privado y revisión de importación

- Responsable: `/root/issue45_implement`; coordinación: `/root`.
- Estado: implementación en curso; integrar la base `e06ebc0` y repetir verificaciones antes de revisión independiente.
- Rama: `codex/spec37-issue45`.
- Base reservada: `fe291ae76403d83e2cd50aec69a3b503c1c14d33`; la coordinación integró después #34 en `e06ebc0565dd500adcb1eaa265f2a873beaac4b5`.
- Alcance: revisión/mapeo/UI y metadatos mínimos del contrato compartido. Sin runtime, nuevos productos/países ni reglas financieras. No modificar tablero ni `SPEC37.md`.

## Cambios

- `ExtractedDocumentData` conserva tipo de documento, monedas y significado de saldos, plazo, cuota y seguro. Los valores originales permanecen separados de las correcciones del revisor; los cargos ausentes no se convierten en cero.
- Los mapeadores rechazan resúmenes de cuenta, monedas mixtas/no admitidas y datos ambiguos. Un préstamo solo se crea tras completar capital, tipo de plazo, moneda, primer periodo, tasa y confirmaciones requeridas. La fecha de corte no se precarga como primer periodo; una tasa nominal no recibe frecuencia anual predeterminada. No se usa `exampleScenario`; una tabla de conciliación tampoco recibe mes o saldo de capital inventados.
- El parser textual y el contrato VLM clasifican esos significados; el VLM conserva campos ausentes y principal sin clasificar en lugar de convertirlos en capital inicial.
- La UI muestra una sección de datos originales, campos corregibles separados y confirmación antes de aplicar. Separa capital compuesto, capital, cuota base/total, plazo original/restante y valor asegurado/cargo periódico; bloquea ambos destinos para resúmenes de cuenta o monedas mezcladas.
- Consumidores: #32 debe manejar los nuevos metadatos/ausencias sin asumir moneda, tipo de saldo ni importación aceptada; #33 debe mantener la misma separación al exportar/mostrar datos revisados. No se modifica su runtime.

## Evidencia y límites

- RED de comportamiento sobre base limpia `fe291ae`: `:shared:jvmTest --tests com.finsim.ingestion.ImportReviewRegressionTest` ejecutó un test que exige que `ExtractedDocumentData().toScenario()` lance `IllegalArgumentException`; falló por `AssertionError`, confirmando que la implementación base devolvía el préstamo de ejemplo. Log fuera del repositorio: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue45-baseline-red.log`.
- Primera pasada de la implementación: `:shared:jvmTest` ejecutó 122 tests; 121 pasaron y uno falló por una expectativa obsoleta que esperaba que la fecha de corte sustituyera al periodo inicial explícito. La aserción se corrigió para esperar el periodo seleccionado; la repetición está pendiente tras incorporar #34. No se presenta esa pasada como GREEN.
- Durante la inspección posterior se cerraron dos supuestos que el primer pase aún precargaba: el primer periodo desde la fecha de corte y frecuencia 12 para tasas nominales sin dato explícito. Hay aserciones nuevas para frecuencia ausente/explícita; la corrida final sigue pendiente.
- Fixtures controlados verifican parsing, separación de metadatos y decisiones de mapeo; no acreditan inferencia real del modelo ni interpretación semántica humana.
- Revisión semántica humana de la referencia sigue pendiente y no bloquea la entrega de código. Referencia y PDFs permanecen privados; no copiar valores ni identificadores al repositorio o al comentario público.
- JVM/host/Wasm y cualquier comprobación de dispositivo aún deben quedar registrados individualmente con comando, commit y resultado reales antes de pedir revisión.
