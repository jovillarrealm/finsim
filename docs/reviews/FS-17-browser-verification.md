# FS-17 — Verificación con Chrome DevTools

Fecha: 2026-10-14. Rama: task/fs-17-offer-comparison. Código comprobado: 66a78a0 (incluye las correcciones 2063906). Sin integración ni despliegue. Se retomó el handoff de C:/Users/Lenovo/AppData/Local/Temp/finsim-FS17-chrome-devtools-handoff.md.

**Estado posterior:** 49b13fe conserva las correcciones de f8551b4, recupera foco Web y conecta JSON de préstamos. Nueve pruebas de navegador y siete UI pasan; transferencia, simulación y recuperación Android verificadas. Los apartados iniciales son históricos; véanse las continuaciones finales y sus límites de lector/audiencia.

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

## Continuación: accesibilidad y transferencia — f8551b4

Los dos defectos concretos del P1 anterior están corregidos: nombres ausentes y árbol bloqueado tras seleccionar una opción. La aceptación completa de accesibilidad sigue pendiente.

### Cambio y causa

ComparisonField añade contentDescription con la etiqueta visible. ComparisonChoice despliega opciones dentro del formulario, anuncia estado y devuelve el foco al selector. No modifica dinero, validación, cálculos, persistencia ni versiones. Todos sus consumidores usan la misma costura.

El síntoma coincide con [CMP-10623](https://youtrack.jetbrains.com/issue/CMP-10623). Un comentario shortcut conserva el criterio para volver a menús: corrección de plataforma verificada. No se parcheó el DOM ni se cambió la política de DevTools.

### Comprobaciones actuales — PASAN

- gradle :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:wasmJsBrowserDistribution --console=plain: correcto, 4 min 2 s; 7 pruebas UI, APK y Web reconstruidos con la corrección.
- node --test --test-concurrency=1 kmp/composeApp/browser-tests/comparison-accessibility.test.cjs kmp/composeApp/browser-tests/comparison-transfer.test.cjs: **5 pruebas, 5 pasan, 0 fallos**; última ejecución conjunta 19,24 s.
- Las cuatro regresiones de accesibilidad fallaron contra 66a78a0 por los nombres ausentes y la desaparición del formulario tras selección. Con el parche pasan en 1280 y 360 px: nombre del campo, entrada de teclado, país/moneda, árbol conservado y reapertura/cierre con Espacio tras devolver foco.
- Chrome DevTools confirma nombres y formulario conservado después de Colombia; Espacio reabre/cierra el selector. Se capturó también el formulario en 360x800 con mobile/touch.
- La quinta prueba usa Chrome aislado mediante Playwright ya instalado, con permiso explícito del comando. Introduce dos cotizaciones por UI y lee la descarga real, incluido nombre, tipo y entradas. Edita A a 160, aplica/guarda 1280, rechaza JSON inválido conservándolo e importa el archivo descargado para recuperar 1200. El guardado 1280 permanece hasta Guardar. Tras recarga muestra A1200/B1120; guardar el préstamo anterior después de una recarga no cambia la clave de comparación.
- DevTools upload_file mantiene su restricción de rutas. El ciclo real de archivos se comprobó mediante el comando Playwright autorizado, sin modificar permisos globales de DevTools.
- No se repitió shared:jvmTest: el módulo shared no cambió. Sus 115 pruebas de 66a78a0 siguen siendo evidencia vigente del mismo código, distinta de las 7 UI y 5 de navegador ejecutadas ahora.

Reproducción: dependencias raíz instaladas y Vite sirviendo el Wasm actual en 4174. En este equipo NODE_PATH se fijó solo para el comando a C:/Users/Lenovo/Documents/finsim/node_modules para reutilizar dependencias; no hay nueva dependencia ni cambio global.

### Android — cotización y recuperación PASAN

APK instalado mediante adb install -r en emulator-5554, sin borrar datos. UIAutomator confirma los nombres en nodos padre android.view.View y su hijo EditText. Se introdujeron por controles reales país CO, moneda COP, objetivo 1000, A8x150 y B8x140; se aplicó, guardó, hizo force-stop/start de Finsim y abrió Ofertas. Captura y XML fresco confirman A1200/B1120 recuperados. Los campos inicialmente vacíos se comprobaron antes de escribir.

UIAutomator no entregó raíz durante algunos arranques/transiciones. Esos intentos no acreditan el producto; no se utilizó un XML antiguo para declararlos correctos. Después se tomó captura y dump fresco con ambos resultados. No se observó un fallo visible de Finsim en esa comprobación.

### Alcance y pendientes

Los clics de navegador usan force:true porque Compose expone nodos semánticos transparentes sobre canvas. La transferencia usa 1280x5000 para aislarla del desplazamiento: NO demuestra visibilidad ni acceso físico a todos los controles en teléfono compacto.

Los intentos de automatizar el flujo completo con viewport normal no alcanzaron campos fuera de pantalla cuyos bounds accesibles eran 0x0. Esto no demuestra un nuevo defecto de cálculo/importación, ni permite declarar resuelto el desplazamiento accesible. Los scripts esperan el foco del input nativo y verifican el valor en el campo esperado antes de aplicar.

Pendientes: lector real y recorrido completo de teclado/scroll a 360 px, simulación/contexto y transferencia Android, documentos antiguos E2E y audiencia. Sin integración ni despliegue; deuda anterior KMP abierta.

### Revisión final

Standards: 0 incumplimientos documentados nuevos. Spec: 0 hallazgos nuevos en parche y scripts; reconoce corregidos los dos defectos específicos y mantiene límites de móvil/lector. Dos revisores independientes leyeron el diff desde 2b04554 y ambos scripts; no sustituyen las ejecuciones coordinadas.

### Evidencia añadida

En ../../.scratch/financing-offer-comparison/verification/:
- browser-fields-fixed.txt: nombres y árbol conservado después de Colombia.
- browser-mobile-fields-fixed.txt y browser-fields-fixed-mobile.jpg: formulario en 360x800.
- android-accessibility-fixed.xml: etiquetas antes del ingreso.
- android-quotes-restored.xml y android-current-screen.png: ofertas recuperadas tras reinicio.

Los apartados iniciales pertenecen a 66a78a0 y quedan como historial. Esta continuación describe el comparador en f8551b4.

## Continuación desde el handoff de pendientes — 49b13fe

2026-10-14 · /root, misma reserva y worktree. Sin integración ni despliegue.

### Correcciones y diagnóstico

1. Tab desde un campo retiraba su input HTML; el foco nativo quedaba en body y la siguiente tecla no llegaba a Compose. Se reprodujo en 360x800 y escritorio, con eventos de teclado y foco observados. Coincide con [la corrección upstream CMP-9388 / PR 3246](https://github.com/JetBrains/compose-multiplatform-core/pull/3246). main.kt Wasm espera el shadow root y devuelve foco al canvas únicamente al retirar el input enfocado, sin otro elemento enfocado y con ventana activa. El comentario shortcut fija cuándo retirar el arreglo. No se modifica el árbol semántico ni se actualiza el stack.
2. La transferencia de préstamos anterior no estaba conectada en KMP: App anunciaba exportación con callback nulo y su importador solo ofrecía extractos. App reutiliza ComparisonFileActions para importar/exportar JSON validado por WorkspaceStore; el nombre de archivo se pasa por llamada. Se conserva Importar extracto como acción separada y se rechaza importación con eventos pendientes. Sin cambios de contrato, motor, persistencia shared ni cálculo.

### Verificación final del código — PASA

- Desde kmp: gradle :composeApp:wasmJsBrowserDistribution :composeApp:assembleDebug :composeApp:testDebugUnitTest --console=plain — correcto, 5 min 2 s. XML: 7 pruebas UI, 0 fallos. Warnings deprecados/bundle conocidos; no errores de compilación.
- Con NODE_PATH=C:/Users/Lenovo/Documents/finsim/node_modules: node --test --test-concurrency=1 kmp/composeApp/browser-tests/comparison-accessibility.test.cjs kmp/composeApp/browser-tests/comparison-transfer.test.cjs kmp/composeApp/browser-tests/comparison-compact.test.cjs kmp/composeApp/browser-tests/legacy-transfer.test.cjs — **8 pruebas, 8 pasan**, 122,55 s.
- Las nuevas regresiones de teclado y documento antiguo fallaron antes de la corrección. El script de transferencia existente ahora espera geometría visible y comprueba el borrado completo del campo antes de escribir, para no aceptar 160.001 como 160.
- shared no cambió respecto a 66a78a0: se conservan sus 115 pruebas previas, sin ejecutarlas de nuevo. git diff --check correcto.
- DevTools recargó el Wasm actual sin mensajes de error de consola. SHA256 de composeApp.js: C3CB57FC779C6354E24EC8479659BFB922BB6833E44E3F68EFCACB86D3F8FA8D.

### Alcance Web acreditado

La nueva prueba compacta usa 360x800 con mobile/touch, clics físicos dentro de bounds visibles y rueda dentro del contenido; no usa force:true ni modifica localStorage para crear el caso. Completa dos cotizaciones A8x150/B8x140, confirma cargos, añade contexto 600/400/100, aplica y rechaza 150.001 conservando 1200. Corrige a 160, obtiene 1280, guarda 170 pendiente, recarga y recupera aplicado 160/borrador 170, descarta y conserva indicador sin guardar. Desplaza la lista mensual anidada, selecciona mes 1 y comprueba márgenes -60/-40 y ancho360 sin desbordamiento.

La regresión de teclado comprueba salida del campo con Tab, activación/repetición con Espacio, entrada en el siguiente campo y retorno con Shift+Tab. No acredita por sí sola todo el recorrido solo con teclado.

El documento antiguo reproduce el fixture Monedas de tests/acceptance.spec.ts: moneda omitida, capital 1000, EM 1 %, 2 meses y abono 100 en mes 1. Importación real recalcula caja 1014.02; descarga escenario-finsim.json conserva el evento, moneda COP y entradas sin cronogramas derivados. Versión 99 se rechaza conservando datos/guardado y recarga. Esta prueba acredita el schema histórico sintético, no un extracto bancario real.

### Android acreditado hasta aquí

Datos sintéticos originales respaldados antes de alterar. La primera ronda de transferencia/rechazos se ejecutó con el APK f8551b4; el lector de archivos no cambió en 49b13fe. Después se instaló el APK 49b13fe con -r para F06, contexto, redondeo y documento antiguo. Se exportó mediante CreateDocument a Downloads, se leyó el archivo real y se importó mediante OpenDocument. Contiene A8x150/B8x140. JSON inválido, versión99 y archivo de 2.000.001 bytes fueron rechazados; XML fresco confirma A1200/B1120 conservados. Cancelar el selector retorna a las mismas vistas. No se usaron dumps antiguos cuando UIAutomator informó raíz nula.

F06 se introdujo por archivo JSON real desde el selector, seguido de acciones UI: resumen A1015.02/B1000; tabla, desplazamiento y selección común del mes 1. XML fresco acredita A cuota507.51, capital497.51, interés10.00 y saldo502.49; B cuota500/capital500/saldo500; márgenes -407.51 y -400.00. Luego se editó ingreso600→650 por UI, se guardó pendiente y se reinició. La UI recuperó aplicado F06/borrador 650; descartar restauró 600. No se afirma introducción manual completa de todos los campos de la simulación a partir de esta importación.

UIAutomator ocasionalmente devolvió raíz nula tras arranques/transiciones. Se eliminaron dumps de prueba antes de generarlos, se descartaron esos intentos y se obtuvieron captura/XML nuevos. Capturas mostraron la app; logcat AndroidRuntime no mostró excepción en la comprobación realizada. No se atribuye una causa a la plataforma ni se presenta eso como una prueba exhaustiva de estabilidad.

### Límites y siguiente paso

No se ejecutó lector real: no había NVDA en las rutas comprobadas, proceso de lector activo ni TalkBack instalado/activado en el emulador; no se instaló ni se simuló un lector. Narrador de Windows no se probó. Sigue pendiente el recorrido íntegro solo con teclado, el teclado virtual/hardware Web y la observación de una persona destinataria. Se solicitó disponibilidad de audiencia, sin sesión observada ni aprobación inferida.

La revisión local del parche conserva el módulo shared, la validación existente y los límites de alcance. No sustituye una revisión independiente de toda la migración. La deuda anterior de KMP enumerada en FS-17.md sigue abierta. No marcar FS-17, KMP o los diez tickets hechos; revisión/integración y aceptación siguen separadas.

### Redondeo y recuperación Android añadidos

La variante importada de F06 usa capital 100, tasa cero y 3 cuotas. La UI muestra rango 33.33–33.34, total 100 y último mes 3; seleccionar el extremo final del control mensual muestra mes 3, cuota 33.34 y saldo 0.00. Evidencia: android-rounding-summary.xml y android-rounding-month3.xml. El ingreso pendiente 650/restauración 600 tiene XML propio: android-pending-context-restored.xml. Son fixtures sintéticas importadas por el selector y acciones reales de edición/selección; no una referencia contractual.

### Compatibilidad de préstamos en Android — PASA

Con el APK 49b13fe se importó el mismo documento histórico Monedas por OpenDocument. XML fresco muestra cuota COP507,51, intereses COP14,02 y caja COP1.014,02. CreateDocument propone escenario-finsim.json; se eligió el nombre de prueba fs17-legacy-export.json para evitar colisiones. La descarga local del archivo escrito contiene457 bytes y conserva moneda COP y abono extra100. Una lectura anterior al fin del callback encontró el archivo todavía vacío y no se contó como evidencia; la relectura completa validó su contenido.

Evidencia: android-legacy-imported.xml y android-legacy-exported.json. El flujo no guardó el préstamo importado encima del préstamo anterior.

JSON inválido fue rechazado también en la herramienta de préstamos Android, conservando Monedas y caja1014.02 (android-legacy-rejected.xml). Al terminar se cerró/reabrió la app y se compararon todas las claves y valores de SharedPreferences contra el respaldo inicial: coincidencia exacta, sin guardar el préstamo de prueba sobre el anterior. La comparación inicial quedó restaurada explícitamente por importación/guardado antes de ese cierre.

### Recorrido representativo solo con teclado

comparison-keyboard.test.cjs cubre entrada a Ofertas por Tab/Espacio, país/moneda, dos cotizaciones y confirmación de cargos, contexto600/400/100, guardado del borrador y aplicación. Con Shift+Tab abreA, rechaza150.001 sin reemplazar1200, corrige160 y aplica1280. Con flechas selecciona el mes2; con Tab/Espacio abre la tabla, selecciona0 y después1 y lee márgenes -60/-40. Los campos editados y el botón mensual deben estar enteramente dentro del viewport. No se usa clic para el caso final ni se alteran DOM/almacenamiento para completar datos.

La ejecución conjunta de cinco archivos produjo9/9 pruebas correctas en150,55 s. Después se sustituyó la activación inicial por clic del caso de teclado por entrada con Tab/Espacio y se repitió ese caso completo de forma enfocada. No cambió código de producto desde49b13fe y no se repitieron las ocho pruebas sin cambios. La prueba solo reproduce comportamiento observable de teclado en Chrome de escritorio a360x800; no es lector de pantalla ni hardware móvil.

El recorrido representativo de teclado queda acreditado; las menciones anteriores de ese pendiente son históricas. Siguen pendientes lector real, teclado virtual/hardware Web, audiencia y revisión/integración. Los criterios técnicos demostrados de ticket10 están marcados, conservando el ticket abierto y sin cierre de FS-17/KMP.

### Cierre de esta continuación

Caso de teclado final sin clic inicial:1/1 correcto,34,60 s. Las otras ocho pruebas sin cambios conservan la ejecución conjunta9/9 anterior; no hubo modificaciones de producto posteriores a49b13fe. Test de teclado entregado en46531a1. Revisión independiente final de ambos ejes: [informe](FS-17-continuation-review.md), sin defectos nuevos; heurística menor de nombre conservada sin refactor. Lector y audiencia siguen pendientes.

Se retiraron únicamente temporales propios del diagnóstico, el fixture de2MB y el respaldo local de preferencias después de comprobar restauración exacta. Se conservan fixtures sintéticas y evidencia útil en el worktree. El checkout original finsim y sus temporales no se tocaron.
