# Continuidad del proyecto

Al iniciar o retomar implementación, lee `docs/implementation-plan.md` y `docs/tracking.md`. Identifica la tarea asignada y sigue el protocolo de reserva antes de editar.

Consulta `docs/product-brief.md` para alcance y aceptación; para cálculos, tasas, seguros, eventos, validación o importación, lee también `docs/financial-conventions.md`. `docs/research/stack-maintainability.md` contiene el fundamento del stack.

Actualiza el registro de tu tarea antes de cerrar la sesión. Solo la sesión coordinadora modifica el tablero compartido. Una tarea terminada en una rama pasa a revisión; se cierra después de integrarla y verificarla.

## Trabajo con subagentes

- Al delegar, usa `gpt-6-luna` con esfuerzo `max`.
- Mantén como máximo un subagente activo a la vez. Ejecuta implementación, revisión y correcciones por turnos; espera a que termine el turno actual antes de activar el siguiente.
- Estas son las reglas predeterminadas del proyecto. Cambia el modelo, esfuerzo o concurrencia solo por una instrucción explícita posterior del propietario.
