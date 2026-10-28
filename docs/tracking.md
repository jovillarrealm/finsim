# Tracking de implementación

## Coordinación activa — Spec #37

2026-10-20 · /root. El propietario autoriza publicar e implementar el desglose de [#37](https://github.com/jovillarrealm/finsim/issues/37), con dos subagentes secuenciales por ticket (implementación y revisión). Rama `codex/spec37-verification`, base publicada `839c69c`, worktree `C:/Users/Lenovo/.codex/worktrees/spec37-verification/finsim`. GitHub es la fuente de verdad para alcance, decisiones, tickets y dependencias; el estado anterior de este documento es histórico. Continuidad auxiliar en [SPEC37](tasks/SPEC37.md). Las corridas móviles sin hardware pueden quedar pendientes documentadas sin bloquear desarrollo/revisión.

## Estado actual

Continuidad 2026-10-27: #33 publicada hasta `cc474c9`; perfil de verificación corregido y evidencia [en GitHub](https://github.com/jovillarrealm/finsim/issues/33#issuecomment-6460336281). WebGPU quedó sin respuesta en carga (240 s); WASM inició generación, sin tokens en 180 s. Sin inferencia semántica acreditada, #33 sigue abierta y #46 pendiente. No integrado en main.

Continuidad 2026-10-27: #33 conserva avance y correcciones revisadas en `codex/spec37-issue33` publicada, punta `9bb3f32`, sin integrar. `npm run check`, build Wasm y prueba de recuperación en Chrome pasaron; inferencia real se detuvo durante carga WebGPU, PNG/JPEG no verificados y perfil completo pendiente. #33 sigue abierta; [evidencia y límites](https://github.com/jovillarrealm/finsim/issues/33#issuecomment-6459776054). #46 espera resultados reales de #33.

Continuidad 2026-10-27: el propietario prioriza Docling web. Se reserva #33 para `/root/issue33_docling_luna`, único subagente Luna/max, seguido por revisión independiente; #46 continúa después de #33. El perfil web de #40/#41 terminó PASS (34/34 y 26/26), pero #41 conserva cambios sin commit/revisión en su worktree y una posible lectura obsoleta del portapapeles; permanece abierta. Detalle en [SPEC37](tasks/SPEC37.md) y GitHub.

Continuidad 2026-10-22: #40 incorporada como avance parcial revisado en `cce6d20`, con perfil completo y prueba física pendientes; sigue abierta. Reserva vigente #41 para `/root/issue41_resume_luna`, único implementador Luna/max y después revisor por turno. Retoma `codex/spec37-issue41` desde el checkpoint preservado `0588874`, incorporando por merge la punta publicada de integración antes de editar. Detalle/evidencia actuales en [SPEC37](tasks/SPEC37.md) y GitHub.

Actualizado: 2026-10-18.

> [!CAUTION] Error Arquitectónico Grave Registrado (2026-10-18) — [ADR 0002](adr/0002-dual-web-stack-divergence-incident.md)
> Se incurrió en un error grave de divergencia de pila: durante la implementación del refresco visual de Stitch, el agente continuó modificando y validando la aplicación web de React en `src/` (servida con Vite en el puerto 5173) en lugar de concentrarse exclusivamente en el codebase unificado de Compose Multiplatform WasmJs (`kmp/composeApp`).
> Esto generó una webapp en React completamente paralela y separada del codebase KMP, duplicando componentes (`ScenarioComparison.tsx`, `LoanSummary.tsx`, `MonthlyTable.tsx`) frente a sus equivalentes en Kotlin (`HorizontalComparisonChart.kt`, `LoanSummary.kt`, `MonthlyTable.kt`).
> El único objetivo Web oficial en producción es `kmp/composeApp` compilado a `wasmJs` (`:composeApp:wasmJsBrowserDistribution`), desplegado en GitHub Pages. Queda prohibido el desarrollo en `src/` y se exige que toda prueba de navegador futura se ejecute contra el artefacto WasmJs de KMP.

Este tablero es la fuente de estado integrado. El [plan](implementation-plan.md) define alcance, dependencias y criterios de cierre. Los registros por tarea guardan el detalle de continuidad.

Prioridad confirmada por el usuario el 2026-10-17: **FS-18 primero**. Los pendientes anteriores de lector real, comprensión financiera de audiencia y deuda/verificación KMP se retoman después de FS-18. Permanecen abiertos; no bloquean iniciar FS-18 ni se consideran completados por este aplazamiento.

| ID | Entrega | Estado | Responsable / sesión | Rama / worktree | Evidencia integrada |
| --- | --- | --- | --- | --- | --- |
| FS-00 | Repositorio y aplicación | hecha | /root | main / finsim | 26862de; build, pruebas y navegador |
| FS-01 | Contratos y validación | hecha | /root | main / finsim | 2b79d08; tipos y contratos |
| FS-02 | Motor de tasas | hecha | /root/rates | task/fs-02-rates / finsim-fs02 | 01dbbc8 integrado; 9 pruebas y build conjunto |
| FS-03 | Préstamo base y seguros | hecha | /root/loan_engine | task/fs-03-loan / finsim-fs03 | 911885c; 28 pruebas y build conjunto |
| FS-04 | Primera interfaz | hecha | /root | main / finsim | 1167e15; escritorio, móvil y teclado verificados |
| FS-05 | Eventos y atrasos | hecha | /root/converter | task/fs-05-events / finsim-fs05 | 44f9330 integrado en 4d7b7ec; pruebas financieras |
| FS-06 | Persistencia | hecha | /root/persistence | task/fs-06-persistence / finsim-fs06 | a04b510 integrado en 084ee1d; 9 pruebas y build |
| FS-07 | Comparación y edición | en revisión | /root | task/fs-07-comparison / finsim-fs07 | 2099e69 integrado; interacción verificada; falta confirmación de uso |
| FS-08 | Conciliación | hecha | /root | task/fs-08-reconciliation / finsim-fs08 | referencia literal, navegación y diferencia visible verificadas |
| FS-09 | Conversor visual | hecha | /root | task/fs-09-converter / finsim-fs09 | referencia 1% EM, navegación y teclado verificados |
| FS-10 | Aceptación integrada | en revisión | /root | main / finsim | 42 pruebas, 2 flujos navegador; falta observación del criterio 8 |
| FS-11 | GitHub Pages | hecha | /root | main / finsim | run 35532310565 exitoso; 3 flujos comprobados sobre URL pública |
| FS-12 | CDT/ETF | diferida | — | — | — |
| FS-14 | Edición de meses por lotes | hecha | /root + /root/batch_events | codex/fs-14-batch-events / finsim-fs14 | bc535a2 publicado; 4 flujos verificados en URL pública |
| FS-13 | Moneda del escenario | hecha | /root + /root/currency_ui | codex/fs-13-currency / finsim-fs13 | 8cba454 integrado; 45 pruebas, 3 flujos navegador y build |
| KMP T01–T10 | Migración Kotlin Multiplatform (Web & Android) | en revisión | /root | feat/kmp-migration | 10 tickets completados; motor decimal, paridad y persistencia verificados; APK y web preparados; adb MCP y Android CLI activos |
| FS-15 | Ingesta de extractos en edge (Pipeline Híbrido) | en revisión | /root | task/fs-15-ingestion | 389ec56; Tickets 01–04 completados; 83 pruebas unitarias jvmTest, build Android y WasmJs exitosos |
| FS-16 | Rediseño y Ergonomía Móvil (Mobile Layout Revamp) | en revisión | /root | feat/kmp-migration | Integrado técnicamente en main 542c9f2; tickets 01–05 y verificaciones anteriores conservados; usuario reporta mejora de layout confirmada por testers el 2026-10-17 |
| FS-17 | Comparador de ofertas de financiación | en revisión | /root | task/fs-17-offer-comparison / finsim-fs17 | Integración 542c9f2; publicación para audiencia a22f0ff/run 38777302454; 115 shared, 7 UI, 9 navegador CI y 2 sobre URL pública; lector/audiencia pendientes |
| FS-18 | Resultados copiables y gráficos de préstamo | hecha | /root | task/fs-18-implementation / finsim-fs18-implementation | Integrada en main 7da1f71; desplegada en GitHub Pages c341542/run 39088886664; 115 JVM tests, 13/13 Playwright E2E tests, CSV/Arrow downloads, tarjeta de inspección sincronizada con 6 números (capital, intereses, seguros en modo original y modificado), estética Esmeralda y Menta Financiera |
| Stitch T06–T10 | Adopción visual y ergonomía Stitch (Mobile Nav, Hero Ahorro, Trayectoria con Extinción, Torres y Cronograma de Hitos) | hecha | /root | main | Integrada en main; 54 vitest tests pasan, typecheck limpio, build ok, 115 JVM tests y tests Compose/Android exitosos; tickets T06-T10 completados |


## Estados

- `pendiente`: tiene dependencias sin integrar.
- `lista`: dependencias integradas; puede reservarse.
- `en curso`: reservada por una sesión identificada, con rama y worktree propios.
- `pausada`: conserva su reserva y un siguiente paso registrado para otra sesión.
- `bloqueada`: registra impedimento concreto, quién puede resolverlo y condición de salida.
- `en revisión`: implementación con evidencia; falta revisión, integración o aceptación de uso expresamente registrada.
- `hecha`: integrada en `main`, criterios satisfechos y comprobaciones del conjunto exitosas.
- `diferida`: fuera del hito activo.

## Protocolo entre sesiones y worktrees

### Reserva

1. La primera sesión de implementación se identifica como coordinadora en este archivo. Solo esa sesión edita el tablero en el checkout de integración. Para sustituirla, cerrar su actividad y registrar el relevo; dos coordinadores simultáneos no están permitidos.
2. FS-00 se ejecuta en el directorio actual porque todavía no existe Git. Al cerrarla, registrar commit base y comprobaciones antes de abrir worktrees.
3. Para cada tarea lista, la coordinadora comprueba el estado actual de `main`, las ramas y `git worktree list`. Registra responsable, rama `task/fs-NN-descripcion`, ruta absoluta y estado `en curso`; crea `docs/tasks/FS-NN.md` con la ficha siguiente y guarda la reserva en un commit de integración antes de crear el worktree.
4. Crear el worktree desde ese commit de `main` en una carpeta hermana, fuera del árbol del proyecto. Una rama y un worktree por tarea; no compartir el mismo checkout entre sesiones escritoras.
5. Entregar a la sesión ejecutora ID, rama, ruta y commit base. Leer una copia antigua del tablero no autoriza una reserva. Las asignaciones pasan por la única coordinadora; Git por sí solo no evita reservas simultáneas.

### Ejecución y pausa

1. La sesión ejecutora lee su ficha, las especificaciones aplicables y los límites de archivos. Comprueba que está en la rama y ruta asignadas.
2. Solo actualiza su ficha `docs/tasks/FS-NN.md`; el tablero y las fichas de otras tareas quedan bajo sus responsables. Crear la carpeta de fichas al reservar la primera tarea, sin generar fichas vacías para todo el backlog.
3. Antes de tocar un archivo compartido o ampliar alcance, solicitar coordinación y registrar la dependencia. Mantener las tareas pequeñas; dividir mediante IDs derivados, por ejemplo FS-05a, solo si aparece una necesidad concreta.
4. Antes de pausar o entregar, registrar cambios, comprobaciones y siguiente paso exacto. Guardar commits locales del trabajo propio cuando sea posible. Si queda trabajo sin commit, enumerar archivos y advertir que solo existen en ese worktree; no borrar el worktree.
5. Una sesión nueva retoma la misma rama/worktree y revisa estado y diff reales. No duplica la tarea ni asume que una sesión ausente ha liberado su reserva. La coordinadora confirma el relevo y actualiza responsable/estado.

### Revisión e integración

1. La ejecutora entrega commit, ficha y resultados. La coordinadora marca `en revisión` y comprueba criterios, diff y límites de responsabilidad.
2. Integrar una tarea por vez en `main`, con checkout limpio y dependencias ya integradas. Resolver conflictos preservando cambios ajenos; ejecutar tipos, pruebas pertinentes y build. Los comandos concretos se leen del proyecto creado por FS-00.
3. Si falla integración o falta evidencia, mantener la tarea abierta y registrar el siguiente paso. Solo marcar `hecha` con referencia al commit integrado y resultados; una prueba no ejecutada se registra como tal.
4. Actualizar tareas desbloqueadas a `lista` y comunicar cambios de contratos. Los worktrees consumidores incorporan `main` antes de continuar sobre esos cambios.
5. Retirar un worktree únicamente cuando esté limpio, su trabajo esté integrado y la ficha conserve el traspaso. Conservar cualquier trabajo pendiente.

## Ficha de tarea

Copiar en `docs/tasks/FS-NN.md` al reservar. El estado operativo de la ficha puede adelantarse al tablero mientras espera integración; el tablero lo publica la coordinadora.

```markdown
# FS-NN — Título

- Responsable / sesión:
- Estado operativo:
- Rama y ruta absoluta del worktree:
- Commit base de main:
- Última actualización:
- Alcance y criterios: enlace a la tarea en implementation-plan.md

## Traspaso actual

- Implementado:
- Archivos modificados:
- Comprobaciones: comando, resultado y fecha; indicar las no ejecutadas.
- Commit de entrega: registrar el hash del commit de código; la ficha puede ir en un commit posterior.
- Cambios sin commit:
- Bloqueos / decisiones pendientes:
- Siguiente paso exacto:

## Historial breve

- Fecha · sesión · cambio relevante o motivo de pausa.
```

## Inicio de una próxima sesión

Para continuar: «Retoma FS-10 desde main. Lee su ficha y registra la aceptación de uso pendiente. La implementación técnica ya está integrada; no reinicies FS-00».

Para retomar una tarea asignada: «Retoma FS-NN en su worktree registrado. Lee su ficha, verifica el estado real y continúa desde el siguiente paso. Actualiza el traspaso al terminar».

Para coordinar: «Revisa entregas en revisión, integra una por vez, verifica y actualiza el tablero. Reserva las siguientes tareas independientes antes de abrir nuevas sesiones».

## FS-13 · Moneda del escenario

2026-09-20 · hecha · /root · codex/fs-13-currency · worktree hermano finsim-fs13. COP, ARS, USD y EUR; sin conversión; compatibilidad con escenarios anteriores en COP.

## FS-14 · Edición de meses por lotes

2026-09-21 · hecha · /root · codex/fs-14-batch-events · worktree hermano finsim-fs14. Feedback: edición inmediata incómoda; agregar/modificar/quitar varios eventos antes de aplicar, con validación atómica y descarte.

## FS-15 · Gráficos verticales

2026-09-21 · hecha · /root · codex/fs-15-charts · worktree hermano finsim-fs15. Original encima de modificado, ambos a ancho completo.

## FS-17 — Comparador de ofertas de financiación

2026-10-14 · en revisión · coordinadora /root · task/fs-17-offer-comparison · C:/Users/Lenovo/Documents/finsim-fs17. Reserva vigente desde main ac7b763; dependencia KMP incorporada en la rama de tarea. Sin integración ni despliegue de FS-17; no se cierran KMP ni aceptación anterior.

Entrega en rama: f8551b4 corrige nombres accesibles y árbol Web bloqueado tras selección; 66a78a0 resuelve el bloqueo Gradle. Android/Web reconstruidos, 7 pruebas UI y 5 de navegador actuales pasan. Las 115 pruebas shared anteriores corresponden al mismo módulo sin cambios. Transferencia real JSON Web verificada mediante Playwright autorizado; DevTools conserva su restricción de rutas. APK instalado -r: dos cotizaciones y recuperación A1200/B1120 tras reinicio verificadas.

Pendientes: recorrido compacto completo y lector real; simulación/contexto/transferencia Android; documentos antiguos E2E y audiencia. La ventana amplia y los clicks force:true de las pruebas no acreditan todo el flujo móvil. La deuda anterior de KMP sigue abierta.

Continuidad vigente en el worktree asignado: docs/tasks/FS-17.md y docs/reviews/FS-17-browser-verification.md. La ficha y el handoff conservados en main antes de esta entrega describen un estado anterior; consultar la rama de tarea para retomar. Solo la coordinadora modifica este tablero.

2026-10-14 · /root · pendientes consolidados en [handoff vigente](C:/Users/Lenovo/AppData/Local/Temp/finsim-FS17-pendientes-handoff.md), que sustituye el handoff anterior de Chrome DevTools. Reserva y estado en revisión conservados; sin nuevas pruebas, integración ni publicación. El documento reside en TEMP; la ficha viva conserva el enlace y el siguiente paso.

2026-10-14 · /root · reanudación autorizada por el usuario desde handoff vigente. Misma reserva y worktree; diagnóstico de teclado/scroll Web 360x800 antes de cualquier corrección. Sin integración ni publicación.

2026-10-14 · /root · continuación entregada en revisión: código `49b13fe`, recorrido de teclado `46531a1`, ficha y evidencia `f49a64f`, en la misma rama/worktree. Foco Web recuperado al retirar el input nativo; importación/exportación real de préstamos restaurada con adaptadores existentes. Nueve pruebas de navegador verificadas (el último caso de entrada sin clic inicial se repitió de forma enfocada), siete UI y builds Android/Web correctos. Las 115 pruebas shared anteriores corresponden al módulo sin cambios; no se repitieron.

Android: transferencia real y rechazos no destructivos, F06/tabla/márgenes negativos, redondeo final, recuperación del contexto pendiente y documento histórico con abono comprobados. La simulación se importó; no acredita introducción manual completa de todos sus campos. El guardado final coincide exactamente con el respaldo inicial. Web compacto y recorrido representativo solo con teclado verificados; hardware Web y teclado virtual no comprobados.

Revisión independiente Standards/Spec: cero incumplimientos o defectos nuevos bloqueantes; una observación opcional de nombre conservada. Pendientes vigentes: lector real y observación de audiencia. La deuda KMP anterior sigue abierta. No se integra, publica ni marca FS-17/KMP como hecha. Continuidad vigente en `finsim-fs17/docs/tasks/FS-17.md`, `docs/reviews/FS-17-browser-verification.md` y `docs/reviews/FS-17-continuation-review.md`; estos registros sustituyen TEMP para retomar. Reserva /root conservada, estado en revisión.

2026-10-15 · /root · lectura del handoff y reanudación documental FS-17 en su reserva vigente, HEAD inicial f49a64f limpio. Bundle Web idéntico al verificado (SHA256 conservado en ficha); sin suites nuevas ni cambios de producto. No hay lector activo ni dispositivo adb conectado; Narrador está instalado pero no se ejecutó ni se observó lectura real. Se solicitó aclaración FS-17/FS-18 y disponibilidad de audiencia; sin aceptación inferida. Ficha viva actualizada, lector/audiencia pendientes y estado en revisión conservado. Sin integración, publicación ni implementación FS-18.

2026-10-15 · /root · usuario confirma primero la verificación FS-17. Ejecución nueva de cinco archivos browser-tests: **9/9 pasan**, 154,32 s, incluido el caso final solo con teclado en el mismo run. Bundle KMP idéntico al acreditado; sin producto nuevo ni repetición de shared/UI/builds. Computer Use rechazó automáticamente usar Narrador («Computer Use was not approved to use Narrador», sin motivo adicional); autorización explícita solicitada, sin lector ejecutado ni aceptación inferida. Audiencia pendiente. Evidencia/ficha actualizadas en rama asignada; FS-17/KMP siguen en revisión, sin integración/publicación ni implementación FS-18.

2026-10-15 · /root · el usuario autoriza nueva publicación para pruebas con audiencia. Ampliación coordinada FS-17: workflow Pages construirá KMP en lugar de React; firma Android solo se valida al construir Android firmado, sin claves en CI Web. Se prepara integración técnica en main y verificación del conjunto/URL pública. Publicación todavía no realizada; FS-17/KMP permanecen en revisión por lector, audiencia y deuda previa. Reserva FS-18 conservada, sin implementación.

2026-10-15 · /root · publicación para audiencia completada en **a22f0ff**, [run 38777302454](https://github.com/jovillarrealm/finsim/actions/runs/38777302454) exitoso. Main integra KMP/FS-17 en 542c9f2, conserva reservas y aceptación abierta. CI construye Web sin claves Android, valida shared/UI y **9/9** recorridos bajo /finsim/. Version.json público confirma SHA; **2/2** recorridos adicionales de archivos reales pasan sobre URL pública. Ofertas abre a 360x800 en emulación. No se acredita lector, audiencia, hardware Web/teclado virtual ni cierre de deuda KMP; FS-17/KMP siguen en revisión. FS-18 no implementado; dependencia de fuentes ya incorporada a main para coordinación posterior.

2026-10-17 · /root · usuario solicita otro AAB. Reserva FS-17 conservada; ampliación coordinada para versión Android 1.0.3/código 4 sobre main 7c908e6, bundleRelease y validación/firma comparada con el AAB anterior. Sin subida a Play Console ni nueva publicación Web; aceptación/deuda KMP siguen pendientes.

2026-10-17 · /root · compilación AAB detecta configuración local de firma apuntando a ruta ausente del worktree; se reutiliza el keystore existente mediante ruta absoluta en properties ignorado, sin copiar secretos. R8 incluido con AGP 8.7.3 emite errores de lectura de metadata Kotlin 2.2. Se coordina pin acotado de R8 8.10.21 en settings.gradle.kts conforme a documentación oficial, sin cambiar AGP/Kotlin ni dependencias de producto. Recompilar y validar antes de entregar.

2026-10-17 · /root · AAB **1.0.3/código 4** entregado en C:/Users/Lenovo/Documents/finsim/dist/android/finsim-1.0.3-4.aab (4.871.936 bytes), fuente **8fe3689** integrada en main. Build release correcto, R8 8.10.21 confirmado en mapping, bundletool validate y generación de APK de validación correctos; firma verificada y certificado idéntico al AAB anterior código 3. Hash completo y límites en ficha FS-17 (entrega 47bb0a1). Sin instalación en dispositivo, subida a Play Console ni nueva publicación Web; suites anteriores conservadas, aceptación/deuda abiertas. Propiedades de firma permanecen ignoradas y no se copian claves.

2026-10-17 · /root · usuario reporta que los testers coinciden en la mejora de los problemas de layout y solicita merge a main. Feedback positivo de layout registrado en FS-17/FS-16; no se infiere prueba de lector ni comprensión financiera sin guía. feat/kmp-migration y task/fs-17-offer-comparison ya están incluidos en main 974dae4; origin/main está seis commits detrás tras fetch, sin divergencia. Se sincronizan las entregas integradas y este registro con GitHub. Se conservan estados en revisión para los criterios restantes, evidencia técnica vigente y reserva FS-18; sin nueva publicación Pages ni subida a Play Console.

## FS-18 — Copia de resultados y gráfico unificado del préstamo
2026-10-17 · /root · implementación iniciada en task/fs-18-implementation, worktree C:/Users/Lenovo/Documents/finsim-fs18-implementation; commits 234651c y 6f1b326 desde reserva db94785. Gráficos sincronizados, filtros, copia Android/Wasm, detalle y tabla implementados sin cambios de motor/JSON. 115 shared + 3 presentación y builds Android/Web pasan; revisión de estándares sin violaciones y hallazgos de spec corregidos. Verificación final sigue abierta: run de producción termina con 11/13 (9 regresiones previas y 2 nuevos flujos); fallan selección de texto en navegación táctil y alcance final del resumen en flujo de borradores/teclado, ambos antes aprobados en desarrollo. Causa sin diagnosticar; emulador apagado para aislar interferencia. Android instala/abre, pero copia nativa no acreditada por ANR de System UI/emulador offline. Ficha FS-18 contiene siguiente paso y logs locales; usuario pide compactar para continuar. Estado en curso, sin integración, push ni publicación; deuda previa permanece después de FS-18.

2026-10-17 · /root · usuario solicita nueva rama y comenzar implementación. Reserva renovada: task/fs-18-implementation, C:/Users/Lenovo/Documents/finsim-fs18-implementation, desde main 9bd1cba más este commit de reserva. Especificación aprobada y redirect documental incorporados a main desde task/fs-18-loan-experience (6a2fd37); rama/worktree de diseño conservados. Alcance de archivos compartidos coordinado: UI/coordinación del préstamo, portapapeles por plataforma y pruebas; sin cambios al motor ni JSON. Criterios A01–A13/F01–F06; UI real y transformación decimal de presentación son las superficies de prueba acordadas. Reservar antes de crear worktree; solo /root modifica este tablero. Pendientes anteriores continúan después de FS-18.

2026-10-17 · /root · usuario pospone los pendientes anteriores hasta después de FS-18. FS-18 es la siguiente prioridad; reserva y especificación canónica en C:/Users/Lenovo/Documents/finsim-fs18/.scratch/loan-results-experience/spec.md conservadas. KMP/FS-17 ya están integrados en main b3d2752; al retomar, actualizar la rama FS-18 desde main y resolver la continuidad documental sin merges entre ramas hermanas. Esta priorización no inicia implementación ni cierra aceptación anterior.

2026-10-14 · en curso (diseño, sin implementación) · coordinadora/ejecutora /root · reserva `task/fs-18-loan-experience` · C:/Users/Lenovo/Documents/finsim-fs18. Solicitud: campos copiables, composición intereses/capital en curva de saldo, controles para limpiar la vista y retirada condicionada de doble pista. Entrevista grill-with-docs antes de acordar experiencia. Documentos desde main; referencia de UI actual en FS-17 `f49a64f`. La implementación depende de coordinar/incorporar KMP/FS-17 sin merges entre ramas hermanas. No altera aceptación ni reserva de FS-17.

2026-10-14 · en revisión (entrega documental) · /root · usuario confirma cierre Q10 y solicita mattpocock-skills:to-spec. Especificación canónica publicada en tracker Markdown local: `finsim-fs18/.scratch/loan-results-experience/spec.md`, commit `a6a3909`, Status/Triage Label `ready-for-agent`. Ambos gráficos serán líneas original/modificado; se descartan barras y se retira doble pista al verificar el reemplazo. Incluye copia, filtros, navegación mensual, 34 historias, criterios A01–A13 y referencias F01–F06. Documento/enlaces y 17 igualdades en centavos verificados. Sin implementación, suites nuevas de producto, integración ni publicación de app. Reserva conservada; siguiente paso coordinar incorporación de KMP/FS-17 antes de implementar o descomponer en tickets. La etiqueta de spec no elimina dependencias ni cierra pendientes de aceptación anteriores.
