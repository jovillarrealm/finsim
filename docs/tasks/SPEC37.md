# Spec #37 — coordinación de implementación y verificación

- Responsable: /root, sesión coordinadora.
- Fuente de verdad: https://github.com/jovillarrealm/finsim/issues/37 y sus comentarios/sub-issues.
- Rama de integración: `codex/spec37-verification`.
- Worktree de integración: `C:/Users/Lenovo/.codex/worktrees/spec37-verification/finsim`.
- Base publicada: `839c69c6e8f7c114424acb6f3fa9b7aae91063a2`.
- Estado: en curso, publicación de tickets autorizada por el propietario el 2026-10-20.
- Método autorizado: dos subagentes secuenciales por ticket, implementador y revisor de Standards/Spec; integración a cargo de /root.

## Acuerdos operativos

- Los tickets y sus dependencias se consultan y registran en GitHub; este archivo es continuidad auxiliar, no una spec paralela.
- El corpus privado contiene dos PDFs y un TXT ampliado. Los documentos y la referencia permanecen fuera de Git.
- La falta de teléfono permite omitir corridas móviles con motivo, criterios y procedimiento de reanudación. No convierte esas capacidades en aprobadas ni bloquea desarrollo o revisión.
- Cada implementador parte de la punta integrada. Cada revisión usa como punto fijo el commit de integración al asignar el ticket. Los dos ejes de revisión se mantienen separados, aunque el revisor los ejecute secuencialmente por instrucción del propietario.
- No incorporar los cambios ajenos sin commit de otros checkouts. No cerrar antecedentes por existencia de avances locales.

## Traspaso actual

- Once tickets #38–#48 publicados con etiqueta `ready-for-agent`, sub-issues y dependencias nativas verificadas. Autorización/corpus actualizados en el comentario de #37; columna19 moneda confirmada por el propietario en #37#issuecomment-6294154389.
- #38 integrada y publicada en `ea7e1fe62dc3e5f30510d1cabde0f92c047e3fbf`, cerrada tras Standards0/Spec0. Matriz canónica422criterios y34referencias; ambos hallazgos iniciales corregidos y verificados por el mismo revisor. Informe #38#issuecomment-6301803848.
- #36 revisada (Standards 0 / Spec 0), integrada en `8ef5fe3ce71ffe4433342e56a4c452a7b84e2054` y cerrada en GitHub. Evidencia: 115 pruebas shared JVM + 5 host y 13 recorridos de navegador aprobados; sin cambios de producción. Informe publicado en #36#issuecomment-6301672098. Rama de integración publicada; todavía no integrada en main.
- Base: `:shared:jvmTest` 115 y `:composeApp:testDebugUnitTest` 8 pruebas pasaron, sin fallos. Build Wasm de producción pasó. Estas comprobaciones describen la base publicada, no acreditan las entregas nuevas.
- #34 integrada y publicada en `804997d635645fdb1cebac651518ef677819673e`: AGP9.1.1/Gradle9.3.1/Kotlin2.4.21/compileSdk37, Compose1.9.3 conservado. Standards0; Spec conserva dos criterios parciales (launch bloqueado por AVD/ADB y actualización firmada no ejecutada). #34 permanece abierta, con reanudación en #43/#44; no son fallos observados del APK. Pruebas115shared+5host, releasefirmado, Wasm y13browserPASS; artefactos/logs/XML preservados externamente.
- El prerrequisito de compilación compatible de #32 ya está integrado, por eso se levanta su arista de bloqueo por #34 sin cerrar ésta ni certificar ejecución móvil. Por instrucción reiterada del propietario, todos los subagentes activos usan Luna/max. /root/issue32_implement_luna reemplaza al antiguo implementador interrumpido, conserva su rama/worktree y preparación parcial, y trabaja sobre la integración compatible. #45 posee los contratos de revisión/mapeo; #32 prepara runtime sin invadirlos.
- Siguiente paso: terminar/revisar la matriz #38, integrar; continuar la frontera de dependencias. Las corridas de teléfono siguen separadas de la posibilidad de progresar/revisar código.
- Referencia privada: ambos PDFs inspeccionados; trece pares del TXT contrastados con el PDF hipotecario y saldos del resumen de cuenta comprobados independientemente. Inferencia real no ejecutada.

## Reservas de la frontera tras #38

- #39: /root/issue39_implement y después revisor independiente por asignar, Luna/max. Rama `codex/spec37-issue39`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue39/finsim`. Perfil financiero y evidencia; comparte sólo adición de scripts npm con #34, resolver preservando ambas entregas.
- #45: /root/issue45_implement y después revisor independiente por asignar, Luna/max. Rama `codex/spec37-issue45`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue45/finsim`. Referencia privada y revisión/mapeo de importación; no implementar runtime ni tocar la referencia financiera del motor.
- Ambas reservas parten de este registro después de integrar #38. #34 conserva el único slot de compilación pesada hasta finalizar; las siguientes tareas coordinan sus corridas con /root.
- Slot actual: #34 terminó y AVD se detuvo; #39 confirmó17casosJVM y pasó a revisión; #45 ejecuta red/green KMP secuencial. #32 espera para compilar hasta liberación explícita.

## Frontera después del perfil financiero

- #39 integrada y publicada en `277fe3f57f9654600dfacaa3ffa689e794c5bb47` y seguimiento `a56b6252b45b9eba0e15d1a570e5924d9adcf1a7`; cerrada tras Standards0/Spec0. Informe #39#issuecomment-6305474126. Se corrigió la sobreacreditación nominal12 y se repitió PF con Gradle9.3.1:17casos+ejecución PASS. El seguimiento de formato pasa chequeo del rango completo y4selfchecks, sin repetir Gradle. Wasm/APK/UI quedan fuera del alcance aprobado del perfil JVM.
- #45 candidato `ebdbeae` pasó124JVM,5host y compilaciónWasm; su revisor encontró dos defectos (moneda destino de conciliación y precisión monetaria redondeada), dos parciales (unidades de referencia/evidencia de UI) y dos heurísticas de duplicación. Mismo implementador corrige antes de devolver al mismo revisor; no integrado ni cerrado. Resumen e informe locales disponibles durante interrupción de conexión con GitHub.
- #32 compiló APKdebug y Wasm con runner real y pasó las pruebas dirigidas; conserva2FAIL de contratos antiguos de fake en suite general, pendientes de adaptar tras integrar #45. No hay inferencia ni ejecución APK acreditadas. Modificaciones posteriores al build se identifican como no verificadas hasta la corrida final.
- #40 reservada a /root/issue40_implement y después revisor independiente por asignar, ambos Luna/max. Rama `codex/spec37-issue40`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue40/finsim`, desde este registro publicado. Configuración/eventos/gráficos web y perfil verificable; coordinar cambios puntuales de App.kt con #45 (moneda de importación) y #32 (fábrica de runtime), sin refactor general.
- Slot pesado actual: #45 para sus correcciones y UI; #40 prepara sin compilar hasta autorización. El tablero GitHub conserva specs/tickets; estas notas registran continuidad y límites de evidencia.
- #41 reservada a /root/issue41_implement y después revisor independiente, ambos Luna/max, desde este registro posterior a999765d. Rama `codex/spec37-issue41`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue41/finsim`. Persistencia/JSON/copiar/CSV/Arrow con columna19 Moneda; coordinar cambios de App.kt y scripts browser con #40/#45. Se inicia al liberar un slot de agente; sin compilación pesada simultánea.
- Resumen de avance canónico actualizado en #37#issuecomment-6305680321; revisión inicial45 publicada en #45#issuecomment-6305666448. Worktree39 limpio e integrado; prueba compilada/JUnit preservada externamente antes de solicitar archivo.
- #42 reservada a /root/issue42_implement y después revisor independiente, ambos Luna/max, desde este registro posterior a08f15e6. Rama `codex/spec37-issue42`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue42/finsim`. Conciliación/ofertas web con expectativas independientes; preparar casos sin invadir los contratos de #45 ni el perfil de #40. Se inicia al liberar el turno parcial de #41; validación final sobre las dependencias integradas.
- #41 detectó exportación CSV/Arrow permitida con lote pendiente; incorpora la guardia completa App.kt→MonthlyTable y la columna19 Moneda en firmas comunes, Wasm y Android. Checkpoint parcial con pruebas ligeras; validación real espera #40 integrado. El mismo implementador retomará, no cuenta como entrega aprobada.
- #43 reservada a /root/issue43_implement y después revisor independiente, ambos Luna/max, desde este registro posterior a a289b1b. Rama `codex/spec37-issue43`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue43/finsim`. Se iniciará al liberar un slot; perfil Android financiero con protección de estado y evidencia honesta de corridas omitidas. No usar el teléfono del usuario ni asumir el serial/densidad del runner histórico.
- #45 seguimiento final: 131 pruebas shared y5host aprobadas, compilación y distribución Wasm aprobadas. Falta interacción controlada y segunda comprobación del mismo revisor. #40 posee ahora el slot Gradle; #45 puede observar su distribución en navegador sin recompilar.

## Reanudación y dispositivo físico — 2026-10-21

- El propietario confirmó que el móvil está disponible y autorizado por USB. Esta autorización sustituye el impedimento anterior de disponibilidad; se permiten las acciones necesarias de prueba conservando datos, sin borrar almacenamiento ni desinstalar aplicaciones.
- #43 posee el dispositivo físico. Detectó ausencia del paquete canónico y verificó firma/hash del release preservado de #34 antes de instalarlo como base histórica. El APK instalado coincide con ese artefacto. No representa cambios todavía no integrados de #40/#45/#32. La primera corrida física conserva un fallo de navegación después del teclado; se acota el recorrido antes de repetirlo. Autorización y alcance publicados en #43#issuecomment-6309182537.
- Inventario observado: Android17/API37, pantalla1080×2340 a480dpi, RAM total11.36GiB. Las cuatro pestañas medidas tienen52–88dp; eso no acredita todos los controles, ausencia de presión de memoria ni rendimiento/inferencia Gemma.
- #45: la segunda revisión detectó normalización prematura de dinero JSON VLM; informe #45#issuecomment-6306161317. El mismo implementador reprodujo1.005→1005.00 en orquestación y corrigió la frontera JSON. Candidato `adb3439abb779ee9cfef9634afead11062f52f70`,134pruebas shared aprobadas; el mismo revisor comprueba el delta antes de integrar. La evidencia Wasm/UI anterior conserva su fuente anterior al nuevo guard; distribución final consumidora pendiente. Resumen #45#issuecomment-6309243794.
- #40 conserva los fallos históricos de compilación, dependencias y acceso por teclado/nombres accesibles. Corrige la semántica del formulario y añade reporte inicial incompleto/progreso para conservar evidencia si hay interrupciones. Las comprobaciones ligeras pasan; nueva compilación y recorridos pendientes. Recuperó el slot Gradle después del GREEN JVM de #45.
- #41 y #42 siguen en checkpoints parciales `0588874` y `4d535ef`, sin verificación Wasm ni revisión independiente. Retomar los mismos implementadores después de integrar #40/#45. La distinción entre país de oferta y perfil argentino del préstamo quedó publicada en #42#issuecomment-6306076959.

## Estado coordinado — 2026-10-21

- #45 integrada y publicada en `89f4f6127be38bbf40e0e47a6cab77bf2e7adf1d`, cerrada después de corregir moneda de destino, precisión monetaria y normalización JSON previa a validación. Última revisión del delta Standards0/Spec0;134pruebas shared JVM aprobadas. Informe canónico #45#issuecomment-6309355198. Las capturas Wasm anteriores no acreditan el guard JSON final; el próximo build consumidor lo incorporará.
- El arranque físico del release base #34 ya está aprobado por evidencia de #43, publicado en #34#issuecomment-6309747448. #34 permanece abierta por actualización firmada con escenario preservado, aún pendiente de #44. La corrida global #43 fue FAIL por foco/teclado; no convertir el PASS parcial de arranque en aprobación financiera.
- #43 liberó el dispositivo mientras completa las recetas financieras reales del verificador. El inventario debe conservar la escala de fuente observada1.5; no reducirla para pasar ergonomía. No borrar datos ni desinstalar para resolver firmas.
- #32 pasa143pruebas shared y prepara la revalidación host/APK/Wasm tras ajustar cancelación/liberación del runtime. No se han descargado pesos ni probado inferencia. Conserva el único slot Gradle pesado.
- #40 corrige el acceso a campos fuera del viewport y el cierre/tiempo límite de los casos. La corrida web bloqueada se conserva como incompleta, con resultados parciales; no representa PASS. #41/#42 esperan su integración para reanudar sus mismos implementadores.
- Siguiente frontera: revisión independiente de #32/#40 al entregar; completar #43 y reservar #44. #33 espera #32; #46/#47 y el catálogo #48 siguen pendientes. GitHub conserva el alcance y las decisiones vigentes.

## Reserva siguiente — #44

- #44 queda reservada para `/root/issue44_implement` y después `/root/issue44_review`, ambos Luna/max, al liberarse un slot. Rama prevista `codex/spec37-issue44`, worktree previsto `C:/Users/Lenovo/.codex/worktrees/spec37-issue44/finsim`, desde la punta de integración publicada al iniciar. No se ha iniciado todavía.
- Reutilizar el perfil Android que entregue #43; coordinar un único propietario del teléfono y el único slot Gradle. La base firmada #34 está instalada y preservada externamente. Antes de reemplazarla, guardar un escenario sintético identificable y comprobar persistencia por UI; actualización con misma firma y datos conservados, sin desinstalar ni limpiar almacenamiento. El candidato debe incorporar las entregas pertinentes ya revisadas y declarar su fuente/hash real.
- Alcance: persistencia, importación/exportación, portapapeles, CSV/Arrow, offline y actualización real. Nada de inferencia simulada ni apagado global de red sin restauración; preferir restricción de red del proceso o entorno verificable y documentar cualquier impedimento. No atribuir host/JVM a interacción APK.

## Perfil Android integrado — #43

- Código integrado en `1e661d6ce7e9b185a4990728fb0fc85997b84c09` tras revisión y seguimiento del mismo revisor: Standards0/Spec0. Se corrigieron la restauración tras mutación parcial, FAIL frente a omisión, mismatch de base APK con splits y la confirmación explícita de estado guardado antes de reiniciar.21selfchecks Node pasan también en integración.
- #43 permanece abierta por aceptación física: siete recetas ejecutables todavía NOT_RUN sobre el próximo APK conjunto. El código invocable ya permite continuar #44; las corridas base34FAIL siguen conservadas. `--saved-state-confirmed` es una atestación del invocador, no crea respaldo ni guarda automáticamente.
- #44 inicia desde esta integración; debe preparar un escenario sintético guardado antes de actualizar la base34. Se registró además la ruta Android que sustituía Arrow por CSV en #44#issuecomment-6310188117; la verificación exige IPC real y lectura independiente de bytes, junto a la columna19Moneda.

## Política vigente — un subagente a la vez

- 2026-10-21: el propietario ordenó ejecución completamente secuencial con un único subagente activo. Sustituye las indicaciones anteriores de concurrencia. Se mantiene Luna/max y los dos roles por ticket, también secuenciales: implementación, revisión, corrección y seguimiento del mismo revisor antes de abrir el siguiente trabajo.
- Único subagente activo: #32 implementador, terminando validación de cancelación nativa y reintentos de descarga. #40 fue interrumpido con sus archivos y evidencia preservados; los demás esperan. #43 ya está revisado e integrado, con aceptación física pendiente. El worktree #44 se creó desde0fcd8fb pero no tiene agente iniciado ni cambios.
- Al retomar #40: fallo real de accesibilidad tras popup de Compose1.9.3; la pantalla cambia pero el árbol mantiene las opciones cerradas y Tab no lo recupera, sin excepciones. Diagnóstico externo `targeted-issue40-20261021-scroll-repair/compose-a11y-popup-diagnostic.json`. Resolver un control accesible mínimo y verificarlo antes de completar recorridos; no sustituirlo por cobertura ficticia ni alterar DOM para aparentar accesibilidad.

## Estado vigente tras revisión e integración de #32

- #32 código aprobado e integrado/publicado en `6cf9f4a27a6007738bda09565b9027fda73bb0ef`; Standards1P3 heurístico, Spec0. JVM144/host150 (144 comunes más6 específicas), APKdebug y sharedWasm aprobados. [Revisión e integración](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6320332425).
- Release firmado de esa fuente construido e instalado con reemplazo en Samsung. #34 cerrada tras completar actualización con escenario distintivo preservado y comparación JSON completa; [prueba física](https://github.com/jovillarrealm/finsim/issues/34#issuecomment-6320519464). #44 conserva el resto del alcance abierto.
- Ningún subagente activo en este momento. Coordinación ejecuta la prueba física32 con el fixture sintético existente antes de reactivar40/41. Descarga de modelo iniciada; inferencia/memoria/latencias pendientes. Los bloqueos web40 y los restantes tickets mantienen su estado anterior. Código todavía fuera de main, sin despliegue.

## Corrida física #32: fallo y corrección en curso

La descarga real terminó, se seleccionó el fixture sintético con texto pegado vacío y se intentó la inferencia. El APK release abortó en `nativeCreateConversation` por `NoSuchMethodError SamplerConfig.getTopK()I`; Android confirma crash nativo. Sin campos extraídos ni latencias/pico PSS completos. La corrida es FAIL, no NOT_RUN; [evidencia canónica](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6320720537). Se observaron además etiquetas comprimidas con fuente150%, conservada sin reducirla.

El mismo implementador #32 volvió a trabajar como único subagente activo, Luna/max, para diagnosticar R8/JNI y corregir el layout mínimo del flujo. Después retomará el mismo revisor. El modelo descargado y el escenario guardado permanecen; no desinstalar, borrar datos ni repetir descarga. #40 y los demás siguen esperando. [Estado publicado en #37](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6320732829).

## Repetición física #32 tras corregir R8

Revisión del arreglo: Standards0/Spec0 defectos de código. Integrado/publicado en `7ef7e3e914e897b73da5ca815f4b0bd089245eaf`; APK firmado revisado SHA256 `bc32c28885b60bb9a71c4cd7f987322a7f0d8ecc6168f9634b4ffc5b3cef73bb` instalado conservando datos/modelo. El cierre JNI desapareció y las etiquetas de ingesta150% son legibles.

La extracción sigue FAIL en el fixture sintético y en la página1 del PDF real reciente del corpus privado, ambos con texto pegado vacío y sin correcciones. La app informa que no hubo campos utilizables; faltan datos diagnósticos para localizar la causa entre generación/parseo/imagen. Ninguna extracción ni pico/latencia aprobados; resto de páginas no ejecutadas. [Informe canónico](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6322064833).

Único subagente activo: mismo implementador32 Luna/max, investigando causalmente antes de ajustar prompt. Se retoman los mismos roles, sin nuevos agentes internos. #34 conserva cierre por actualización/datos; #40 y los demás esperan. Datos privados fuera de GitHub, código fuera de main.

## Diagnóstico físico de generación #32 y documento correcto

El propietario precisó que el PDF privado objetivo es el extracto hipotecario reciente, no el otro documento del corpus utilizado en la corrida anterior. Su identidad y ubicación se conservan solo en la continuidad privada. El extracto hipotecario sigue sin probarse; la prueba privada anterior no acredita ese documento.

El APK temporal de diagnóstico `a909ec77cabe67015e862dcdcf0abb30e2cf646c` (SHA256 `e991acd811984f1b727c91e2621210083fe4dde2850e0143abd3fffc79c87cd7`) se instaló conservando escenario/modelo. Con el PDF sintético verificado, Colombia y texto pegado vacío, la respuesta real tuvo2011caracteres: JSON truncado, valores nulos y claves repetitivas ajenas al contrato. El callback notificó512fragmentos con presupuesto512tokens; no se equiparan ambos contadores como regla general. Extracción **FAIL**. Esto acredita un problema en la salida generada, pero todavía no su causa raíz. Inicialización856ms, inferencia51868ms, total61411ms y pico PSS muestreado2420939KiB describen esta corrida fallida. [Evidencia canónica](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6325884164).

Único subagente activo: implementador de reemplazo32, Luna/max, con la respuesta sintética real para diagnóstico causal. El implementador anterior fue interrumpido a solicitud del propietario. Debe retirar toda captura temporal y su gate antes del APK final y de cualquier prueba privada; después retoma el mismo revisor independiente. El APK diagnóstico no está integrado ni acredita aceptación. No se borró Logcat, almacenamiento ni modelo. #32 permanece abierta; #40 y los demás esperan.

## Repetición física con contrato cerrado #32

Candidato local `bba4bdb0f83a257f04bf1f71d5bd246a44cdadb6`, APK SHA256 `773d20c0fc7ed552b0a5bcb0472cccb9834a346ac32753d96f0cc3b0dbb34fdc`, instalado y verificado conservando escenario/modelo. Toda captura temporal de contenido fue retirada. Mismo fixture sintético, Colombia, texto pegado vacío y página1: **FAIL de extracción**. Ahora devuelve JSON válido de231caracteres,79fragmentos y0claves desconocidas, pero ninguno de los11campos reconocidos tiene valor utilizable. El cierre de contrato elimina la salida repetitiva/truncada, sin resolver extracción.

CPU: inicialización639ms, inferencia12631ms, total18612ms y pico PSS muestreado2619242KiB. [Informe físico](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6326290835). No se probó el PDF privado ni se aumentó el presupuesto de salida. Mismo implementador de reemplazo Luna/max reactivado como único subagente para investigar imagen/instrucciones/runtime; luego mismo revisor. Candidato todavía sin integrar; #32 abierta y siguientes tickets esperando.


## Repetición física con imagen antes del texto #32

Fuente `391e158c23a71db050db4745c56f6770266cfdea`, APK instalado/verificado SHA256 `ad25a6ac0caae46a0f9ca06600f0de04e6ebe868dac6108fd26d68161e080945`. Mismo sintético/página1/Colombia/texto vacío: **FAIL**, JSON válido254caracteres,87fragmentos,12claves conocidas vacías y0desconocidas. Cambiar solo orden de imagen/texto no resuelve lectura. CPU inicialización610ms, inferencia13317ms, total19886ms, pico PSS2518965KiB. [Informe físico](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6326511573).

Mismo reemplazo Luna/max reactivado, único subagente, para inspeccionar raster Android y entrega multimodal; fondo/transparencia siguen como hipótesis sin confirmar. Sin prueba privada, raw logging ni integración de candidatos; #32 permanece abierta. Modelo/escenario preservados y siguiente revisión con el mismo revisor pendiente.


## Cuarta corrida #32 y revisión del avance seguro

APK de fondo blanco fuente `07f7079fccc848e97b2d8029ee627abf60d7691a`, SHA256 `84ee6aedfcad95522cb6032c29da3106cd6ac532c2ad9d89d17daac5ef2c3a6e`, instalado/verificado con datos/modelo preservados. Mismo sintético/página1/Colombia/texto vacío: **FAIL**, JSON válido222caracteres,73fragmentos,10claves conocidas vacías y0desconocidas. Inicialización623ms, inferencia11251ms, total18005ms, pico PSS2546642KiB. Fondo blanco cambia imagen139859→230302bytes sin resolver lectura.

[Resumen anterior a revisión](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6326703328): tres pruebas estáticas dirigidas y builds release aprobados; ninguna aceptación de extracción/rendimiento ni prueba del PDF hipotecario correcto. El implementador de reemplazo finalizó en `851b141fbda4acbe355e01465863cf46ed343c71`. El mismo revisor32 Luna/max es ahora el único subagente activo, evaluando desde `0ef0c4e78116250a1cd1e62da6f4a28a9fcfb702`, Standards y Spec secuenciales. Captura temporal totalmente retirada, diagnóstico seguro y tres ajustes mínimos aún sin integrar. Se mantiene #32 abierta/FAIL; avanzar después con tickets independientes sin encadenar ajustes del modelo sin causa demostrada.

## Investigación adicional y aceptación semántica #32

A solicitud explícita del propietario se ejecutó un investigador adicional Luna/max después del revisor, sin solaparse. No confirmó causa raíz. Informe externo `issue32-independent-investigation.md`: separar legibilidad de imagen de interpretación del esquema mediante un control literal y extracción estructurada de la misma imagen; presupuesto visual insuficiente sigue como hipótesis no medida. No se realizaron builds ni acciones de teléfono en esa investigación.

El propietario plantea que extraer algo no garantiza correspondencia, con el ejemplo de cuotas con y sin seguros. [Criterios de #32 actualizados](https://github.com/jovillarrealm/finsim/issues/32): verificar valor/unidad/significado por campo, vincular etiqueta/página, distinguir datos impresos y derivados, y separar interpretación y corrección manual. Los campos base/total ya existentes se reutilizan. `hasRecognizedValues` y confianza por presencia no acreditan exactitud. [Aclaración canónica en #37](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6327024977). #45 conserva su cierre por alcance controlado; #47 evalúa ambos casos privados y la referencia mantiene revisión humana pendiente.

El revisor32 encontró P2 en la ficha que aún presenta como pendiente el FAIL físico del84EE, y P3 en etapa de rechazo de memoria posterior a inicializar. Mismo implementador de reemplazo reactivado como único subagente para corregirlos y preparar un diagnóstico sintético acotado de lectura literal frente al contrato estructurado. Sin cambios de presupuesto visual/prompts productivos ni pruebas privadas en esa fase; mantener FAIL histórico y retirar toda instrumentación temporal antes de integrar. Código todavía sin integrar desde la última revisión, #32 abierta.


## Prueba discriminante #32: lee cuota, salida estructurada vacía

APK1989AAFB…DB47 instalado/verificado preservando escenario/modelo; fuentes de build con cambios locales verificadas y checkpoint posterior `2a7d59e20baf3d9d3796237df88a489a3116e6b9`. Mismo sintético/página1/Colombia/texto vacío y mismo PNG real SHA256 `86b4b0ec671903988818aedd199a9cbe4cce96cde62f18ff57756e1e7ad6770c`,1240×1753,230302bytes. Control literal **MATCH** de etiqueta/importe/moneda en6201ms; extracción estructurada **FAIL**, JSON válido222caracteres73fragmentos sin datos utilizables,10672ms. Total del par23381ms/pico PSS2529811KiB no acreditan rendimiento general ni lectura completa. [Evidencia](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6327557722).

Único subagente: investigador adicional reactivado para comprobar si el esquema llega al prompt del modelo o solo al decodificador, sin nuevos builds/ajustes. Causa raíz aún no confirmada. Sonda temporal no integrada ni apta para PDF privado; retirarla antes de candidato final. Prueba dirigida4/4 y release aprobado; corrida completa previa **FAIL**157casos/1aserción en test de reintentos. Root identificó posible carrera en ese test (latch antes del progreso legítimo del reintento), corrección todavía pendiente. #32 abierta con criterios semánticos vigentes.


## Candidato final sin sonda y revisión — reanudación 2026-10-21

La spec #37 en GitHub ya muestra estado aceptado/en implementación y dos PDFs, con enlaces a las decisiones vigentes de disponibilidad móvil, Moneda19, Luna/max secuencial y aceptación semántica. El texto histórico conserva su procedencia; no se amplía el alcance financiero.

Implementador de reemplazo32 finalizó en `b78288341c6171f4ba1b3dafad9c49fa13ec8c30`; código de build `d5b7a24ac19b08f8ff7d5092242173dca3618bbd`, árbol KMP `7c13f05d91d2b672b0e024ff3234da1dc7fbcdb8`. Sonda/helper/tests/hooks retirados. Se conserva guía de campos; el runtime indica `candidates_available_unverified`. La revisión apila cabecera/metadatos y muestra «Datos por revisar» en vez de confianza heurística85%.

Host153/153, release firmado y firma aprobados. APK SHA256 `e0d2540961419c3d0141755b1623729a85b7ea8bf2ebf792f7d764ab88686e61`; físico/visual150% de este binario todavía pendiente. La corrida previa9055 sigue **FAIL semántico** por cuotas, tasa, plazos y componentes omitidos; no se atribuye a este build ni se transforma en aprobación. [Resumen previo a revisión](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6330422386).

Único subagente activo: mismo revisor32 Luna/max, revisando desde su último punto851b141. Distinguir defectos del delta y requisitos funcionales aún incumplidos; código integrable no implica spec completada. Después coordinación comprobará el APK exacto conservando datos/modelo y avanzará con entregas independientes. #32 sigue abierta; PDF hipotecario correcto todavía sin prueba.

## Seguimiento de revisión: conservación de candidatos ambiguos

El mismo revisor terminó sobre `b782883` desde base fija `851b141`: Standards0, Spec2P2. El prompt contradice el contrato de ambigüedad al pedir null por significado incierto; además, la compuerta preexistente omite `unclassifiedPrincipal` y puede impedir que el único candidato llegue a revisión. [Hallazgos publicados](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6330542216).

Único subagente activo: implementador de reemplazo32 Luna/max, corrigiendo ambos puntos con alcance mínimo y sin nuevos ajustes del modelo. Después retoma el mismo revisor. E0 sigue sin corrida física;9055 conserva FAIL semántico; #32 abierta. #40 permanece preservada y puede avanzar independientemente tras esta ronda. No se integró el candidato ni se probó el PDF hipotecario.

## Correcciones revisadas e integración parcial de #32

Seguimiento independiente desdeb782883 sobred8eb37f: ambosP2 resueltos, Standards0/Spec0 nuevos. Candidatos ambiguos preservados y compuerta corregida; importación aún requiere confirmación explícita y acción. Código5d6355f, host156/15626XML0fallos/errores/omitidas y release firmado PASS, hash APK `3bd94de4d006b76209291f8e86d4ef3df8b8ff12d6eafbc7232034e5ee5e3d3a`. Integrado/publicado en `98a552916a40a56adab592cd23c4b3ba55de1269`; árbol KMP `504dc179ba3cc9b188c5f72f02d22f4206aecc71` idéntico al build. No se repitió compilación sobre un árbol idéntico.

APK exacto instalado con reemplazo y hash instalado verificado, escenario/modelo preservados. Sintético/página1/Colombia/texto vacío: **FAIL semántico**; cuota base toma el total con seguro y tasa mensual aparece anual, además de omisiones. Cabecera/metadatos150% legibles y «Datos por revisar» sin85% heurístico. Sin ediciones/aplicación. [Resultado sintético](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6330763211).

Primera prueba del caso hipotecario privado correcto en ese APK: **FAIL end-to-end** por importes/magnitudes/categorías y omisiones. No se atribuye causa exacta al modelo o parser sin respuesta cruda; la posible confusión de valor asegurado/capital es hipótesis, no coincidencia exacta. Se conservan identidad y valores fuera de GitHub. Referencia privada actualizada con la ejecución, manteniendo revisión humana pendiente. Sin correcciones/confirmación/aplicación; escenario preservado. [Resultado redactado](https://github.com/jovillarrealm/finsim/issues/32#issuecomment-6330823501), [seguimiento47](https://github.com/jovillarrealm/finsim/issues/47#issuecomment-6330823870). Página2 y segundo documento con este APK NOT_RUN; perfil invocable47 pendiente. #32 permanece abierta, sin rendimiento/fidelidad aprobados.

Siguiente tarea independiente: reanudar #40 en su worktree preservado con un único implementador Luna/max, después revisor por turno. No más ajustes del modelo en esta fase. Código aún fuera de main, sin despliegue.

Reanudación40: `/root/issue40_resume_luna`, único subagente activo Luna/max. Checkpoint previo4489483 preserva12paths heredados; merge de670a95b en1c62c6e conserva comandos web/dispositivo. Investiga el popup de accesibilidad antes de nuevos recorridos. La ficha32 incluye ya el resultado de coordinación físico/sintético/privado y sus límites.

## #40 — perfil completo fallido, corrección dirigida y revisión independiente

La implementación queda en `549638b1d027d6f47cb6f3061364e574238ced84`, rama `codex/spec37-issue40`, worktree limpio. El perfil web oficial construido desde `ab68e2d` conserva **FAIL**: `npm test` 21/23 y `test:e2e` 17/19. Las 13 pruebas heredadas pasaron en ambas suites. La distribución de ocho archivos, SHA-256 `781f439ea25a33a6a39b88d3392c7b0362e3176e50cdfba4db15a87d5916fc30`, coincidió byte a byte con lo servido.

Las capturas posteriores del mismo Wasm confirmaron que los dos fallos ocurrían al buscar celdas de M2 fuera del viewport vertical mientras el helper seguía desplazando columnas. La corrección compartida usa gestos reales y anclas visibles para preparar ambos ejes. La prueba dirigida posterior pasó 2/2: configuración 125,347 s y eventos 235,832 s; este último conserva un margen estrecho de 4,168 s respecto de su límite. El perfil completo no se repitió después de la corrección y su FAIL sigue vigente.

[Resumen previo a revisión](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6333104639). Único subagente activo: `/root/issue40_review`, Luna/max, Standards y Spec secuenciales sobre el delta desde `475ce87` (merge-base `670a95b`), incluyendo todo el formulario, dominio y verificador heredados. Revisará también la reutilización de builds basada en fechas. Coordinación ejecuta las pruebas host actuales, sin tocar fuentes. Android físico de este cambio, lector de pantalla, navegación exclusivamente por teclado y rasterización tabular permanecen sin acreditar. #40 sigue abierto y no integrado; #32 mantiene sus resultados semánticos FAIL.

### Resultado de revisión y pruebas host de #40

El revisor terminó sobre `549638b`: Standards, cero infracciones documentadas y una heurística P3 por overload de tres controles sin uso; Spec, tres P2 por identidad de builds reutilizados basada en fechas, seguros fijos con más de dos decimales aceptados y dos casos con ID ausentes del catálogo. [Informe publicado](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6333276784). Único subagente activo ahora: el mismo implementador `/root/issue40_resume_luna`, corrigiendo esos hallazgos; después retoma el mismo revisor. No se repetirá el perfil completo hasta comprobar las correcciones.

Coordinación verificó `549638b` limpio: JVM compartido 152/152, host de la aplicación 5/5 y host Android compartido 161/161, todos sin fallos, errores ni omisiones. Este último vuelve a ejecutar 152 casos comunes y agrega nueve específicos de Android; no se suman como casos únicos entre destinos. [Evidencia publicada](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6333228174). La ejecución host supera el NOT_RUN de la ficha al entregar; el perfil web oficial conserva FAIL y no hay aceptación física del nuevo cambio.

### Reanudación de estabilidad de #40 — 2026-10-22

Las correcciones quedaron revisadas en `04809ea`; coordinación confirmó JVM 153/153, host Android compartido 162/162 y host de aplicación 5/5. El perfil de ese commit compiló Wasm pero falló en el manifiesto por falta de `readFile`, sin ejecutar navegador. Coordinación restauró la importación y añadió una regresión de bytes reales con fecha conservada; self-checks 5/5, seguimiento independiente sin nuevos hallazgos, commit `bf7856a`.

El perfil completo posterior `2026-10-22T05-43-43-423Z-29983f3f` terminó **FAIL**: suite general 22/24 y E2E 16/19. Build y ocho archivos HTTP PASS, distribución `080eabde63bf4a0c1b682829aa57875deace87411cafe8de03aa40ea11f5efb3`. Fallaron clic inicial de Ofertas, acceso a una celda de eventos en una suite y, en E2E, espera de archivo rechazada al cerrar y discrepancia de portapapeles. No se atribuyen las operaciones JSON ni el cálculo como causa sin evidencia. El diagnóstico posterior de eventos, mismo artefacto comprobado, pasó en 222,120 s; no sustituye el FAIL completo.

[Estado canónico y relevo](https://github.com/jovillarrealm/finsim/issues/40#issuecomment-6356495921). Único implementador activo: `/root/issue40_stability_repair`, Luna/max, en el mismo worktree y rama de #40 desde `bf7856a`. Debe conservar expectativas, corregir observación/gestos con geometría real y entregar evidencia dirigida antes de nueva revisión independiente por turno. La asignación continúa la reserva existente. Ningún código de #40 se integró todavía; no hubo nuevas acciones de móvil ni cambios a los resultados semánticos FAIL de #32.
