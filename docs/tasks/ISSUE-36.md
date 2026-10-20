# #36 — Retirar pruebas tautológicas de hitos y ahorro

- Responsable / sesión: /root/issue36_implement; coordinación /root.
- Estado operativo: implementación terminada en rama; pendiente de revisión independiente e integración por la coordinadora. Issue abierta.
- Rama / worktree: `codex/spec37-issue36`, `C:/Users/Lenovo/.codex/worktrees/spec37-issue36/finsim`.
- Base: `0968ac92edf6acd859466b1f16d7fad1b1ebbbe5`.
- Alcance canónico: [#36](https://github.com/jovillarrealm/finsim/issues/36), prerrequisito de [#48](https://github.com/jovillarrealm/finsim/issues/48) / [#37](https://github.com/jovillarrealm/finsim/issues/37).
- Última actualización: 2026-10-20, America/Bogota.

## Traspaso actual

- Reutilización autorizada de los cambios pertinentes de `fbf822be8388eee986326b3d020e6ca7e596e900` y `d24974585bf0106a88369186d55628ae66b96307`: retirar tautologías, preservar CSV e invariantes del motor y corregir las referencias que afirmaban paridad visual.
- Superficies: suite Kotlin existente, nombres/ubicaciones de sus pruebas y sección de cobertura Kotlin en `docs/specs/STITCH-SCREENS-SPEC.md`. Sin cambios de código de producción, finanzas, contratos, exportaciones, suites browser ni configuración.
- Implementado: retiradas las dos pruebas que reconstruían hitos y la prueba de ahorro sin eventos; eliminada la derivación local del porcentaje de ahorro. Conservadas la integridad CSV y las comprobaciones financieras de caja, evolución del saldo y liquidación anticipada, con nombres que ya no afirman paridad visual.
- Commit de código: `e9c176eaee85bb82ea19d1deeeb5458d08e2ee88` (`Ref #36`). Los dos tests resultantes y la sección documental coinciden con la solución local reutilizada; no se incorporó su historia de coordinación, toolchain ni ktlint.
- Cambios sin commit al entregar: ninguno.
- Siguiente paso exacto: incorporar la actualización exclusivamente documental `523f91d1feb330f5f8f8b947271a35ac1ce7bbc9` de la integración y pasar esta entrega y el resumen siguiente al segundo subagente, para revisión de Standards/Spec; la coordinadora decide integración y cierre después de verificar.

## Resumen previo a revisión y evidencia de ejecución

Ejecución `issue36-20261020T1012-0500`, Windows, America/Bogota. Código revisable `e9c176eaee85bb82ea19d1deeeb5458d08e2ee88`; durante las pruebas solo esta ficha documental estaba sin commit. Producción, motor, scripts de pruebas, dependencias y configuración son idénticos a la base publicada `839c69c6e8f7c114424acb6f3fa9b7aae91063a2`; el checkout de construcción `0968ac92edf6acd859466b1f16d7fad1b1ebbbe5` añade únicamente documentación. No se reconstruye Wasm por eliminar tests o actualizar documentos.

| Comprobación | Resultado real | Evidencia |
| --- | --- | --- |
| `npm ci` | APROBADA; 26 paquetes instalados, lockfile intacto | Salida de esta sesión; npm reportó una vulnerabilidad alta existente, sin modificar dependencias |
| `gradle -p kmp :shared:jvmTest :composeApp:testDebugUnitTest --no-daemon --console=plain` | APROBADA; 115 pruebas shared JVM y 5 de Compose en host Android, cero fallos/errores; 1 min 36 s | [Log Kotlin](C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue36-kmp.log); XML bajo `kmp/shared/build/test-results/jvmTest/` y `kmp/composeApp/build/test-results/testDebugUnitTest/` |
| `FINSIM_WEB_URL=http://127.0.0.1:4176/ npm test` (variable establecida en PowerShell) | APROBADA; 13/13, cero fallos/canceladas/omitidas; 254.736 s | [Log navegador](C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue36-browser.log) |
| `git diff --check` y comparación de producción/suites/configuración contra la base | APROBADA; sin errores de espacios ni cambios de producción o de las suites browser | Diff del commit de código |

Entorno Kotlin: Gradle 8.11.1, JDK 21 de `C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`; `JAVA_HOME` corregido únicamente para el proceso, sin editar el toolchain. Navegador: Chrome headless mediante Playwright existente, Node 26.8.1 / npm 11.19.0; seis archivos de `browser-tests`, concurrencia uno. La suite conserva teclado, nombres accesibles, escritorio/360 px, comparación, transferencia JSON, rechazo no destructivo, portapapeles, filtros, importes, navegación táctil M1200 y aplicación de borradores.

Artefacto: distribución Wasm de producción construida por la coordinadora, build aprobado en 7 min 50 s. [Manifiesto de ocho archivos](C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/baseline-wasm-manifest.json) y [log de construcción](C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/baseline-wasm.log). Se descargó `composeApp.js` desde el servidor local utilizado por la suite y se verificó su SHA256 `da4675bee0b39f6d118f3d68ff42b20b1e16b1c399bf8acddcde2cee2c19170a`, coincidente con el manifiesto. Wasm principal `341141461dea1ed1844c.wasm`: SHA256 `54e87647a18f2acb5a48190bebf8deabeff1c702d1ec625aeb758209111ff246`.

## Omisiones y límites

- No se añadieron tests para reemplazar tautologías: #36 permite retirarlas. Las interfaces de prueba válidas se conservan; los tests Kotlin resultantes no acreditan hitos visibles ni estado visual del resumen de ahorro. Esa cobertura queda explícitamente sin evidencia en estos tests, para el trabajo posterior de #37/#48.
- No se ejecutaron APK instalado, teléfono físico, emulador, lector real ni aceptación de audiencia en esta entrega de pruebas. La suite Kotlin Android se ejecuta en el host; la emulación táctil acredita web, sin sustituir Android. Para retomar recorridos Android, usar el perfil de la plataforma cuando se prepare en #37, con APK/dispositivo identificados y el alcance de reinicio acordado.
- La base de #36 dispone de `npm test` y no de `npm run test:e2e`; no se inventa ese segundo perfil ni se atribuye una corrida inexistente. Su consolidación corresponde al catálogo de #37. Algunos casos de accesibilidad existentes usan clics forzados; su resultado se limita a sus aserciones y no certifica accesibilidad global.
- CSV conserva exactamente la prueba válida de 18 columnas de esta base. La decisión posterior sobre la columna de moneda corresponde a #41/#44; no se modifica el exportador aquí.
- Logs, manifiesto y XML son evidencia local, no se publican ni incluyen documentos privados. No se accedió a `snstv`, no se hizo push, no se cerró ninguna issue ni se modificó el tablero compartido.

## Historial breve

- 2026-10-20 · Reserva por la coordinadora y lectura de AGENTS, plan, tracking, brief, convenciones, glosario, ADR 0002, #36 y decisiones de #37. No se modifica el tablero compartido.
- 2026-10-20 · Código guardado en `e9c176e`; verificaciones Kotlin y navegador aprobadas. Resumen y límites registrados antes de la revisión independiente.
