# Issue #32 — Runner Gemma Android real

- Responsable de implementación: `/root/issue32_implement_luna`, reemplazo secuencial del implementador anterior.
- Estado: punta revisada de #45 integrada en `c1199b4`; runtime Android, descarga, rasterización y diálogo de importación cableados. El árbol final de código `4d6117c0ac6ff7a959f3bade166c9b596bea0343` pasa Wasm y host; la suite JVM pasó en un árbol anterior con los mismos fuentes compartidos. El APK debug se construyó antes del último cambio de dispatcher. Pendientes revisión independiente y prueba sintética en Samsung. **#32 sigue abierta; no se descargó el modelo ni se ejecutó inferencia.**
- Rama: `codex/spec37-issue32`.
- Base del trabajo: checkpoint `3cf6f577` más merge de #45 `c1199b4` (sobre #34).
- Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue32/finsim`.
- Alcance canónico: [#32](https://github.com/jovillarrealm/finsim/issues/32), aclaraciones de [#37](https://github.com/jovillarrealm/finsim/issues/37) y handoff en `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/AGENT-CONTEXT.md` / `COORDINATION.md`.

## Historial de preparación

Antes de integrar #34, el slice inicial del gate se probó en RED/GREEN aislado: el baseline permitía 7,999 GiB y la implementación aislada lo bloqueó. Los logs siguen en `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-gate-red.log` y `issue32-gate-green.log`; su regresión parcial fue 17/17 en `issue32-gate-regression.log`. Eso acredita aquel slice aislado, no el runtime ni la entrega actual.

La prueba mínima anterior con Kotlin 2.2.20 no podía leer metadata Kotlin 2.4 del AAR LiteRT-LM 0.18.0 (`issue32-runtime-compatibility.log`). #34 integró Kotlin 2.4.21 / AGP 9.1.1 / Gradle 9.3.1 sin bypass; el runtime actual compila contra el AAR real. El tamaño/SHA fijados del modelo se contrastaron previamente con la API de Hugging Face; no se descargaron pesos.

## Código incluido en esta rama

- Artefacto fijado a `litert-community/gemma-4-E2B-it-litert-lm`, revisión `7fa1d78473894f7e736a21d920c3aa80f950c0db`, archivo `gemma-4-E2B-it.litertlm`, 2.583.085.056 bytes, SHA-256 `ab7838cdfc8f77e54d8ca45eadceb20452d9f01e4bfade03e5dce27911b27e42`.
- `AndroidModelDownloader` transmite el archivo desde la URL de revisión inmutable. `AndroidModelStorage` escribe en `filesDir/models` a un `.part`, calcula SHA-256 de los bytes que persiste y publica el archivo/marker atómicamente solo si coinciden extensión, tamaño y hash fijados. La ruta es privada y persistente.
- `AndroidGemmaVisionRunner` adapta `VlmVisionRunner` a LiteRT-LM 0.18.0: entrega la página PNG como `Content.ImageBytes`, usa JSON restringido y muestreo determinista, desactiva speculative decoding, comprueba el artefacto antes de cargar y reporta errores sin inventar datos. La presión actual usa `ActivityManager.MemoryInfo.lowMemory`; se registra memoria disponible y PSS como diagnósticos. Backend CPU.
- El gate exige al menos 8 GiB de RAM total y bloquea presión actual por separado. En Android, `getDeviceCapabilityGate()` consulta `ActivityManager`; sin contexto falla cerrado. Web/JS/Wasm declaran Gemma no disponible. La condición de RAM no se presenta como garantía de espacio libre suficiente.
- `AndroidPdfRasterizer` alimenta `PdfRenderer` con un descriptor seekable sobre los bytes en memoria; no escribe PDF ni raster a temporales. La actividad registra el contexto de aplicación requerido.
- `FakeVlmVisionRunner` ya no decodifica bytes de imagen como texto ni fabrica una respuesta; siempre falla en análisis y nunca reporta modelo listo. Los tests de orquestación que deban cubrir éxito usarán un stub local con literal controlado y etiqueta de contrato, no un test de inferencia.
- Se actualizó el texto del componente de descarga al tamaño fijado y a errores de disponibilidad. Se conserva la prueba en `GemmaEligibilityTest.kt` que evita que una elegibilidad forzada eluda el gate de 8 GiB.
- `DocumentImportDialog` consume un runtime por plataforma, muestra descarga solo en Android y mantiene separado el texto pegado de los bytes PDF. Texto pegado va al parser Tier 1; el PDF va al rasterizador y al runner real solo tras gate y modelo validado. El flujo web muestra indisponibilidad y conserva la entrada manual.
- Android SAF lee el PDF en `Dispatchers.IO` sin permiso persistente ni copia a disco. El límite es 25 MiB por archivo y 12 millones de píxeles por página rasterizada a 150 DPI; los excesos y OOM reportan errores claros. El diálogo mantiene el PDF solo en memoria y muestra que procesa la página 1; la hoja de revisión indica la página analizada.
- El orquestador ya no decodifica `pdfBytes` para extracción ni para escoger página. Si no hay texto manual, la página elegida es la primera. Cancelación se propaga como cancelación y el diálogo cancela descarga/cierra el runner al cerrarse.
- Los dos casos que antes esperaban éxito del fake ahora usan un stub de test con literal controlado, con nombres/comentarios que aclaran que acreditan el contrato de orquestación y no inferencia. Se añadió cobertura para PDF con texto seleccionable que debe pasar por raster, cancelación y exceso del límite de tamaño.

## Verificación

Las tareas Gradle válidas se ejecutaron secuencialmente con Gradle 9.3.1, JDK 21.0.12.1, `--max-workers=1` y Kotlin in-process. Base: `c1199b4f287117c08dbf525c06c864c08d14a2a1`. Árboles de código registrados: `f79650b6f7b76f4086bfc78069fe4da802be407d` (primera corrida completa), `7fa87835b4aed53adbc0111f157ffdbb03764364` (cierre coordinado del runtime), `4d6117c0ac6ff7a959f3bade166c9b596bea0343` (dispatcher final multiplataforma). Las diferencias entre ellos están en el cierre del runtime en la UI común; los fuentes `shared` no cambiaron después de la primera corrida.

| Comprobación | Resultado |
| --- | --- |
| `:shared:jvmTest`, árbol `f79650b…` | **PASS, 143 pruebas, cero fallos/errores/omitidas.** Incluye contratos de orquestación y límites de rasterización. Los fuentes compartidos no cambiaron en los árboles UI posteriores. |
| `:composeApp:testAndroidHostTest`, árboles `f79650b…`, `7fa87835…` y `4d6117c…` | **PASS, 5 pruebas en cada corrida, cero fallos/errores/omitidas.** Compila el diálogo Compose final y el selector SAF Android. |
| `:androidApp:assembleDebug`, árbol `7fa87835…` | **PASS.** Generó `kmp/androidApp/build/outputs/apk/debug/androidApp-debug.apk` (62.795.772 bytes). El árbol final cambió `Dispatchers.IO` a `Dispatchers.Default`; no se reconstruyó el APK después de ese cambio. Ningún APK se instaló. |
| `:composeApp:compileKotlinWasmJs`, árbol `f79650b…` | **PASS.** Compila el actual web indisponible y la ruta manual. |
| `:composeApp:compileKotlinWasmJs`, árbol `7fa87835…` | **FAIL**, `Dispatchers.IO` no existe en Wasm. Se conservó el fallo y se cambió a `Dispatchers.Default`. |
| `:composeApp:compileKotlinWasmJs`, árbol `4d6117c…` | **PASS** después de esa corrección multiplataforma. |
| `git diff --cached --check` | **PASS** en los árboles registrados antes de las corridas; repetido después de actualizar esta ficha. |
| Instalación/ejecución móvil, descarga de pesos e inferencia física | **NO EJECUTADAS.** No se inició AVD ni se manipuló el teléfono; no se descargaron 2,6 GB. No se acredita rendimiento, extracción real, memoria en hardware ni launch. |

Una primera orden Gradle falló antes de ejecutar tareas porque PowerShell/Gradle parseó mal `-Pkotlin.compiler.execution.strategy=in-process`; se corrigió pasando la propiedad como argumento del proyecto. Fue un error de invocación y no se cuenta como fallo de código. El fallo Wasm de `7fa87835…` sí fue de código y queda visible arriba, junto con la corrección y el pase final.

La corrida histórica anterior al wiring registró 118 pruebas con 116 pases y 2 fallos: ambos esperaban éxito del fake común. En esta entrega esos casos quedaron como stubs de contrato explícitos y la suite actual completa pasó 143/143. El runner de producción no interpreta bytes de imagen como texto y los stubs no acreditan inferencia.

Warnings observados sin fallo: Android SDK reportó XML schema 4 a herramientas que conocen hasta 3; opt-ins Wasm existentes; deprecaciones de `MenuAnchorType`; y librerías nativas LiteRT empaquetadas tal cual.

## Prueba física pendiente en Samsung

La ruta que consume la prueba es `DocumentImportDialog` → SAF `PdfDocumentPickerButton` → `DocumentIngestionRuntime.android` → `HybridDocumentOrchestrator(forceTier2 = true)` → `AndroidPdfRasterizer` (página 1, 150 DPI, solo memoria) → `AndroidGemmaVisionRunner`. En el diálogo se selecciona Colombia, se elige el PDF sintético de una página, se descarga el modelo desde la tarjeta, se espera a que aparezca como instalado y se pulsa **Analizar página 1 con Gemma**. El texto pegado debe quedar vacío para que la entrada sea inequívocamente visual. El diálogo muestra los valores para revisión; el usuario confirma o corrige antes de importar.

El texto sintético colombiano de `DocumentImportDialog` puede servir de contenido para el PDF de prueba: capital original COP 30.000.000, plazo pactado 48 meses, tasa E.A. 19,50 %, cuota mensual COP 850.000, abono a capital COP 350.000 e interés corriente COP 477.500. Para preparar el fixture, copia ese bloque de demostración en un editor local y expórtalo como PDF de una página. El texto de muestra de la UI no es por sí solo un PDF adjunto.

La corrida real debe registrar dispositivo/Android/API, `totalRamGb`, estado `lowMemory` del gate, SHA/tamaño del modelo validado, memoria disponible antes de carga, pico PSS, latencia de inicialización e inferencia, campos devueltos y resultado de revisión, además de errores/cierre de la app. `AndroidGemmaVisionRunner` emite la línea `Gemma real image inference complete` con `GemmaInferenceDiagnostics` en Logcat. Aún faltan el PDF sintético accesible al teléfono, la descarga real de 2.583.085.056 bytes, esos valores medidos y la inferencia real. No instalar el APK debug generado sobre la instalación release; tras integrar #40/#41, preparar la actualización firmada con la misma clave antes de coordinar la instalación.

## Integración y límites pendientes

- #45 está integrado en `c1199b4`; se ajustaron solo los tests de contrato necesarios en esta entrega. La ruta manual sigue disponible en plataformas sin Gemma o cuando el gate/modelo no permite visión.
- Revisión independiente Luna/max pendiente. El APK no se instaló ni ejecutó; la medición real del gate y del comportamiento con el modelo requiere coordinación física con #43/#47 y la actualización firmada de #44.
- Después de la revisión, `/root` coordina integración y estado de GitHub. Esta entrega no hace push, merge ni cierre.
- Corrida física con el archivo real y fixture autorizado sigue pendiente; requiere coordinación separada de pesos/dispositivo. Nunca reportar PASS por omisión.
- Sin push, merge, cierre de issue ni cambios a `docs/tracking.md`, SPEC37 o tablero compartido.

## Fuentes primarias

- [LiteRT-LM 0.18.0 `Config.kt`](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/Config.kt) y [API Kotlin / getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/docs/api/kotlin/getting_started.md): opciones de salida JSON, speculative decoding, muestreador y entrada multimodal.
- [Android `PdfRenderer`](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer), [`ProxyFileDescriptorCallback`](https://developer.android.com/reference/android/os/ProxyFileDescriptorCallback) y [`StorageManager.openProxyFileDescriptor`](https://developer.android.com/reference/android/os/storage/StorageManager#openProxyFileDescriptor(int,%20android.os.ProxyFileDescriptorCallback,%20android.os.Handler)): descriptor seekable backed by an in-memory callback.
- Revisión fijada de pesos: [Hugging Face](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/7fa1d78473894f7e736a21d920c3aa80f950c0db). Metadatos verificados previamente por la API de HF; pesos no descargados.
