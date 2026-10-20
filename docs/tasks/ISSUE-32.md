# Issue #32 — Gemma Android real

- Responsable: subagente implementador `/root/issue32_implement`; reserva asignada por `/root`.
- Estado: preparación parcial, pendiente del prerrequisito #34; **#32 no terminado ni listo para revisión**.
- Rama: `codex/spec37-issue32`.
- Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue32/finsim`.
- Base fija: `0968ac92edf6acd859466b1f16d7fad1b1ebbbe5`.
- Fuente: [#32](https://github.com/jovillarrealm/finsim/issues/32), [#37 y aclaraciones](https://github.com/jovillarrealm/finsim/issues/37).
- Fecha: 2026-10-20.

## Alcance

Runner LiteRT-LM Android desde píxeles, paquete `.litertlm` inmutable verificado por tamaño/SHA-256, descarga real a archivo, RAM total mínima 8 GiB y presión actual independiente, conexión a revisión existente. Web no ejecuta Gemma. Se adaptan únicamente prerrequisitos de almacenamiento, selección y raster en memoria necesarios; no se migra toolchain ni implementa Docling.

Seams aprobados: contratos públicos de runtime/gate/descarga y UI observable. TDD red → green por comportamiento, sin acreditar inferencia a través de dobles.

## Comprobaciones y traspaso

- Implementado únicamente el primer slice: umbral 8 GiB y textos/casos que dependían del umbral anterior. El runtime, descarga real, presión actual y conexión de imágenes siguen pendientes.
- RED conductual: `GemmaEligibilityTest` compilado con Kotlin 2.2.20 y ejecutado con JUnit 4.13.2 contra las clases JVM del baseline publicado/reservado; falla porque 7,999 GiB era elegible. Log: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-gate-red.log`.
- GREEN aislado: mismo test recompilando `DeviceCapabilityGate.kt` y su actual JVM con Kotlin 2.2.20; 1/1 pasa. Log: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-gate-green.log`.
- Regresión aislada: 17/17 pasan (`GemmaEligibilityTest`, `DeviceCapabilityGateTest`, `ModelDownloadManagerTest`, `HybridDocumentOrchestratorTest`), usando gate recompilado y dependencias/clases restantes del baseline de integración. Esto acredita contratos de gate y dobles; **no** inferencia ni compilación completa del cambio. Log: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-gate-regression.log`. No se ejecutó Gradle en paralelo al build pesado del coordinador.
- Compatibilidad **FALLIDA** observada, no omitida: prueba mínima con el compilador Kotlin 2.2.20 e import real de `ConversationConfig(enableResponseFormat = true, enableSpeculativeDecoding = false)` desde AAR oficial `litertlm-android:0.18.0`. Error: metadata 2.4.0, compilador puede leer hasta 2.3.0. Log: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue32-runtime-compatibility.log`. `javap` confirma `mv=[2,4,0]`; también 0.17.1 tiene metadata 2.4 y no ofrece el control explícito de speculative decoding. AAR 0.18 declara minSdk 24 y bytecode Java 21; esas dos condiciones son compatibles con la base.
- El coordinador confirmó resolver el prerrequisito de herramientas en #34 y retomar **este mismo agente** después; no se aplica puente Java, supresión de metadata ni actualización unilateral.
- Consulta independiente sin descargar pesos: API Hugging Face de la revisión `7fa1d78473894f7e736a21d920c3aa80f950c0db` devuelve `gemma-4-E2B-it.litertlm`, 2.583.085.056 bytes, SHA-256 `ab7838cdfc8f77e54d8ca45eadceb20452d9f01e4bfade03e5dce27911b27e42`.
- Fuentes primarias para retomar: [Config LiteRT-LM 0.18](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.18.0/kotlin/java/com/google/ai/edge/litertlm/Config.kt), [allowlist Gallery](https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_11.json), [modelo fijado](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/tree/7fa1d78473894f7e736a21d920c3aa80f950c0db).
- Corrida Samsung/pesos: **NO EJECUTADA**; no se autoriza instalar/reiniciar la aplicación abierta ni descargar 2,6 GB en esta etapa. Requiere dispositivo elegible aislado/autorización de estado y pesos verificados. No certifica inferencia, números, rendimiento ni memoria real.
- Siguiente paso exacto: coordinador integra #34 compatible y envía su punta a este implementador; incorporar esa punta y continuar slice de gate plataforma/presión, runner Android, storage/descarga streaming y UI. Reutilizar únicamente el runner y prerrequisitos pertinentes del commit local `f762b5b`, adaptando al seam actual `VlmVisionRunner`; evitar tensor engine/selector dual Docling obsoletos y ramas completas. Compilar Android/Web y pruebas antes del resumen al revisor secuencial. El rasterizador Android base aún escribe temporales: sustituir por descriptor en memoria, sin importar los defectos de reflexión de contraseñas del commit antiguo.
- Sin push, merge ni cierre de issues; tablero compartido intacto.
