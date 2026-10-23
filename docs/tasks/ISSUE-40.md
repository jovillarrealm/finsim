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

## Corrección de coordinación tras la ejecución completa de `04809ea`

La segunda revisión confirmó resueltos los tres P2 y la heurística P3, sin nuevos hallazgos. En `04809ea` limpio, coordinación ejecutó JVM compartido 153/153, host Android compartido 162/162 y host de aplicación 5/5: todos PASS, sin fallos, errores ni omisiones. Los casos comunes se repiten entre destinos.

El perfil `2026-10-22T05-33-07-185Z-8d94f6c2` terminó FAIL después de un build Wasm exitoso: `artifactManifest` no pudo leer los archivos porque se había eliminado la importación `readFile` al retirar reuse. No se sirvió la distribución ni se ejecutaron suites browser en esa corrida (NOT_RUN); el error no se atribuye a finanzas ni a interfaz.

Coordinación restauró la importación compartida por el manifiesto y el cálculo de hashes de evidencia. La nueva regresión lee bytes de un archivo temporal, comprueba un SHA-256 conocido y verifica que el manifiesto cambie con el contenido aunque conserve el timestamp. Self-checks actuales 5/5 PASS; la corrección del verificador requiere seguimiento del mismo revisor antes de repetir el perfil. El producto Kotlin no cambió respecto de `04809ea`; las identidades del productor y del verificador se registrarán en la siguiente ejecución.

## Estabilización dirigida de recorridos UI y relevo

La sesión `/root/issue40_stability_repair` continuó sobre la misma rama, sin cambios al tablero compartido. El productor Wasm autorizado es `5faf6a286f69163c725fc69556fbba8d15523965`; añadió nombres accesibles españoles a la tabla real, cuerpo, encabezados y filas. El checkpoint de helpers de navegador quedó en `72404371c4c8` (`test: stabilize issue 40 table scrolling`), después de `6055992`, `d51be6a`, `55e1fff` y `60a3422`.

La revelación horizontal usa bounds estables de encabezados que intersectan el scrollport real; permite un encabezado parcialmente recortado como ancla cuando el borde medido tiene margen suficiente, empieza con escala de rueda 1 y verifica el avance real (máximo 80 px) en cada gesto. La preparación de un mes valida solo la fila y el cuerpo en vertical; cada lectura revela su propia columna, y MONTH-SYNC revela `Mes` justo antes de tocar M3. La fila completa puede tocar el borde vertical del cuerpo; los botones accionados mantienen contención estricta. El helper acotado de bounds conserva sus casos para rectángulos estables, movimiento y geometría ausente/recortada.

La calibración sintética de EVENT confirmó 44 movimientos medidos de hasta 60 px y las cifras M1=0, M2=210, M3=115; cuenta solo como calibración geométrica, no como aceptación de integración. Después, sobre el preview `http://127.0.0.1:4186/`, cuya distribución se cotejó contra el manifiesto del productor (8/8 archivos por bytes y SHA-256), pasaron secuencialmente: `WEB-40-CONDITIONS` 1/1, `WEB-40-MONTH-SYNC` 1/1, `WEB-40-EVENT-BATCH` 1/1, transferencia JSON actual 1/1, transferencia JSON legacy 1/1 y portapapeles real 1/1. Las pruebas del helper de bounds pasaron 3/3. El caso de portapapeles escribió un marcador distinto antes de cada copia, esperó observacionalmente un cambio y conservó el esperado `507,51` sin repetir clics.

La evidencia de esta ronda está en `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue40-stability-5faf6a2/directed-7240437-20261022T/`. Los seis logs de suite tienen hashes en `coordinator-directed-log-hashes.json`; `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue40-stability-5faf6a2/evidence-index.json` indexa con SHA-256 los logs, trazas, capturas, productor y artefactos previos, incluido el reporte histórico. El resumen estructurado es `directed-7240437-20261022T/result-summary.json`. La calibración exitosa y los intentos geométricos fallidos anteriores permanecen en `directed-6055992-20261022T/`; no se renombraron ni se descartaron.

El perfil completo histórico `2026-10-22T05-43-43-423Z-29983f3f` sigue siendo FAIL: `npm test` 22/24 (2 FAIL) y `test:e2e` 16/19 (3 FAIL), según su reporte original `C:/Users/Lenovo/.finsim/verification/web/2026-10-22T05-43-43-423Z-29983f3f/report.json`. Los dirigidos posteriores no sustituyen ese resultado. El árbol Kotlin de la app y shared permaneció idéntico al productor; no se ejecutó otro build ni un perfil completo en esta sesión.

## Estado de relevo tras estabilización

- Las correcciones de interfaz anteriores ya revisadas siguen resueltas; los cambios de geometría/test de esta actualización esperan una nueva revisión independiente.
- No ejecutar el perfil completo antes de esa revisión. Tras revisión, coordinación podrá correr un nuevo perfil con nueva distribución y preservar todos los estados FAIL/NOT_RUN en el reporte.
- Siguen pendientes la aceptación del perfil completo, la revisión nueva y la prueba en dispositivo físico. Android host 5/5 de `5faf6a2` es evidencia previa de coordinación y no demuestra prueba en dispositivo ni se atribuye a este cambio de helpers.
- El comentario de coordinación con el relevo de esta ronda es [#40, comentario 6357441126](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6357441126). #40 permanece abierto.

## Seguimiento de revisión: diagnóstico compartido y límites de suites

La revisión independiente final de `4393be9` informó cero incumplimientos y dos observaciones P3: captura/traza duplicada en las suites browser y unidades `stable-box` incluidas en el glob UI. Se corrigieron en esta misma rama, sin alterar lógica de aplicación ni oráculos:

- `web-diagnostics.cjs` centraliza la captura real en las cinco suites que ya la usaban. Conserva `FINSIM_WEB_DIAGNOSTICS` como destino externo, nombres únicos, `trace.log` y captura en las rutas de fallo. La captura es best-effort: si falla al capturar, registra el error cuando puede y no reemplaza el fallo de UI original. `issue-40-financial-ui.test.cjs` mantiene `fullPage: true`; las otras suites conservan la captura del viewport.
- Las tres unidades de `waitForStableBox` se movieron fuera del glob browser a `scripts/browser-geometry.test.cjs`. El glob `test:e2e` vuelve a incluir únicamente los 19 recorridos UI; `npm test` agrega los cinco checks del verificador y las tres unidades de geometría. No se catalogaron las unidades como casos UI.
- La descripción Markdown del reporte de `verify-web` ahora explica por separado esos cinco checks, las tres pruebas auxiliares y los recorridos UI de `test:e2e`.

Checks de esta refactorización auxiliar: sintaxis Node de todos los CJS bajo `browser-tests`, `scripts/browser-geometry.test.cjs` y `scripts/verify-web.mjs`; separación estática de comandos y conteo esperado 19; pruebas geométricas 3/3 PASS; `scripts/verify-web.test.mjs` 5/5 PASS; `git diff --check` PASS. No se ejecutaron browser, build, Gradle ni perfil completo. Los seis recorridos dirigidos PASS del checkpoint `7240437` son evidencia anterior a este cambio auxiliar y no se reclasifican como una nueva ejecución. El FAIL histórico y la aceptación pendiente descritos arriba permanecen.

Logs finales de estos checks: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue40-review-followup-4393be9/final-checks-post-fix/`. El worktree queda en la rama existente y espera el seguimiento secuencial del revisor.
