# FS-17 — Verificación con Chrome DevTools

Fecha: 2026-10-14. Rama: task/fs-17-offer-comparison. Código comprobado: 66a78a0 (incluye las correcciones 2063906). Sin integración ni despliegue. Se retomó el handoff de C:/Users/Lenovo/AppData/Local/Temp/finsim-FS17-chrome-devtools-handoff.md.

## Compilación y pruebas — PASA

Se completó la actualización pendiente del DSL: Android usa compilerOptions/JvmTarget.JVM_21 en ambos módulos y Wasm usa outputModuleName. No se cambiaron fórmulas ni código de producto en esta sesión. Referencia: [opciones oficiales de Kotlin](https://kotlinlang.org/docs/gradle-compiler-options.html).

Desde kmp:
- gradle :shared:jvmTest --tests com.finsim.comparison.ComparisonWorkspaceTest :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution --console=plain — correcto; 18 pruebas enfocadas, APK debug y Web producción actuales. 13 min 10 s.
- gradle :shared:jvmTest :composeApp:testDebugUnitTest --console=plain — correcto; XML: 115 pruebas shared y 7 HorizontalComparisonChartTest, 0 fallos/errores/omitidas.
- git diff --check — correcto.

Artefactos: kmp/composeApp/build/outputs/apk/debug/ y kmp/composeApp/build/dist/wasmJs/productionExecutable/. Vite sirvió este último en http://127.0.0.1:4174/. No se probó el React legado como sustituto.

## Navegador — comprobaciones que PASAN

Chrome DevTools MCP funciona: list_pages, new_page con contexto aislado, navegación, formularios, teclado, snapshots y capturas. Los contextos fs17-verification y fs17-accessibility-repro contienen exclusivamente datos sintéticos.

- Dos cotizaciones introducidas por UI: objetivo 1000, A 8 cuotas de 150, B 8 de 140. Aplicar muestra 1200 y 1120, ambos parciales al no confirmar todos los cargos; costo nominal sin completar, conforme a F03.
- Una edición monetaria inválida se rechaza y conserva resultados aplicados. La herramienta fill_form sobre un campo ya poblado produjo .001150 al intentar 150.001; se verificó el valor realmente mostrado, no el valor solicitado. Corrección mediante clic, Control+A y type_text a 160: A pasa a 1280; escala y detalle mensual usan 160 y B sigue 140.
- Tabla: selección común del mes 1 actualiza el detalle de ambas ofertas. La tabla dentro de LazyColumn se recorrió con rueda de ratón; no acredita desplazamiento con lector.
- Guardar un pendiente de 170 conserva aplicado 160 y referencia 150. Recargar restaura borrador 170 y resultado 1280 sin aplicar silenciosamente. Descartar vuelve a 160 y marca trabajo sin guardar.
- Exportar invoca la descarga de comparacion-finsim.json. Se inspeccionó el Blob generado: tipo financing-comparison, versión 1, aplicado/borrador/referencia independientes. No se confirmó el archivo descargado en disco ni una ida y vuelta de archivos.
- F06: fixture cargada directamente en localStorage, seguida de acciones UI Aplicar/tabla/selección. A: capital 1000, EM 1%, 2 cuotas de 507.51, total 1015.02; capital/interés/saldo mes 1: 497.51/10.00/502.49; mes 2: 502.49/5.02/0.00. B a tasa cero: 500 por mes.
- Contexto de F06: ingreso 600, gastos 400, cuotas vigentes 100; margen mensual A -407.51 y B -400.00, sin recortar signos.
- Variante F06, con cuota explícita compatible: capital 100, tasa 0, plazo 3, cuota 33.33. Tabla: 33.33, 33.33, 33.34; saldo cero y horizonte 3, sin cuarto mes.
- F02: fixture local + Aplicar. A precio 900, aporte 200, cargo inicial 10, capital 700, tasa 0, 2 cuotas: inicial 210, cuotas 350, total 910, costo 10. B precio 1000, aporte 0, 4 cuotas cotizadas de 250: total 1000, costo 0. B menos A: total +90, inicial -210, último mes +2.
- Navegar al préstamo anterior y Guardar mantiene el comparador; claves separadas finsim.scenario y finsim.comparison.v1.
- Emulación real 360x800, escala 1, mobile/touch: pantalla inicial y resúmenes de F06 legibles, documento de ancho 360 sin desbordamiento horizontal. No acredita el flujo compacto completo. resize_page por sí solo limitó el ancho a 500; se usó emulate para obtener 360.
- Recursos locales recibieron 200/304; sin errores de consola. Advertencia WebGL INVALID_ENUM al consultar WEBGL_debug_renderer_info.

Las fixtures de F02/F06 prueban recuperación, cálculo y presentación mediante el almacenamiento público; NO acreditan entrada manual completa ni importación de archivos de esas referencias.

## Accesibilidad — FALLA (P1)

Reproducción mínima en contexto limpio, sin fixtures:
1. Abrir Ofertas; snapshot muestra los textbox sin nombre accesible pese a las etiquetas visibles.
2. Abrir País de la operación y seleccionar Colombia.
3. La captura muestra el formulario con País de la operación: Colombia y menú cerrado.
4. El DOM de accesibilidad en document.body.shadowRoot, cmp_a11y_root, y take_snapshot conservan solamente los botones Colombia/Argentina. Se reprodujo en una segunda pestaña y persiste al redimensionar.

Costuras: ComparisonField y ComparisonChoice en ComparisonScreen.kt. No se atribuye una causa interna concreta al framework sin diagnóstico. La actualización de Compose habilitó un árbol inicial, pero no satisface el flujo equivalente exigido por spec y ticket 10. El slider también aparece como generic; la alternativa por botones de tabla se comprobó con ratón.

Evidencia en ../../.scratch/financing-offer-comparison/verification/:
- browser-fields.txt — snapshot inicial con campos sin nombre.
- browser-a11y-menu.txt — árbol posterior a seleccionar Colombia.
- browser-a11y-after-selection.jpg — formulario visible sin menú; corresponde al árbol anterior.
- browser-f06-mobile.jpg — resumen F06 en 360x800.

[Compose 1.9 documenta accesibilidad inicial y límites en contenedores con scroll/sliders](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html#accessibility-support); esa documentación no reemplaza la comprobación observable.

## Transferencia — NO DISPONIBLE por restricción de herramienta

upload_file rechazó C:/Users/Lenovo/Documents/finsim/.scratch/fs17-verification/exported.json con:
Access denied: path ... is not within any of the configured workspace roots.

No se cambió la política ni se eludió la carga rechazada. La importación real, su rechazo no destructivo en pantalla y el ciclo exportar/importar permanecen pendientes. Los contratos de importación sí están cubiertos por las pruebas shared que pasaron; son evidencia distinta.

## Revisión final de correcciones

Dos revisores de lectura independiente, git diff e93692e -- kmp, incluyendo el DSL actual. La revisión anterior desde ac7b763 permanece en FS-17.md y su deuda no se reabre ni se declara resuelta aquí.

### Standards

0 incumplimientos documentados nuevos. Se conserva el motor shared, la distinción desconocido/cero y expectativas literales; se retiró la ruta VLM ficticia. Observación opcional: la comprobación de anualidad con cuota explícita se repite cada mes; no es incumplimiento ni bloqueo y no se optimizó en esta verificación.

### Spec

1 hallazgo P1: accesibilidad Web parcial, confirmado arriba; incumple acceso equivalente de spec y ticket 10. No se detectaron nuevas desviaciones numéricas ni ampliación innecesaria en las correcciones revisadas. Revisor estático; el detalle de pruebas adicionales F02/F06/móvil procede de esta sesión coordinadora.

## Pendientes y siguiente paso

Corregir nombres/foco y restauración del árbol al cerrar desplegables; repetir flujo de teclado y lector real, tabla y desplazamiento a 360 px. Habilitar una ruta permitida de archivo para DevTools y comprobar importación/cancelación/rechazo e ida y vuelta. Instalar el APK reconstruido conservando datos y completar los flujos Android. La aceptación de una persona de la audiencia sigue pendiente. No se cierra FS-17/KMP ni los diez tickets.
