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
