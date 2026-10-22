# Issue #40 — estado y handoff de implementación

- Responsable de implementación: `/root/issue40_resume_luna`; coordinación: `/root`.
- Rama/worktree: `codex/spec37-issue40` en `C:/Users/Lenovo/.codex/worktrees/spec37-issue40/finsim`.
- Estado: las tres correcciones P2 y la limpieza P3 están listas para una segunda revisión independiente. #40 sigue abierto; la aceptación global aún no está acreditada.
- Fuente canónica: [#40](https://github.com/jovillarrealm/finsim/issues/40), [#37](https://github.com/jovillarrealm/finsim/issues/37) y la revisión publicada en [#40](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6333276784). El tablero compartido no se modificó.

## Correcciones para revisión

- Se retiró por completo la ruta `FINSIM_WEB_REUSE_BUILD_REPORT`, sus validadores, helpers, variables y self-checks. `verify:web` siempre construye la distribución Wasm con `--rerun-tasks` cuando pasa el preflight; no se añadió lógica nueva para aprobar reutilización por fechas.
- El seguro fijo usa `Money.parse` tanto al validar el formulario como en `Insurance.validate`, rechazando más de dos decimales sin redondeo silencioso. La precisión decimal porcentual sigue usando la validación existente y no se limitó.
- La regresión significativa comprueba que `1.005` fijo se rechaza, `1.00` fijo se acepta y `0.123456` porcentual conserva su precisión; también comprueba el límite de dominio para un seguro fijo inválido.
- `WEB-40-ORIGIN-A11Y` ahora describe con precisión un recorrido mixto: Tab/Enter/Space para selección y rueda real para mostrar las opciones. El catálogo dice que desplazamiento exclusivamente por teclado queda NOT_RUN.
- `WEB-40-TNUM-UI` ahora declara solo la comparación geométrica de cajas visibles para cifras de igual y distinta longitud. La tabularidad/rasterización de glifos queda NOT_RUN.
- El timeout de EVENT-BATCH es 300 s, ajustado a la duración medida de 235.832 s para evitar cortar el recorrido observado; no representa una promesa de rendimiento.
- Se eliminaron los overloads de tres controles de `AdaptiveFieldRow` tras confirmar que todos sus usos eran de dos controles y no había callers externos.

## Verificación y procedencia

- El test de seguro se ejecutó primero con solo la regresión y falló en la aserción de rechazo de `1.005`; con la implementación, `:shared:jvmTest --tests com.finsim.domain.LoanConditionsDraftTest` pasó 12/12 usando Gradle 9.3.1/JDK 21. No quedó guardado el log de consola de la corrida roja; el XML de la corrida verde está en `C:/Users/Lenovo/.codex/worktrees/spec37-issue40/finsim/kmp/shared/build/test-results/jvmTest/TEST-com.finsim.domain.LoanConditionsDraftTest.xml`.
- Los self-checks Node del verificador (`node --test --test-reporter=tap --test-concurrency=1 scripts/verify-web.test.mjs`) pasaron 4/4 después de eliminar la ruta reuse y añadir comprobaciones de catálogo para ORIGIN-A11Y/TNUM-UI. La sintaxis Node de los archivos cambiados también pasó.
- La coordinación ejecutó antes de estas correcciones, en `549638b`, `:shared:jvmTest` y `:composeApp:testAndroidHostTest` (157/157 en conjunto) y `:shared:testAndroidHostTest` (161/161). Evidencia: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue40-host-549638b/` y `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue40-shared-android-host-549638b/`. Son resultados del checkpoint previo y no se presentan como prueba de los cambios actuales.
- El perfil oficial anterior `2026-10-22T03-59-28-777Z-fe9b5f63` conserva su resultado FAIL: `npm test` 21/23 y `npm run test:e2e` 17/19, ambos con fallos en CONDITIONS y EVENT-BATCH antes del ajuste final del helper.
- El retest dirigido previo al perfil finalizó 2/2 PASS sobre el Wasm entonces servido (`235.832 s` para EVENT-BATCH). Ese Wasm fue producido desde `ab68e2d29d6bd910dbe038a2cbe6f8c5628581fb`, con árbol `781f439ea25a33a6a39b88d3392c7b0362e3176e50cdfba4db15a87d5916fc30` y Wasm `d77056d69c692e7c84fabc5f96e88c5fad5fcadebbf8b1ed859beaddfa4e681a`. Esa identidad no acredita los cambios Kotlin actuales.
- No se ejecutó una compilación Wasm ni el perfil completo tras estas correcciones. La ruta reuse removida se comprobó por búsqueda en `scripts`; los únicos usos restantes de `WEB-BUILD` son los pasos del build real.

## Cobertura pendiente y siguiente acción

- Pendiente: segunda revisión independiente de las tres correcciones P2. Tras su aprobación, ejecutar el `verify:web` completo con una nueva distribución y registrar su identidad/hash, manifiesto HTTP, resultados de ambas órdenes y límites del catálogo.
- Android físico, lector de pantalla, desplazamiento exclusivamente por teclado y rasterización/tabularidad de glifos permanecen NOT_RUN. Los resultados de host Android anteriores no equivalen a pruebas en dispositivo físico.
- No cerrar #40 hasta integrar y verificar el perfil final. No se hicieron cambios al tablero ni a otras fichas.
