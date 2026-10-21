# Issue #32 — Runner Gemma Android real

- Responsable de implementación: `/root/issue32_implement_luna`, reemplazo secuencial del implementador anterior.
- Estado: la revisión independiente inicial encontró dos defectos de cancelación, corregidos en el seguimiento de este candidato. Las pruebas JVM y Android host, el APK debug y Wasm pasan sobre el árbol de código actual. Pendientes segunda revisión independiente y fixture sintético en Samsung. **#32 sigue abierta; no se descargó el modelo ni se ejecutó inferencia.**
- Rama: `codex/spec37-issue32`.
- Base del trabajo: checkpoint `3cf6f577` más merge de #45 `c1199b4` (sobre #34).
- Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue32/finsim`.
- Alcance canónico: [#32](https://github.com/jovillarrealm/finsim/issues/32), aclaraciones de [#37](https://github.com/jovillarrealm/finsim/issues/37) y handoff en `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/AGENT-CONTEXT.md` / `COORDINATION.md`.

## Historial de preparación

Antes de integrar #34, el slice inicial del gate se probó en RED/GREEN aislado: el baseline permitía 7,999 GiB y la implementación aislada lo bloqueó. Los logs siguen en `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-gate-red.log` y `issue32-gate-green.log`; su regresión parcial fue 17/17 en `issue32-gate-regression.log`. Eso acredita aquel slice aislado, no el runtime ni la entrega actual.

La prueba mínima anterior con Kotlin 2.2.20 no podía leer metadata Kotlin 2.4 del AAR LiteRT-LM 0.18.0 (`issue32-runtime-compatibility.log`). #34 integró Kotlin 2.4.21 / AGP 9.1.1 / Gradle 9.3.1 sin bypass; el runtime actual compila contra el AAR real. El tamaño/SHA fijados del modelo se contrastaron previamente con la API de Hugging Face; no se descargaron pesos.

## Código incluido en esta rama

- Artefacto fijado a `litert-community/gemma-4-E2B-it-litert-lm`, revisión `7fa1d78473894f7e736a21d920c3aa80f950c0db`, archivo `gemma-4-E2B-it.litertlm`, 2.583.085.056 bytes, SHA-256 `ab7838cdfc8f77e54d8ca45eadceb20452d9f01e4bfade03e5dce27911b27e42`.
- `AndroidModelDownloader` transmite el archivo desde la URL de revisión inmutable. `AndroidModelStorage` escribe en `filesDir/models` a un `.part` único por intento, calcula SHA-256 de los bytes que persiste y publica el archivo/marker atómicamente solo si coinciden extensión, tamaño y hash fijados. La ruta es privada y persistente; limpiar un intento cancelado no elimina el parcial de un reintento.
- `AndroidGemmaVisionRunner` adapta `VlmVisionRunner` a LiteRT-LM 0.18.0: entrega la página PNG como `Content.ImageBytes`, usa JSON restringido y muestreo determinista, desactiva speculative decoding, comprueba el artefacto antes de cargar y reporta errores sin inventar datos. La presión actual usa `ActivityManager.MemoryInfo.lowMemory`; se registra memoria disponible y PSS como diagnósticos. Backend CPU.
- El gate exige al menos 8 GiB de RAM total y bloquea presión actual por separado. En Android, `getDeviceCapabilityGate()` consulta `ActivityManager`; sin contexto falla cerrado. Web/JS/Wasm declaran Gemma no disponible. La condición de RAM no se presenta como garantía de espacio libre suficiente.
- `AndroidPdfRasterizer` alimenta `PdfRenderer` con un descriptor seekable sobre los bytes en memoria; no escribe PDF ni raster a temporales. La actividad registra el contexto de aplicación requerido.
- `FakeVlmVisionRunner` ya no decodifica bytes de imagen como texto ni fabrica una respuesta; siempre falla en análisis y nunca reporta modelo listo. Los tests de orquestación que deban cubrir éxito usarán un stub local con literal controlado y etiqueta de contrato, no un test de inferencia.
- Se actualizó el texto del componente de descarga al tamaño fijado y a errores de disponibilidad. Se conserva la prueba en `GemmaEligibilityTest.kt` que evita que una elegibilidad forzada eluda el gate de 8 GiB.
- `DocumentImportDialog` consume un runtime por plataforma, muestra descarga solo en Android y mantiene separado el texto pegado de los bytes PDF. Texto pegado va al parser Tier 1; el PDF va al rasterizador y al runner real solo tras gate y modelo validado. El flujo web muestra indisponibilidad y conserva la entrada manual.
- Android SAF lee el PDF en `Dispatchers.IO` sin permiso persistente ni copia a disco. El límite es 25 MiB por archivo y 12 millones de píxeles por página rasterizada a 150 DPI; los excesos y OOM reportan errores claros. El diálogo mantiene el PDF solo en memoria y muestra que procesa la página 1; la hoja de revisión indica la página analizada.
- El orquestador ya no decodifica `pdfBytes` para extracción ni para escoger página. Si no hay texto manual, la página elegida es la primera. Al cerrar el diálogo, la cancelación llega a `Conversation.cancelProcess()` y se espera a que termine la llamada nativa antes de cerrar el runner.
- Los dos casos que antes esperaban éxito del fake ahora usan un stub de test con literal controlado, con nombres/comentarios que aclaran que acreditan el contrato de orquestación y no inferencia. Se añadió cobertura para PDF con texto seleccionable que debe pasar por raster, cancelación, reintento sin carrera y exceso del límite de tamaño.

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

La corrida histórica anterior al wiring registró 118 pruebas con 116 pases y 2 fallos: ambos esperaban éxito del fake común. En el árbol `4d6117c…` esos casos quedaron como stubs de contrato explícitos y la suite pasó 143/143; la validación de seguimiento de este candidato pasó 144/144. El runner de producción no interpreta bytes de imagen como texto y los stubs no acreditan inferencia.

Warnings observados sin fallo: Android SDK reportó XML schema 4 a herramientas que conocen hasta 3; opt-ins Wasm existentes; deprecaciones de `MenuAnchorType`; y librerías nativas LiteRT empaquetadas tal cual.

## Seguimiento de revisión independiente

La revisión de `3f4f995cc3e403c110ce8ee7ef9edc4244a9a4c8` reportó P1 porque `Conversation.sendMessage` es síncrono y no recibía la cancelación del diálogo, y P2 porque la limpieza tardía de una descarga podía borrar el `.part` compartido con un reintento. El runner ahora conecta la cancelación de coroutine con `Conversation.cancelProcess()` y mantiene la conversación viva hasta que la llamada síncrona termina; el diálogo espera la cancelación antes del cierre. El almacenamiento Android crea una ruta parcial distinta por intento y cada tarea conserva su instancia aislada. El downloader comprueba cancelación de nuevo justo después de `read`, antes de guardar bytes o emitir progreso, y el manager acepta reintento cuando el trabajo previo ya está cancelado aunque aún ejecute su limpieza.

La primera reproducción automatizada no fue un RED válido: el test no emitía progreso al empezar el primer intento y falló antes de comprobar la cancelación. Se corrigió el escenario para entrar en estado `Downloading`, cancelar y reintentar inmediatamente en el mismo manager, sin borrar el modelo ni ceder el scheduler. Con el guard antiguo basado en `isCompleted`, la regresión dirigida falló como se esperaba (`expected 2` llamadas al downloader, `was 1`); al restaurar el guard por `isActive`, pasó en la validación final. La corrida final reemplazó el XML de Gradle con el resultado verde, así que el RED queda documentado por la salida de su ejecución, no por el archivo de resultados actual.

La validación final se ejecutó una vez, secuencialmente, con Gradle 9.3.1, JDK 21.0.12.1, un worker y Kotlin in-process, sobre HEAD `3f4f995cc3e403c110ce8ee7ef9edc4244a9a4c8` más el diff de código `1072d4fb80d3145e58c45ff4c1e8ef3b71a5e0ca` y los dos tests Android host no rastreados entonces (SHA-256 `533DED21994A3EA407011284C974D89B0BA5E103E85C9EF3C2517B05F7A59CD2` y `0F86BBC05B524F64ADB6F0333188A7ABBCC336412B2D44E94DD25FB00AE45C19`). Resultado: `:shared:jvmTest` **144/144 PASS**; `:shared:testAndroidHostTest` **146/146 PASS**, incluidas cancelación nativa controlada y limpieza aislada por intento; `:androidApp:assembleDebug` **PASS**, APK de 62.795.772 bytes con SHA-256 `9F088D8FFA55EC3D10F07E6C66E54172F89341E16222B7BBD17169F3C8F1E25E`; `:shared:compileKotlinWasmJs` **PASS**. El APK no se instaló. Los fallos anteriores de configuración de Gradle y preparación de test se conservan como incidencias de preparación distintas, no como fallos del candidato final.

Esta corrida valida compilación, contratos y cancelación coordinada; no acredita inferencia LiteRT-LM ni descarga real. La segunda revisión y la evidencia física Samsung siguen pendientes.

## Prueba física pendiente en Samsung

La ruta que consume la prueba es `DocumentImportDialog` → SAF `PdfDocumentPickerButton` → `DocumentIngestionRuntime.android` → `HybridDocumentOrchestrator(forceTier2 = true)` → `AndroidPdfRasterizer` (página 1, 150 DPI, solo memoria) → `AndroidGemmaVisionRunner`. En el diálogo se selecciona Colombia, se elige el PDF sintético de una página, se descarga el modelo desde la tarjeta, se espera a que aparezca como instalado y se pulsa **Analizar página 1 con Gemma**. El texto pegado debe quedar vacío para que la entrada sea inequívocamente visual. El diálogo muestra los valores para revisión; el usuario confirma o corrige antes de importar.

El texto sintético colombiano de `DocumentImportDialog` puede servir de contenido para el PDF de prueba: capital original COP 30.000.000, plazo pactado 48 meses, tasa E.A. 19,50 %, cuota mensual COP 850.000, abono a capital COP 350.000 e interés corriente COP 477.500. Para preparar el fixture, copia ese bloque de demostración en un editor local y expórtalo como PDF de una página. El texto de muestra de la UI no es por sí solo un PDF adjunto.

La corrida real debe registrar dispositivo/Android/API, `totalRamGb`, estado `lowMemory` del gate, SHA/tamaño del modelo validado, memoria disponible antes de carga, pico PSS, latencia de inicialización e inferencia, campos devueltos y resultado de revisión, además de errores/cierre de la app. `AndroidGemmaVisionRunner` emite la línea `Gemma real image inference complete` con `GemmaInferenceDiagnostics` en Logcat. Aún faltan el PDF sintético accesible al teléfono, la descarga real de 2.583.085.056 bytes, esos valores medidos y la inferencia real. No instalar el APK debug generado sobre la instalación release; tras integrar #40/#41, preparar la actualización firmada con la misma clave antes de coordinar la instalación.

## Integración y límites pendientes

- #45 está integrado en `c1199b4`; se ajustaron solo los tests de contrato necesarios en esta entrega. La ruta manual sigue disponible en plataformas sin Gemma o cuando el gate/modelo no permite visión.
- Segunda revisión independiente Luna/max pendiente. El APK no se instaló ni ejecutó; la medición real del gate y del comportamiento con el modelo requiere coordinación física con #43/#47 y la actualización firmada de #44.
- Después de la segunda revisión, `/root` coordina integración y estado de GitHub. Esta entrega no hace push ni cierra el issue.
- Corrida física con el archivo real y fixture autorizado sigue pendiente; requiere coordinación separada de pesos/dispositivo. Nunca reportar PASS por omisión.
- Sin push, merge, cierre de issue ni cambios a `docs/tracking.md`, SPEC37 o tablero compartido.

## Fuentes primarias

- [LiteRT-LM 0.18.0 `Config.kt`](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/Config.kt) y [API Kotlin / getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/docs/api/kotlin/getting_started.md): opciones de salida JSON, speculative decoding, muestreador y entrada multimodal.
- [Android `PdfRenderer`](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer), [`ProxyFileDescriptorCallback`](https://developer.android.com/reference/android/os/ProxyFileDescriptorCallback) y [`StorageManager.openProxyFileDescriptor`](https://developer.android.com/reference/android/os/storage/StorageManager#openProxyFileDescriptor(int,%20android.os.ProxyFileDescriptorCallback,%20android.os.Handler)): descriptor seekable backed by an in-memory callback.
- Revisión fijada de pesos: [Hugging Face](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/7fa1d78473894f7e736a21d920c3aa80f950c0db). Metadatos verificados previamente por la API de HF; pesos no descargados.
