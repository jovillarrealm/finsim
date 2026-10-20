# Issue #39 — perfil financiero invocable

- Responsable: `/root/issue39_implement`; coordinación: `/root`.
- Estado: implementación entregada en rama; pendiente revisión independiente de Standards y Spec. No integrada ni cerrada.
- Rama/worktree: `codex/spec37-issue39` · `C:/Users/Lenovo/.codex/worktrees/spec37-issue39/finsim`.
- Base de integración confirmada al ejecutar: `fe291ae76403d83e2cd50aec69a3b503c1c14d33` (`codex/spec37-verification`). #34 seguía en revisión y no estaba integrado.
- Commit de implementación: `c0c700e0aa5859dc5b8f69e189094fbaa85a02a1`.

## Entrega

`npm run verify:financial` invoca el perfil PF de motor y calidad en JVM host. El catálogo del script identifica 17 casos de tasas, cronogramas, redondeo, seguros, atrasos/recuperación, muestra ARS, resultado incompleto, validación pública e invariantes; cada entrada especifica método JUnit, expectativa, criterio, referencia y archivo fuente. Se reutilizan las pruebas públicas de `:shared:jvmTest` y se añade una comprobación de capital no positivo por `simulateLoan(Scenario)`.

El reporte JSON/Markdown, logs y JUnit se crean en un directorio único bajo `~/.finsim/verification/financial/<run-id>/`. La identidad incluye rama/commit y huella SHA-256 de cambios locales; se registran entorno, comando, hashes de clases JVM y reportes JUnit. Un método cuenta PASS solo al encontrar su testcase en XML nuevo; se conserva el sufijo de destino `[jvm]`. FAIL, BLOCKED, NOT_RUN y NOT_APPLICABLE permanecen diferenciados; fallo o alcance seleccionado incompleto devuelve código no exitoso. No se añadieron dependencias ni framework de pruebas.

## Comprobaciones

- `npm run test:verification`: 4/4 selfchecks pasan; cubren parseo JUnit PASS/FAIL/skip, correspondencia de casos, sufijo `[jvm]`, ausencia de resultados, estado mixto y resumen no aprobable.
- `node --check scripts/verify-financial.mjs`, `node --check scripts/verification-status.mjs` y `git diff --check`: pasan.
- Corrida diagnóstica `2026-10-20T21-34-46-728Z-1c8ad1d7`: Gradle terminó correctamente pero el reporte quedó **INCOMPLETE** porque la versión inicial del matcher no contemplaba `[jvm]`. Se conserva sin reinterpretarla como aprobación. El XML nuevo de esa corrida se usó únicamente para depurar el reporter.
- Corrida confirmatoria `2026-10-20T21-43-15-810Z-1b421421`: **PASS**, 17/17 casos y ejecución Gradle identificados; salida `0`; Gradle 8.11.1, JVM 21.0.12.1 y Node v26.8.1 sobre Windows; `--no-daemon`, un worker, compilador Kotlin in-process y heap máximo 2 GiB. Evidencia completa en `C:/Users/Lenovo/.finsim/verification/financial/2026-10-20T21-43-15-810Z-1b421421/`.
- El reporte confirma **NOT_RUN** para Wasm, APK Android e interacción UI. La muestra argentina es un caso controlado y no se presenta como regla legal o de prestamista.

## Límites y siguiente paso

PASS acredita exclusivamente los casos seleccionados en JVM host; no acredita FIN-01 en Wasm ni FIN-02/FIN-03/UI-01 en interfaz o APK. La corrida aún usa la base de integración anterior a #34; cuando la coordinadora integre esa punta, decidirá si el cambio de toolchain requiere repetir PF. La ejecución inicial INCOMPLETE permanece como evidencia histórica separada.

La ejecución emitió la advertencia deprecada de Node al lanzar Gradle en Windows y Gradle informó una propiedad de compatibilidad Kotlin deprecada ya presente en la base. Ninguna afectó el resultado confirmatorio.

Próximo paso: revisión independiente Standards/Spec sobre `c0c700e`, usando el resumen publicado en #39. Mantener pendiente la cobertura Wasm/APK/UI; no integrar ni cerrar este issue desde esta rama.
