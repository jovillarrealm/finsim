# ISSUE-34 — Toolchain y módulos Android AGP 9

- Responsable / sesión: /root/issue34_implement, asignada por /root.
- Estado operativo: en curso; reserva autorizada por la coordinadora.
- Rama: `codex/spec37-issue34`.
- Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue34/finsim`.
- Base publicada: `839c69c6e8f7c114424acb6f3fa9b7aae91063a2`; integración y reserva: `523f91d1feb330f5f8f8b947271a35ac1ce7bbc9`.
- Alcance canónico: [#34](https://github.com/jovillarrealm/finsim/issues/34), [ampliación Kotlin compatible](https://github.com/jovillarrealm/finsim/issues/34#issuecomment-6301601178) y [política móvil #37](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6293230094).
- Última actualización: 2026-10-20.

## Inicio y límites

Base real: AGP 8.7.3, Kotlin 2.2.20, Compose 1.9.3, Gradle 8.11.1, Java 21, compileSdk 36; app minSdk 26/targetSdk 36, shared minSdk 24. El reporte de AGP 8.13.2 no corresponde a esta base publicada.

Reutilizar selectivamente el cambio local `128c59f` (solo código/configuración/instrucciones). No incorporar coordinación ni ramas ajenas. Mantener Compose 1.9.3 si la compilación demuestra compatibilidad; cualquier ampliación a #35 requiere evidencia previa.

Superficies necesarias: catálogos y build Gradle raíz/shared/composeApp/androidApp, entrada/manifest/iconos y ProGuard de Android, CI Pages, scripts npm e instrucciones README. Se preservan código financiero, aplicación `io.github.jovillarrealm.finsim_alpha`, namespace de app `com.finsim.app`, versión 1.0.3/código 4, min/target, firma `kmp/keystore.properties` y SharedPreferences `finsim_prefs` con las claves existentes. Sin migración de datos.

Seams autorizados para verificación: CLI pública de build y tests financieros públicos existentes. Configuración mecánica no necesita tests espejo. Ejecutar comprobaciones nuevas de APK, Wasm, shared JVM y Compose host sobre esta rama; los resultados anteriores no son evidencia nueva.

## Traspaso actual

- Implementado: pendiente.
- Comprobaciones: pendientes.
- Launch y actualización firmada con escenario guardado: NO EJECUTADAS; usar emulador aislado si está disponible. No instalar ni reiniciar la app del teléfono del usuario sin autorización para descartar documentos/edición.
- Siguiente paso: aplicar únicamente los cambios requeridos de `128c59f`, fijar Kotlin compatible con metadata 2.4 de LiteRT-LM 0.18.0, compilar y registrar resultados reales antes de revisión independiente.
