# Issue #33 — Granite-Docling real en Web

- Responsable: `/root/issue33_docling_luna`; coordinación `/root`; revisión independiente secuencial.
- Estado: implementación abierta. La rama tiene integración y pruebas, pero el criterio de inferencia real y validación semántica no está demostrado; no cerrar el issue.
- Rama: `codex/spec37-issue33`, con la punta `6b0916f` de `codex/spec37-verification` integrada. No se modificaron `docs/tracking.md` ni `SPEC37`.
- Fuente canónica: [issue #33](https://github.com/jovillarrealm/finsim/issues/33) y [issue #37](https://github.com/jovillarrealm/finsim/issues/37).
- Alcance implementado: runtime Granite-Docling local en Web Worker WasmJs; entrada PDF primera página a 150 DPI y PNG/JPEG; instalación explícita; caché e integridad por tamaño/SHA-256; revisión común con origen Docling visible; mapeo semántico de DocTags/OTSL; límites de archivo/píxeles, cancelación y watchdog por inactividad/progreso.
- Semántica financiera: cuota base, pago total con seguros y significado desconocido siguen separados. Filas ambiguas de tabla y conflictos se dejan sin mapear y aparecen como advertencias; no se suman cuotas ni se imputa el total con seguros como cuota base.
- Privacidad: el navegador probó solo los fixtures sintéticos del repositorio. No se usó ni publicó el corpus privado de referencia. Los documentos permanecen en memoria; tras la instalación, las solicitudes observadas durante el análisis fueron únicamente locales.

## Correcciones de la revisión de `62de3c6`

- La extracción conserva periodicidad de seguro (`mensual`, `anual`, otra o no identificada). Una prima anual o sin periodicidad explícita ya no se interpreta como cargo mensual ni se convierte en `totalInsurance`; permanece visible para revisión con una advertencia. Los cargos expresamente mensuales y el flujo de seguro confirmado por una persona conservan su asignación mensual.
- La respuesta del Worker `stoppedAtTokenLimit` llega al parser y genera una advertencia visible de salida posiblemente incompleta.
- Un error del Worker termina esa instancia, limpia la solicitud y el watchdog pendientes y deja que el siguiente intento cree otro Worker.
- Se agregó el perfil invocable `npm run verify:docling`. Usa solamente el PDF sintético versionado; verifica los hashes servidos del build, Chrome real, respuesta Worker, semántica, solicitudes externas y almacenamiento del documento. Guarda informe JSON/Markdown fuera del repositorio en `%USERPROFILE%\.finsim\verification\docling\<runId>\`. Requiere una distribución fresca (`npm run build`) y un Chrome disponible.
- `npm run check` es el punto invocable de #37 para typecheck, verificaciones financieras/dispositivo, pruebas del Worker y pruebas KMP (JVM y Android Host). La ejecución aquí usó Gradle 9.3.1 mediante `PATH`; el Gradle global configurado era 8.11.1.

## Verificación

- Manifiesto fijado: `onnx-community/granite-docling-258M-ONNX`, revisión `e8602580df77443fc3421cf3bae0601da601e5c6`, 17 artefactos y `213192793` bytes (203.3 MiB). La instalación explícita terminó con 17/17 artefactos verificados.
- Worker Node: `npm test` pasó 10/10 pruebas, incluyendo URL/revisión exactas, SHA-256, reglas GET/HEAD sin cuerpo (también `Request` con body heredado y `init.body: undefined` tanto en rutas locales como en URLs fijadas) y fallback WebGPU→WASM.
- KMP: `:shared:jvmTest` pasó 170 pruebas. `clean :composeApp:wasmJsBrowserDistribution` terminó con Gradle 9.3.1 en 3m53s; generó desde cero `docling/worker.mjs`, `docling/pdf.worker.mjs` y los recursos ONNX Runtime asyncify locales. Tras la última corrección de red, la distribución se recompiló y copió en 9 s.
- Suite general `npm test`: 11 casos de navegador pasaron contra la distribución Wasm; el siguiente caso quedó sin salida por 90 s y se interrumpió. No es un resultado global aprobado; esa suite reúne pruebas no relacionadas con #33.
- Chrome real contra la distribución oficial WasmJs: la interfaz permitió seleccionar el PDF sintético; PDF.js y los recursos WASM locales cargaron. Transformers.js falló primero por el tokenizador incompleto y luego por inicialización WebGPU; se añadieron los cinco archivos fijados faltantes, el path de revisión fijo y el runtime WASM asyncify local.
- Retest en Chrome con captura de mensajes reales del Worker: la verificación informó 17/17 artefactos y PDF.js rasterizó la página 1 a 1241×1754. Llegaron las etapas `processor`, `model` y dos eventos `loading-model` para WebGPU separados por 1,005 ms. Luego no hubo más mensajes durante ~184 s; no llegaron `model-fallback`, `ready`, `generating`, tokens, resultado ni error del Worker. El watchdog canceló al vencer la inactividad configurada en 180 s. Esto sitúa la falta de resultado durante la carga/inicialización observada en este navegador y no demuestra un fallo general del modelo.
- PNG/JPEG: interfaz y límites están implementados; no se verificó inferencia real de imagen porque la carga del modelo no completó en Chrome.
- Regresión de seguros y tokens: `:shared:jvmTest --tests com.finsim.ingestion.DoclingDocTagsParserTest --tests com.finsim.ingestion.DocumentTextParserTest` pasó tras incluir primas anuales, totales sin evidencia mensual y la advertencia de límite de tokens.
- Verificación invocable: `npm run check` pasó (typecheck, 21 verificaciones, 10 pruebas del Worker, shared JVM y Android Host). `:composeApp:wasmJsBrowserDistribution` pasó con Gradle 9.3.1 en 3m25s; webpack reportó recomendaciones de tamaño para los bundles. El test de ciclo de vida del Worker pasó en Chrome contra esa distribución (1/1). `git diff --check` no encontró errores de whitespace.
- `npm run verify:docling` está disponible pero **no se ejecutó** en esta revisión: no se obtuvo una causa que justificara reintentar el perfil real. El último intento registrado terminó por inactividad durante `loading-model` WebGPU; el perfil no tiene resultado de inferencia ni aceptación semántica. La salida del perfil real continúa pendiente y no se declara PASS.

## Pendiente para aceptar #33

1. Obtener un resultado de inferencia real del fixture sintético en navegador (PDF y, por separado, PNG/JPEG), con progreso observable o una ventana de espera adecuada a la velocidad del runtime.
2. Revisar valores/campos contra el fixture esperado: origen Docling, moneda, periodo, principal, plazo, tasa, cuota base separada del total con seguros; las ambigüedades deben permanecer sin asignar.
3. Confirmar que la revisión editable presenta advertencias antes de permitir aplicar los datos. No aplicar el fixture al escenario durante esta prueba.

La compilación o instalación íntegra del modelo por sí sola no acredita inferencia ni aceptación semántica. La tarjeta queda abierta hasta completar esos pasos.
