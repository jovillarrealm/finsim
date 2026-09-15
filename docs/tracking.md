# Tracking de implementación

## Estado actual

Actualizado: 2026-09-15. Implementación iniciada. Git inicializado en main. Sesión coordinadora: /root. Tarea activa: **FS-00**.

Este tablero es la fuente de estado integrado. El [plan](implementation-plan.md) define alcance, dependencias y criterios de cierre. Los registros por tarea guardan el detalle de continuidad.

| ID | Entrega | Estado | Responsable / sesión | Rama / worktree | Evidencia integrada |
| --- | --- | --- | --- | --- | --- |
| FS-00 | Repositorio y aplicación | en curso | /root | main / finsim | instalación y verificación pendientes |
| FS-01 | Contratos y validación | pendiente | — | — | — |
| FS-02 | Motor de tasas | pendiente | — | — | — |
| FS-03 | Préstamo base y seguros | pendiente | — | — | — |
| FS-04 | Primera interfaz | pendiente | — | — | — |
| FS-05 | Eventos y atrasos | pendiente | — | — | — |
| FS-06 | Persistencia | pendiente | — | — | — |
| FS-07 | Comparación y edición | pendiente | — | — | — |
| FS-08 | Conciliación | pendiente | — | — | — |
| FS-09 | Conversor visual | pendiente | — | — | — |
| FS-10 | Aceptación integrada | pendiente | — | — | — |
| FS-11 | GitHub Pages | pendiente | — | — | — |
| FS-12 | CDT/ETF | diferida | — | — | — |

## Estados

- `pendiente`: tiene dependencias sin integrar.
- `lista`: dependencias integradas; puede reservarse.
- `en curso`: reservada por una sesión identificada, con rama y worktree propios.
- `pausada`: conserva su reserva y un siguiente paso registrado para otra sesión.
- `bloqueada`: registra impedimento concreto, quién puede resolverlo y condición de salida.
- `en revisión`: implementación en rama con evidencia; falta revisión o integración.
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

Para arrancar: «Implementa FS-00 siguiendo AGENTS.md y el protocolo de docs/tracking.md. Registra evidencia y deja la siguiente tarea lista».

Para retomar una tarea asignada: «Retoma FS-NN en su worktree registrado. Lee su ficha, verifica el estado real y continúa desde el siguiente paso. Actualiza el traspaso al terminar».

Para coordinar: «Revisa entregas en revisión, integra una por vez, verifica y actualiza el tablero. Reserva las siguientes tareas independientes antes de abrir nuevas sesiones».

