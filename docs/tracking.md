# Tracking de implementación

## Estado actual

Actualizado: 2026-09-21. Edición de meses por lotes integrada y publicada en **bc535a2**. Sesión coordinadora: /root. Verificación técnica completada: 45 pruebas unitarias, 4 flujos de navegador y build con comprobación de tipos. Publicado: https://jovillarrealm.github.io/finsim/. Pendiente: aceptación de uso FS-07/FS-10.

Este tablero es la fuente de estado integrado. El [plan](implementation-plan.md) define alcance, dependencias y criterios de cierre. Los registros por tarea guardan el detalle de continuidad.

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

## FS-18 — Copia de resultados y gráfico unificado del préstamo

2026-10-14 · en curso (diseño, sin implementación) · coordinadora/ejecutora /root · reserva `task/fs-18-loan-experience` · C:/Users/Lenovo/Documents/finsim-fs18. Solicitud: campos copiables, composición intereses/capital en curva de saldo, controles para limpiar la vista y retirada condicionada de doble pista. Entrevista grill-with-docs antes de acordar experiencia. Documentos desde main; referencia de UI actual en FS-17 `f49a64f`. La implementación depende de coordinar/incorporar KMP/FS-17 sin merges entre ramas hermanas. No altera aceptación ni reserva de FS-17.

2026-10-14 · en revisión (entrega documental) · /root · usuario confirma cierre Q10 y solicita mattpocock-skills:to-spec. Especificación canónica publicada en tracker Markdown local: `finsim-fs18/.scratch/loan-results-experience/spec.md`, commit `a6a3909`, Status/Triage Label `ready-for-agent`. Ambos gráficos serán líneas original/modificado; se descartan barras y se retira doble pista al verificar el reemplazo. Incluye copia, filtros, navegación mensual, 34 historias, criterios A01–A13 y referencias F01–F06. Documento/enlaces y 17 igualdades en centavos verificados. Sin implementación, suites nuevas de producto, integración ni publicación de app. Reserva conservada; siguiente paso coordinar incorporación de KMP/FS-17 antes de implementar o descomponer en tickets. La etiqueta de spec no elimina dependencias ni cierra pendientes de aceptación anteriores.
