# ARCH-02 — Resolución de Divergencia del Doble Stack en la Costura de Cálculo

- Responsable / sesión: /root/arch-divergence
- Estado operativo: en revisión
- Rama y ruta absoluta del worktree: feat/kmp-migration / C:\Users\Lenovo\Documents\finsim
- Commit base de main: ac7b763
- Última actualización: 2026-10-12
- Alcance y criterios: Candidato 2 de la revisión de arquitectura. Consolidación de `kmp/shared` como módulo canónico, deprecación de `src/engine/` y `src/domain/`, creación de ADR 0001, GLOSSARY.md y adenda en stack-maintainability.md.

## Traspaso actual

- Implementado:
  1. Deprecación formal documentada con `@deprecated` y especificación de la costura canónica en los módulos TypeScript de `src/domain/` y `src/engine/` (`scenario.ts`, `amortizationAccounting.ts`, `loan.ts`, `rates.ts` y sus suites de pruebas).
  2. Registro de Decisión Arquitectónica (ADR) creado en `docs/adr/0001-canonical-kmp-shared-engine.md`.
  3. Glosario canónico creado en `GLOSSARY.md` con términos de dominio financiero y principios de diseño de módulos profundos.
  4. Adenda de consolidación agregada en `docs/research/stack-maintainability.md`.
- Archivos modificados / creados:
  - `src/domain/scenario.ts`
  - `src/domain/scenario.test.ts`
  - `src/engine/amortizationAccounting.ts`
  - `src/engine/amortizationAccounting.test.ts`
  - `src/engine/loan.ts`
  - `src/engine/loan.test.ts`
  - `src/engine/rates.ts`
  - `src/engine/rates.test.ts`
  - `docs/adr/0001-canonical-kmp-shared-engine.md`
  - `GLOSSARY.md`
  - `docs/research/stack-maintainability.md`
  - `docs/tasks/ARCH-02.md`
- Comprobaciones:
  - `gradle :shared:jvmTest`: 83 pruebas unitarias aprobadas (0 fallos, 0 errores, 100% pasando).
  - `npm test`: 50 pruebas unitarias en Vitest aprobadas (11 archivos, 100% pasando).
- Commit de entrega: pendiente de integración.
- Cambios sin commit: archivos modificados en el workspace.
- Bloqueos / decisiones pendientes: ninguna.
- Siguiente paso exacto: Sesión coordinadora revisa e integra en rama correspondiente.
