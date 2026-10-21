# Issue #32 — Runner Gemma Android real

- Responsable de implementación: `/root/issue32_implement_luna`, reemplazo secuencial del implementador anterior.
- Estado: runtime, descarga y rasterización Android implementados; falta la integración con la punta revisada de #45, corregir/actualizar los callers de prueba dependientes, repetir la suite completa y completar revisión independiente. **#32 sigue abierta y no se presenta como inferencia ejecutada.**
- Rama: `codex/spec37-issue32`.
- Base del worktree: `4a17aee` (incluye #34).
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
- Se actualizó el texto del componente de descarga al tamaño fijado y a errores de disponibilidad. Se conservó el cambio previo sin commit en `GemmaEligibilityTest.kt` que evita que una elegibilidad forzada eluda el gate de 8 GiB.

## Verificación

Todas las tareas Gradle de esta etapa usaron Gradle 9.3.1, JDK 21.0.12.1, `--max-workers=1` y Kotlin in-process. El slot se liberó después de la compilación; esperar nueva autorización/integración antes de otro build.

| Comprobación | Resultado |
| --- | --- |
| `:shared:jvmTest` completa | **118 ejecutadas, 116 pasan y 2 fallan.** Los fallos son exclusivamente `HybridDocumentOrchestratorTest.testEmptyOrScannedDocumentTriggersTier2WhenModelDownloadedAndHardwareEligible` (línea 58) y `testForceTier2BypassesTier1EvenWithHighConfidenceDigitalText` (línea 218): ambos esperaban éxito simulado del viejo fake. Se conserva este resultado; `HybridDocumentOrchestratorTest` no se modifica hasta que #45 termine revisión e integración. |
| Pruebas dirigidas `GemmaEligibilityTest`, `ModelDownloadManagerTest`, `VlmVisionRunnerTest`, `DeviceCapabilityGateTest` | **PASS, 13 pruebas, cero fallos/errores/omitidas.** |
| `:androidApp:assembleDebug` | **PASS.** Incluyó `:shared:compileAndroidMain`, por lo que compilaron LiteRT-LM, descarga, almacenamiento Android, gate y rasterizador. Se empaquetó `liblitertlm_jni.so`; esto no demuestra carga del modelo ni inferencia. |
| `:composeApp:compileKotlinWasmJs` | **PASS.** Web compila con el gate de Gemma deshabilitado. |
| `:composeApp:testAndroidHostTest` | **NO EJECUTADO.** La corrida conjunta se detuvo tras los dos fallos de `:shared:jvmTest`; queda pendiente después de integrar #45. |
| `git diff --check` | **PASS** en el árbol actual; repetir al cerrar la integración. |
| Instalación/ejecución móvil, descarga de pesos e inferencia física | **NO EJECUTADAS.** No se inició AVD ni se manipuló el teléfono; no se descargaron 2,6 GB. No se acredita rendimiento, extracción real, memoria en hardware ni launch. |

La primera orden Gradle falló antes de tareas por cómo PowerShell separó el argumento `-P`; se corrigió y la ejecución válida usó el argumento entre comillas. No se cuenta como fallo de código.

Después de estas pasadas se endureció el gate Android por defecto para consultar la presión real mediante el contexto de aplicación, se limpian los chunks parciales del almacenamiento en memoria de pruebas y se eliminó la salida literal configurable del fake común. La compilación final de esos últimos cambios queda pendiente de la punta revisada de #45, respetando la coordinación del único slot.

Warnings observados sin fallo: Android SDK reportó XML schema 4 a herramientas que conocen hasta 3; opt-ins Wasm existentes; deprecaciones de `MenuAnchorType`; librerías nativas LiteRT no strippeadas y empaquetadas tal cual.

## Integración y límites pendientes

- #45 terminó su candidato y está en segunda revisión independiente. No se tocaron en esta entrega sus contratos, `HybridDocumentOrchestrator`, parser, grammar ni review sheet. El coordinador enviará la punta después de integrar #45; entonces conectar el runner/descarga/raster al flujo manual y actualizar solo los callers/tests necesarios. Tests de orquestación deben usar un stub explícito con literal controlado y nombrarse como contrato de orquestación, nunca como inferencia.
- Luego repetir suite compartida completa (resolver los dos fallos viejos), pruebas pertinentes de host, build Android/Wasm y diff-check. El slot pesado está sujeto a coordinación de `/root`.
- Revisar la ruta manual cuando modelo/plataforma/presión no permitan Gemma y confirmar que el raster permanece en memoria durante la integración con UI.
- Corrida física con el archivo real y fixture autorizado sigue pendiente; requiere coordinación separada de pesos/dispositivo. Nunca reportar PASS por omisión.
- Sin push, merge, cierre de issue ni cambios a `docs/tracking.md`, SPEC37 o tablero compartido.

## Fuentes primarias

- [LiteRT-LM 0.18.0 `Config.kt`](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/Config.kt) y [API Kotlin / getting started](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/docs/api/kotlin/getting_started.md): opciones de salida JSON, speculative decoding, muestreador y entrada multimodal.
- [Android `PdfRenderer`](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer), [`ProxyFileDescriptorCallback`](https://developer.android.com/reference/android/os/ProxyFileDescriptorCallback) y [`StorageManager.openProxyFileDescriptor`](https://developer.android.com/reference/android/os/storage/StorageManager#openProxyFileDescriptor(int,%20android.os.ProxyFileDescriptorCallback,%20android.os.Handler)): descriptor seekable backed by an in-memory callback.
- Revisión fijada de pesos: [Hugging Face](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/7fa1d78473894f7e736a21d920c3aa80f950c0db). Metadatos verificados previamente por la API de HF; pesos no descargados.
