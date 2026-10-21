# Issue #39 — perfil financiero invocable

- Responsable: `/root/issue39_implement`; coordinación: `/root`.
- Estado: corrección de revisión preparada y entregada; pendiente segunda revisión independiente Standards/Spec. Issue abierto, no integrado ni cerrado.
- Rama/worktree: `codex/spec37-issue39` · `C:/Users/Lenovo/.codex/worktrees/spec37-issue39/finsim`.
- Base original de la tarea: `fe291ae76403d83e2cd50aec69a3b503c1c14d33` (`codex/spec37-verification`).
- Integración de #34: `e06ebc0565dd500adcb1eaa265f2a873beaac4b5`, incorporada en `ec5eb89101d4819a8b16da14a825e13f345c06b7` (AGP 9, Kotlin 2.4.21, soporte Gradle 9.3.1 y tarea `test:kmp` `:composeApp:testAndroidHostTest`).
- Perfil e implementación: `c0c700e0aa5859dc5b8f69e189094fbaa85a02a1`.
- Corrección P2 de precisión nominal-12: `e1fd108979a48536715c57a0171814be2b9c7ecb`.

## Entrega

`npm run verify:financial` invoca el perfil PF del motor y calidad en JVM host. El catálogo identifica 17 pruebas JUnit de tasas, cronogramas, redondeo, seguros, atrasos/recuperación, muestra ARS, resultado incompleto, validación pública e invariantes. Cada entrada identifica método, expectativa, criterio, referencia y fuente. Se reutilizan las pruebas públicas de `:shared:jvmTest` y se añade una comprobación de capital no positivo por `simulateLoan(Scenario)`.

La revisión P2 encontró que la prueba nominal-12 solo redondeaba la conversión a dos decimales y hacía una vuelta precisa para frecuencia 4. `RatesTest.testNominalFrequencies` ahora compara conversión y vuelta contra los literales independientes `0.01` y `0.12`, cada una con error absoluto menor a `1e-12`. `testTargetRateNominalFrequencies` también comprueba nominal-12 `0.12` y su retorno a mensual `0.01` contra esos literales con la misma tolerancia. Se conservó la promesa del catálogo en vez de debilitarla. No se modificó la lógica del producto ni se simuló un fallo inexistente.

El reporte JSON/Markdown, logs y JUnit se crean en un directorio único bajo `~/.finsim/verification/financial/<run-id>/`. La identidad incluye rama/commit y huella SHA-256 de cambios locales; registra entorno, comando, hashes de clases JVM y reportes JUnit. Cada método cuenta PASS solo al encontrar su testcase en XML nuevo, conservando el sufijo de destino `[jvm]`. FAIL, BLOCKED, NOT_RUN y NOT_APPLICABLE permanecen diferenciados; fallo o alcance seleccionado incompleto devuelve código no exitoso. No se añadieron dependencias ni framework de pruebas.

## Comprobaciones

- `npm run test:verification`: 4/4 selfchecks pasan; cubren parseo JUnit PASS/FAIL/skip, correspondencia de casos, sufijo `[jvm]`, ausencia de resultados, estado mixto y resumen no aprobable.
- `node --check scripts/verify-financial.mjs` y `git diff --check`: pasan.
- Corrida diagnóstica `2026-10-20T21-34-46-728Z-1c8ad1d7`: Gradle terminó correctamente pero el reporte quedó **INCOMPLETE** porque el matcher inicial no contemplaba `[jvm]`. Se conserva como evidencia histórica sin reinterpretarla; sus XML solo se usaron para depurar el reporter.
- Corrida confirmatoria original `2026-10-20T21-43-15-810Z-1b421421`: **PASS**, 17/17 casos en la base anterior a la integración de #34, Gradle 8.11.1/JVM 21.0.12.1.
- Corrida confirmatoria posterior a la corrección `2026-10-21T00-50-07-419Z-f9b870dc`: **PASS**, 17/17 pruebas catalogadas más ejecución Gradle identificadas, salida `0`, commit `e1fd108979a48536715c57a0171814be2b9c7ecb`, árbol limpio. Gradle efectivo 9.3.1, JVM 21.0.12.1, Node v26.8.1 sobre Windows; un worker, compilador Kotlin in-process y heap máximo 2 GiB. Evidencia completa en `C:/Users/Lenovo/.finsim/verification/financial/2026-10-21T00-50-07-419Z-f9b870dc/`.
- La corrida nueva acredita los dos criterios de precisión nominal-12 con sus aserciones literales. El build también pasó `:shared:jvmTest`.
- La evidencia confirma **NOT_RUN** para Wasm, APK Android e interacción UI. La muestra argentina es un caso controlado y no se presenta como regla legal o de prestamista.

## Límites y siguiente paso

PASS cubre solo los casos seleccionados en JVM host. No acredita FIN-01 en Wasm ni FIN-02/FIN-03/UI-01 en interfaz o APK. FIN-02/03 completos siguen pendientes de los recorridos de interfaz #40/#43; la muestra de motor no se presenta como cobertura íntegra.

La ejecución emitió la advertencia deprecada de Node al lanzar Gradle en Windows y Gradle reportó advertencias de código Kotlin ya presentes en la base. Ninguna afectó el resultado confirmatorio.

Próximo paso: segunda revisión independiente Standards/Spec del candidato `e1fd108`, usando el nuevo resumen publicado en #39. Mantener pendiente la cobertura Wasm/APK/UI; no integrar ni cerrar este issue desde esta rama.
