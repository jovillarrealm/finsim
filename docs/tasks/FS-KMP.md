# FS-KMP — Migración Kotlin Multiplatform (Web y Android)

- Responsable / sesión: /root
- Estado operativo: en revisión
- Rama: feat/kmp-migration
- Commit base de main: ac7b763
- Última actualización: 2026-10-11
- Alcance: Implementación completa de los tickets KMP T01 a T10 (Issues #2 a #11 en GitHub) bajo la especificación aprobada (Issue #1).

## Desglose de tickets implementados

1. **KMP T01 (Issue #2)**: Conversor de tasas con precisión decimal de 40 dígitos significativos, redondeo HALF_UP, potencias fraccionarias sin uso de Double como reemplazo silencioso, error absoluto < 1e-12 y soporte en Web y Android.
2. **KMP T02 (Issue #3)**: Motor base de préstamos con abono único, reducción de plazo, cálculo de exceso no aplicado, gráficos de barras horizontales apiladas alineadas con escala común y desplazamiento vertical, y tabla detallada.
3. **KMP T03 (Issue #4)**: Persistencia y transferencia de escenarios JSON versión 1 entre Web, Android y la aplicación TypeScript. Almacenamiento local aislado (`finsim.scenario`), protección contra sobreescritura con borradores pendientes, y tolerancia a JSON corrupto o archivos > 2MB.
4. **KMP T04 (Issue #5)**: Condiciones completas de préstamo: original vs saldo actual, cuota calculada o conocida, selección de monedas (COP, ARS, USD, EUR) sin conversión de importes, seguros fijos y porcentuales redondeados independientemente, con finalización en liquidación o continuación hasta mes final inclusivo. Detección de préstamos incompletos a 1.200 meses.
5. **KMP T05 (Issue #6)**: Edición de eventos por lotes (abonos recurrentes, omisiones de cuota, recuperaciones), prioridad de aplicación (seguros, intereses, capital), seguimiento de atrasos y deuda sin duplicación.
6. **KMP T07 (Issue #7 - T06)**: Perfil argentino de precancelación: umbral gratuito (25% del plazo o 180 días), comisiones e IVA sobre capital prepagado antes del umbral, cálculo de efectivo requerido y preservación en JSON.
7. **KMP T08 (Issue #8 - T07)**: Comparación de extracto bancario contra préstamo simulado original con diferencias firmadas por campo y moneda.
8. **KMP T09 (Issue #9 - T08)**: Corpus de compatibilidad bidireccional y verificación de invariantes financieras de conservación (capital, intereses, seguros, caja).
9. **KMP T10 (Issue #10 - T09)**: Construcción del APK Android (API 35/37) con funcionamiento offline, almacenamiento en SharedPreferences y herramientas de inspección analógicas a Chrome DevTools (ADB MCP server y Android CLI layout/screen).
10. **KMP T11 (Issue #11 - T10)**: Preparación de la distribución Web Wasm/JS de Kotlin conservando íntegra y recuperable la versión web React anterior.

## Comprobaciones técnicas

- `gradle :shared:jvmTest --rerun-tasks`: 23 pruebas unitarias aprobadas (0 fallos, 0 errores) en Decimal, Rates, Loan, Persistence, Reconciliation y ParityCorpus.
- `gradle :composeApp:assembleDebug`: APK Android generado exitosamente en `kmp/composeApp/build/outputs/apk/debug/composeApp-debug.apk` (25.28 MB).
- `gradle :composeApp:wasmJsBrowserDistribution`: Distribución Web Wasm/JS generada exitosamente en `kmp/composeApp/build/dist/wasmJs/productionExecutable/` con Skiko y Binaryen.
- `npm test`: 45 pruebas unitarias de regresión en TypeScript ejecutadas y aprobadas (10 archivos, 100% pasando).
- Integración de herramientas MCP Android: `@abhinavyadav/adb-mcp-server` registrado en `~/.gemini/config/mcp_config.json`, herramientas `platform-tools` (v37.0.1 con `adb.exe`), y `android` CLI con inspección de layout y pantalla.
- Sistema de Logging KMP: Integración de Touchlab Kermit (`co.touchlab:kermit:2.0.4`) en `:shared` y `:composeApp`, instrumentando motor de préstamos, conversor de tasas, persistencia, conciliación y eventos de UI; enrutamiento automático a Android Logcat y Web console.log.
