# FS-17 — Revisión de la continuación

2026-10-14. Base del handoff: `fe26198513a75d38c8fd2e50e3d4665a4d7a1d35`. HEAD revisado: `46531a1`; código de producto: `49b13fe`. Comando: `git diff fe26198...HEAD`. Dos revisores independientes, solo lectura. Tracker Markdown local acordado en la spec; no se instala otra configuración.

## Standards

Sin incumplimientos documentados ni bugs nuevos identificados.

- Respeta ADR 0001: reutiliza WorkspaceStore y adaptadores de archivo; no añade cálculos por plataforma ni modifica el motor TypeScript congelado.
- Conserva validación y manejo de errores de financial-conventions.md §6 e implementation-plan.md FS-06: validación antes de sustituir estado y rechazo que conserva datos. La protección de eventos pendientes se mantiene en App.kt:303.
- La ampliación a transferencia de préstamos está coordinada en la ficha FS-17. Ficha/evidencia sin commit se leyeron como contexto, fuera del diff de código.
- El arreglo de foco documenta límite y retirada con shortcut y tiene regresiones específicas.

Juicio menor, sin bloqueo: **Mysterious Name**. ComparisonFileActions en App.kt:299 y downloadComparison en ComparisonFileActions.wasm.kt:8 sirven ahora también préstamos. Un nombre neutral aclararía su propósito. Se conserva el nombre en esta entrega: no exige corrección funcional y Ponytail evita una edición adicional por una observación opcional.

## Spec

Sin defectos nuevos bloqueantes ni alcance no solicitado identificado.

- **Pendiente aceptado: lector real.** Spec:72 exige acceso con teclado/lector a controles, errores y detalle mensual. La prueba de teclado aporta evidencia; ticket 10:11 permanece abierto por lector real. Hardware Web y teclado virtual conservan sus límites.
- **Pendiente aceptado: audiencia.** Spec:237 exige una observación real de la explicación de aporte, cuota, duración, costo y faltantes. No hubo participante observado; ticket 10:14 continúa pendiente. Impide declarar aceptación completa.
- **Cobertura delimitada.** Spec:236 pide recorridos representativos Android/Web. La simulación Android importada, acciones UI, tabla, márgenes y recuperación respaldan el criterio técnico; no se afirma introducción manual completa de todos sus campos.
- **Compatibilidad dentro del alcance.** Spec:185 pide abrir documentos anteriores sin perder eventos. Reutilizar el selector JSON y comprobar Monedas, su abono, recálculo, exportación y rechazo responde al requisito. No cambia shared, cálculo ni stack.

Los revisores no ejecutaron nuevas suites ni reevaluaron la deuda KMP anterior. La evidencia ejecutada y sus límites se conserva en [el informe de verificación](FS-17-browser-verification.md).

Recuento: Standards 0 incumplimientos + 1 heurística menor de nombre; Spec 0 defectos nuevos + 2 pendientes aceptados (lector y audiencia). Sin integración ni publicación; FS-17/KMP conservan revisión y límites abiertos.
