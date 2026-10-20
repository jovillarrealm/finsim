# ISSUE-34 — Toolchain y módulos Android AGP 9

- Responsable / sesión: /root/issue34_implement, asignada por /root.
- Estado operativo: en revisión (entrega técnica); launch Android bloqueado y actualización firmada con datos no ejecutada.
- Rama: `codex/spec37-issue34`.
- Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue34/finsim`.
- Base publicada: `839c69c6e8f7c114424acb6f3fa9b7aae91063a2`; integración/reserva inicial: `523f91d1feb330f5f8f8b947271a35ac1ce7bbc9`.
- Alcance canónico: [#34](https://github.com/jovillarrealm/finsim/issues/34), [ampliación Kotlin compatible](https://github.com/jovillarrealm/finsim/issues/34#issuecomment-6301601178) y [política móvil #37](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6293230094).
- Última actualización: 2026-10-20.

## Base, implementación y límites

Base real: AGP 8.7.3, Kotlin 2.2.20, Compose 1.9.3, Gradle 8.11.1, Java 21 y compileSdk 36. El reporte de AGP 8.13.2 no corresponde a esta base publicada.

Reutilización selectiva de `128c59f`: solo código/configuración/instrucciones, sin importar coordinación de otra sesión. Commit de implementación: `a001654`. La integración `e96592d` (#36, tests) se incorpora en `97242848`; `fe291ae` (solo docs/reservas) en `16bca760`. El tablero y SPEC37 los modifica únicamente la coordinadora.

Toolchain fijada: AGP 9.1.1, Kotlin 2.4.21 y Gradle 9.3.1; compileSdk 37 (paquete SDK 37.0). Compose 1.9.3 se conserva: Android y Wasm compilan sin ampliar #35. Android separa `:androidApp` (aplicación) de `:composeApp` y `:shared` (plugin Android KMP de biblioteca). Shared conserva además JVM/JS/Wasm; Compose conserva Wasm. Se retira el parche R8 de AGP 8 y sus supresiones antiguas.

Superficies editadas: catálogo/build Gradle raíz/shared/composeApp/androidApp, entrada/manifiesto/iconos/ProGuard Android movidos sin modificación, CI Pages, tarea npm test:kmp e instrucciones README. Sin cambios al motor, UI o serialización. Identidad `io.github.jovillarrealm.finsim_alpha`, launcher `com.finsim.app.MainActivity`, versión 1.0.3/código 4, app minSdk 26/targetSdk 36 y shared minSdk 24 preservados. Firma por el mismo `kmp/keystore.properties` ignorado y keystore existente; no se publica ni copia el keystore. Preferences `finsim_prefs` y clave `finsim.scenario` preservadas.

## Traspaso actual

- Seams autorizados: CLI pública de build y tests financieros públicos existentes. Configuración mecánica sin tests espejo. Skills TDD y Ponytail leídos; no se delegó trabajo.
- Resultados nuevos: APK debug correcto (78 tareas, 6m13s), shared JVM 115/115, host actual 5/5 tras #36, APK release firmado y Wasm de producción correctos (140 tareas, 10m08s). El build final reutiliza el resultado shared válido de esta misma toolchain; vuelve a compilar/ejecutar host tras #36.
- Probe LiteRT-LM 0.18.0: compila el constructor real de ConversationConfig con Kotlin 2.4.21/JVM 21 sin eludir metadata. No acredita inferencia ni incorpora runtime en #34.
- `npm ci` y `npm run typecheck`: correctos; dos avisos npm preexistentes, sin actualizar dependencias fuera de alcance.
- Identidad/min/target/compile/version y certificado del APK release inspeccionados. Firma v2 válida, certificado igual al del AAB local previo; esa comparación no acredita código del AAB ni actualización con datos.
- Artefacto release preservado para #44: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue34-artifacts/`, con APK, identity.json (fuente/hash/certificado/toolchain) y log de firma. Fuente del build: `97242848aca3228064708177288c12b240ecea45`.
- Resumen previo a revisión y logs: [ISSUE-34-validation](../reviews/ISSUE-34-validation.md). Los logs completos están en `.scratch/issue34` de este worktree.
- Actualización firmada desde APK anterior con escenario guardado: NO EJECUTADA, con procedimiento de reanudación en el informe. Teléfono del usuario intacto.
- Chrome: 13/13 pruebas existentes pasan (249,55 s) sobre la distribución nueva identificada en puerto 4178; hash JS servido coincide. Servidor y Chrome cerrados al terminar.
- Launch Android: BLOQUEADO antes de instalar; dos arranques del AVD aislado API36 no completaron inicialización/ADB autorizado. Captura y logs en el informe; no es un fallo observado del APK. AVD detenido, teléfono y servidor ADB intactos.
- Cambios sin commit al entregar: ninguno. Ficha e informe forman el commit documental posterior al código.
- Siguiente paso: revisión independiente Standards/Spec por el segundo agente de #34, desde integración fe291ae; integrar por la coordinadora cuando corresponda, conservando criterios móviles pendientes. No push, publicación, cierre de issue ni integración de la entrega por este implementador.

## Historial breve

- 2026-10-20 · Reserva `bf37edd` guardada antes de editar configuración.
- 2026-10-20 · Implementación `a001654`; incorporación de integraciones coordinadas `97242848` y `16bca760`; builds y pruebas de toolchain correctos.

- 2026-10-20 · Web 13/13 sobre artefacto nuevo; intento AVD aislado bloqueado antes de instalar. Evidencia, artefacto firmado y límites registrados para revisión.
