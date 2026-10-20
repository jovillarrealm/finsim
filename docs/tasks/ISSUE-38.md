# Issue #38 — T01 de spec #37

- Fuente de verdad: https://github.com/jovillarrealm/finsim/issues/38 y https://github.com/jovillarrealm/finsim/issues/37 (cuerpos y comentarios).
- Reserva: 2026-10-20, implementador `/root/t01_implement`, asignación de la coordinadora `/root`.
- Rama/worktree: `codex/spec37-t01`, `C:/Users/Lenovo/.codex/worktrees/spec37-t01/finsim`.
- Base inicial de implementación e inspección de producto: `0968ac92edf6acd859466b1f16d7fad1b1ebbbe5`, punta publicada de `codex/spec37-verification` al iniciar; contiene `main` publicado `839c69c6e8f7c114424acb6f3fa9b7aae91063a2` más coordinación.
- Punto fijo de la primera revisión: `523f91d1feb330f5f8f8b947271a35ac1ce7bbc9`; solo actualizaba coordinación respecto a la base inicial.
- Punto fijo actualizado para comprobar correcciones, asignado por la coordinadora: `8ef5fe3ce71ffe4433342e56a4c452a7b84e2054`, incorporado mediante merge local `8ad08153f2312f88c87d0024fd3c1f5fe853ceed`. Añade la limpieza revisada de tests #36 y su documentación. Las observaciones de la matriz conservan su procedencia histórica `0968ac92…`; no se atribuye el cambio de #36 a T01.
- Estado: en revisión de rama; entrega documental preparada, pendiente de revisor independiente e integración. Sin modificar el tablero compartido ni la coordinación SPEC37.

## Alcance reservado

Inventariar los criterios de #1–#36 por función/plataforma y publicar en #38 su correspondencia con #37, clasificación y evidencia separada de implementación observada. Registrar toolchain y contradicciones. Documentación auxiliar y traspaso en este archivo; GitHub conserva la matriz canónica.

No se cambia producto, lógica, reglas financieras, dependencias ni CI. Sin pruebas artificiales para documentación; TDD leído, se aplicaría si hiciera falta lógica. No acceder ni publicar documentos privados. Un revisor independiente revisará después de esta implementación; integración a cargo de la coordinadora.

## Resultado canónico en GitHub

[Índice, base, observaciones, procedimientos y resumen previo a revisión](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294210371).

La matriz se publica en cinco comentarios enlazados, con **422 filas**: **192 criterios AC**, **170 historias US** y **60 cláusulas adicionales**. Cada checkbox e historia de los cuerpos #1–#36 se transcribió una sola vez; las cláusulas adicionales enlazan decisiones, referencias numéricas y pruebas. Las filas separan clasificación del requisito, implementación observada y evidencia pendiente por plataforma. Los 18 IDs de #37 están representados. Este archivo es continuidad de ejecución, no otra especificación.

| Parte | Fuentes | Filas | Comentario |
| --- | --- | ---: | --- |
| 1 | #1–#11 | 115 | [Migración y funciones](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294200381) |
| 2 | #12, #15, #16 | 84 | [Presentación y resultados](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294201183) |
| 3 | #13, #17–#28 | 100 | [Ingesta y convergencia](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294202024) |
| 4 | #14 | 74 | [Ofertas y referencias](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294202733) |
| 5 | #29–#36 | 49 | [Verificación y toolchain](https://github.com/jovillarrealm/finsim/issues/38#issuecomment-6294203571) |

Lectura completa de cuerpos, comentarios y estados #1–#38 el 2026-10-20. Se conserva la impugnación del propietario a la declaración histórica de completitud de #17. No se modificaron cuerpos, etiquetas ni estados de los antecedentes. La [decisión posterior sobre moneda](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6294154389) se consultó e incorporó: columna 19 al final de CSV y Arrow, encabezado CSV «Moneda», campo de texto Arrow, COP/ARS/USD/EUR por fila, sin conversión. El código de la base sigue exportando 18; registrarlo no acredita corregirlo.

## Base y herramientas verificadas

`git ls-remote` confirmó `main` en `839c69c6e8f7c114424acb6f3fa9b7aae91063a2` y la integración en `0968ac92edf6acd859466b1f16d7fad1b1ebbbe5`. HEAD/rama coincidían con la asignación y el worktree estaba limpio antes de reservar.

- Producto publicado: Kotlin 2.2.20, Compose 1.9.3, AGP 8.7.3, R8 8.10.21; módulo Android+KMP sin separación de #34. `composeApp`: compile/target SDK 36, mínimo 26, JVM 21, aplicación `io.github.jovillarrealm.finsim_alpha`, versión 1.0.3/código 4. `shared` mínimo 24.
- npm lock: TypeScript 7.0.2, Vite 8.3.0, Playwright 1.63.0, Arrow 21.2.0. CI fija Java 21, Gradle 8.11.1 y Node 26; despliega Compose Wasm bajo `/finsim/`.
- Entorno consultado: Windows 11, Node 26.8.1, Java Microsoft 21.0.12.1 y Gradle 8.11.1; launcher/daemon Gradle con JDK 21.0.12.1. Kotlin 2.0.20 del ejecutable Gradle es su runtime embebido, no el plugin Kotlin del producto.
- En esta base no existen `check`, `test:e2e`, `verify`, `verify:all`, `verify:device`, `scripts/`, `tests/`, `docs/verification/`, ADR0003 ni los dos archivos `docs/agents/` enlazados por AGENTS. No se atribuyen herramientas o resultados de otros árboles a esta base.

## Resumen para revisión Standards / Spec

Ejecución documental: **T01-20261020-1007-0500**. Único archivo versionado cambiado: esta ficha. Comentarios públicos: matriz e índice en #38. Sin cambios de lógica, cálculo, UI, exportadores, dependencias, CI o documentos compartidos. No hay llamadores, pruebas, fixtures, configuración ni exports de producto que actualizar por este cambio documental.

Expectativa independiente: inventariar exactamente los AC/US del texto fuente, sin duplicados; cubrir #1–#36 y los 18 IDs #37; conservar procedencia y motivo/sucesor, separar observación de código de ejecución y no atribuir avances locales a una base publicada. La matriz se contrastó con los cuerpos y comentarios de GitHub, el árbol del commit fijo y la decisión posterior del propietario.

Comprobaciones realizadas:

- Conteo de cada checkbox e historia contra la fuente, presencia única de sus IDs y cobertura de los 36 issues y los 18 IDs de #37: correcto, 192 AC + 170 US + 60 cláusulas = 422 filas.
- Cinco comentarios bajo el límite de 65.000 caracteres. Lectura de vuelta desde GitHub: texto de las cinco partes y del índice idéntico al enviado, normalizando solo finales de línea y salto final. Suma de filas publicadas: 422.
- Inspección de archivos/configuración de la base y versiones locales; referencias remotas y rama/HEAD confirmadas. Revisión de rutas enlazadas y `git diff --check` antes del commit.
- Sin PDFs, raster, credenciales, datos privados ni números del corpus en la entrega. No se accedió a `snstv` ni a los dos documentos privados durante T01. Los valores de referencia de ofertas copiados en la matriz proceden de los casos sintéticos públicos de #14.

Hashes SHA-256 del texto público normalizado de cada parte (LF; sin salto final):

| Parte | SHA-256 |
| --- | --- |
| 1 | `0468bbe821bea19d632b39fa142fb9cce789570c4add65a2cc5b43e294dc2ecb` |
| 2 | `9797d0a5c24c66e6d2a98ad38b89452574a17c78e3d9ee2764932b70f019ae92` |
| 3 | `de6aa06176a6bae1aa1f454dfaeb6245fd69c38f9d56734812d5f8009e147dfa` |
| 4 | `398481c0d06c2928639b99c4b979abefe539780eaa674d44dde4fc31f91147b0` |
| 5 | `c2541c0a80f624931d0be4fa924e12aa41ca507072e94457473d84be8dd1c1a3` |

Los hashes de las partes 1 y 5 se actualizaron tras las correcciones de trazabilidad; los de las partes 2–4 permanecen iguales.

## Correcciones solicitadas por revisión independiente

La revisión inicial informó Standards sin hallazgos y dos P2 de Spec. Se aplica exclusivamente su corrección, manteniendo el estado en revisión; la aceptación de estas correcciones corresponde al mismo revisor, no al implementador.

- **Conciliación:** #1 US26 y #8 AC01–AC06 apuntan a la nueva observación **O13**, en lugar de O5 (ofertas). O13 enlaza `ReconciliationView.kt`, `reconciliation/Reconciliation.kt` y `ReconciliationTest.kt` de la base inspeccionada: formulario que recibe el resultado original, comparación pública `reconcileRow` y referencias literales de coincidencia/discrepancia. Se conserva NE y la necesidad de ejecutar PF/PW/PA; código y pruebas presentes no acreditan navegación, validación, teclado o lectura accesible.
- **Dos destinos de modelos:** #33 AC07 ahora termina en **NE→PDW/PDA**, conservando Docling web y Gemma Android. La corrida física continúa pendiente documentada; corregir el mapa no significa ejecutarla.

Ejecución documental de corrección: **T01-20261020-1621-0500**. Se editaron los comentarios existentes de las partes 1 y 5 y el índice, sin volver a publicar la matriz. Lectura de vuelta exacta de los seis comentarios y hashes correctos; permanecen **422 filas, 192 AC, 170 US, 36 issues**. Exactamente ocho filas cambian sus referencias de observación/perfil, con los textos de criterio intactos. Los 34 enlaces de código del índice actualizado resuelven en el árbol observado. No se hicieron nuevas corridas funcionales, revisión independiente propia ni cambios de producto. La incorporación de #36 se preserva intacta y la observación O12 queda identificada como histórica en el índice.

## Omisiones, brechas y riesgos

No se ejecutaron suites financieras, builds, recorridos web/APK, instalación/actualización, inferencia ni sesión de lector/audiencia: **NO EJECUTADA**, por alcance documental de T01. No se sondeó disponibilidad de teléfono/emulador/modelos y no se inventó un bloqueo de hardware. El índice público enumera procedimientos, prerrequisitos, expectativas y evidencia para reanudar cada perfil. No se necesitan pruebas artificiales ni TDD para esta ficha sin lógica nueva.

Brechas observadas por inspección de la base histórica `0968ac92…`, conservadas explícitamente: nominal oculta cuota pactada y el formulario no expone seguros/perfil; Android Arrow devuelve CSV; fake de visión y descarga sintética en commonMain; almacenamiento llamado persistente solo cuenta bytes; raster Wasm en memoria simulado y raster Android con archivo temporal; tema sin Typography tnum global; tests de hitos/ahorro reconstruían resultados sin observar UI. La integración posterior #36 sustituye esos tests y se conserva como entrega ajena revisada. Estas observaciones históricas no son resultados de una corrida nueva ni omisiones por falta de teléfono.

Pendientes de decisión registrados: política mínima Android, IDs F01–F06 financieros reusados con significados distintos, fallback nativo automático, límite de páginas, paletas exactas y presupuesto base sin medición. La dirección de modelos sí está resuelta por #32/#33: Gemma Android/LiteRT-LM y Docling web/Transformers.js; gate Android ≥8 GiB y presión de memoria separada. El corpus vigente es de dos PDFs privados con TXT, frente a uno en el cuerpo inicial de #37; la referencia semántica todavía requiere revisión del propietario. Esta ficha no altera esos documentos ni crea una referencia financiera pública.

La [aclaración móvil](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6293230094) permite continuar revisión/integración con corridas físicas pendientes, con criterio, plataforma, motivo, prerrequisito y procedimiento. Un fallo observado sigue siendo fallo. Ausencia de implementación real de un lector se registra separada de ausencia de hardware para ejecutarlo. Ningún criterio funcional queda aprobado por T01.

## Traspaso y siguiente paso

1. Mismo revisor independiente: comprobar ambas correcciones y esta entrega contra el punto fijo `8ef5fe3ce71ffe4433342e56a4c452a7b84e2054`; informar Standards y Spec por separado. La matriz conserva la procedencia de sus observaciones `0968ac92…` y distingue la limpieza integrada de #36. La implementación no se delegó.
2. Coordinadora: corregir hallazgos justificados, integrar y verificar; solo entonces cerrar #38. Commit de entrega identificado por `Ref #38`; se comunica con hash exacto en el comentario de traspaso y al coordinador.
3. Continuar únicamente con el ticket que asigne la coordinadora; los perfiles y brechas ya apuntan a #39–#48 y a #32–#36. No se inició ninguno durante T01.

Sin push ni integración de la entrega a la rama coordinadora. Se incorporaron únicamente las actualizaciones de integración `523f91d…` y `8ef5fe3…` autorizadas por la coordinadora. El tablero y SPEC37 permanecen bajo su control exclusivo.
