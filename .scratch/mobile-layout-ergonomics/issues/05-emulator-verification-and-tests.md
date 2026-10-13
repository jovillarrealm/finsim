# 05: Verificación Visual Integral en Emulador Android y Suite de Pruebas

**What to build:**
1. Verificación completa de compilación:
   - `./gradlew :shared:jvmTest` (100% de pruebas unitarias pasando).
   - `./gradlew :composeApp:compileDebugKotlinAndroid` (compilación limpia sin advertencias).
   - `./gradlew :composeApp:assembleDebug` (generación de APK de depuración para prueba local).
2. Verificación en Emulador Android:
   - Iniciar el emulador `medium_phone` con `android emulator start medium_phone`.
   - Instalar el APK en el emulador con `android install` o `adb install`.
   - Capturar pantallas automáticas con `android screenshot` para confirmar la resolución de los problemas de layout (pestañas limpias, formulario responsive a 1 columna, acordeón colapsable, tarjetas KPI legibles).
   - Verificar la jerarquía visual con `android layout`.
3. Actualizar `docs/tracking.md` y marcar tickets cerrados.

**Blocked by:** 04-fluid-kpis-and-chart-markers.md

**Triage Label:** `ready-for-agent`
**Status:** completed

- [x] Verificación de pruebas unitarias y compilación de APK.
- [x] Ejecución y captura en emulador `medium_phone` / preview móvil Skiko.
- [x] Actualización de documentación y registro en `docs/tracking.md`.
