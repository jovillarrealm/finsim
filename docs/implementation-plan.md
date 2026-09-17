# Plan de implementación

## Base y alcance

Fecha inicial: 2026-09-15. Existen las especificaciones; aún no hay código, repositorio Git ni comprobaciones ejecutables.

Este plan toma como base el [producto acordado](product-brief.md), las [convenciones financieras](financial-conventions.md) y el [stack recomendado](research/stack-maintainability.md). La solicitud de plan permite organizar el trabajo; los estados del [tablero](tracking.md) reflejan ejecución real, no intención.

Primera entrega: préstamos COP de tasa fija, dos escenarios, abonos para reducir plazo, seguros, omisiones y recuperación, conciliación manual, conversor y persistencia local. CDT/ETF queda en el siguiente hito.

Stack de partida: TypeScript estricto, React, Vite, decimal.js, Zod, Recharts y Vitest. Una aplicación, motor independiente de React, sin backend. Las versiones compatibles se fijan al instalar; el archivo de dependencias bloqueadas se integra antes de abrir trabajo paralelo.

## Secuencia y tareas

Los directorios siguientes son límites propuestos de responsabilidad, no archivos que deban crearse por anticipado. Cada tarea produce una entrega revisable; puede ocupar varias sesiones en el mismo worktree.

### Fase 0 — Base compartida

**FS-00 · Preparar repositorio y aplicación.** Dependencias: ninguna.

- Inicializar Git con rama de integración `main`, exclusiones y primer commit de los documentos y aplicación mínima. Verificar primero que no haya aparecido un repositorio desde la planificación.
- Configurar TypeScript estricto, React/Vite, Vitest y scripts de desarrollo, comprobación de tipos, pruebas y build. Instalar las dependencias acordadas y fijarlas en el lockfile.
- Responsabilidad: configuración raíz, dependencias, entrada de aplicación y README con instrucciones locales.
- Cierre: instalación reproducible, comprobación de tipos, prueba mínima y build exitosos; app visible en navegador. Registrar versiones, comandos y commit base.

**FS-01 · Contratos y validación.** Depende de FS-00.

- Definir esquema versionado de entradas: préstamo original o saldo actual al día, tasa, cuota/plazo, seguros y eventos; decimales serializados como cadenas.
- Definir resultados mensuales y resumen: cargos generados, distribución por pago habitual/recuperación/extra, capital vencido, pendientes, excedente no aplicado, liquidación y resultado incompleto.
- Fijar convención de índices de mes, fecha inicial, unidades de tasa y representación de dinero; documentarlas junto al contrato. Centralizar precisión y redondeo según las convenciones financieras.
- Responsabilidad: `src/domain/`; introducir únicamente contratos requeridos por la primera entrega.
- Cierre: pruebas de datos válidos e inválidos, límites de 1.200 meses, dinero de más de dos decimales, valores no finitos y discriminantes desconocidos. Publicar firmas y un ejemplo mínimo para los consumidores.

### Fase 1 — Motor verificable y primera interfaz

**FS-02 · Conversión de tasas.** Depende de FS-01.

- Implementar EA, EM y nominal anual con frecuencia explícita; conservar precisión interna.
- Responsabilidad: `src/engine/rates.ts` y sus pruebas.
- Cierre: ejemplos literales de la sección 6 de convenciones; ida y vuelta con error absoluto máximo `1e-12`, tasa cero y frecuencias válidas. La interfaz se entrega en FS-09.

**FS-03 · Préstamo base y seguros.** Depende de FS-02.

- Calcular cuota o aceptar cuota conocida; registrar cronograma, seguros y totales. Implementar la distribución de pagos en un solo lugar para extenderla en FS-05.
- Responsabilidad: `src/engine/loan*` y pruebas relacionadas.
- Cierre: seis referencias de la sección 5, conservación de capital/cargos/caja, cuota final sin mes espurio, seguros posteriores y resultado incompleto cuando no amortiza. Ningún saldo negativo.

**FS-04 · Primera interfaz con préstamo base.** Depende de FS-03.

Preparación paralela permitida desde FS-01: componentes que reciben `Scenario` y `LoanResult` por propiedades. La conexión real y el cierre siguen dependiendo de FS-03.

- Ejemplo identificado, entrada de préstamo personal, valores editables, resumen y tabla mensual desplegable; textos en español y formato colombiano.
- Responsabilidad: `src/features/loan/`, estilos y composición de la aplicación.
- Cierre: préstamo original y saldo actual funcionan en escritorio y móvil; errores junto a campos; controles etiquetados y operables con teclado. Recoger observaciones de la primera vista para FS-07.

**FS-05 · Eventos, atrasos y liquidación.** Depende de FS-03.

- Añadir abonos únicos/recurrentes, omisiones y recuperación siguiendo la secuencia mensual y prioridad de categorías. Conservar cuota base; permitir extensión real del plazo.
- Responsabilidad: mismo motor de préstamos y sus pruebas; no trabajar en paralelo con otra modificación de ese motor.
- Cierre: referencias de recuperación completa y sus tres variantes; omisiones consecutivas, recuperación insuficiente, omisión con abono, excedentes, eventos posteriores a liquidación, seguros y límite operativo. Escenario sin eventos idéntico al original; totales truncados identificados como incompletos.

**FS-06 · Guardar, exportar e importar.** Depende de FS-01.

- Implementar almacenamiento local y JSON versionado con validación; recalcular resultados al cargar, sin persistir cronogramas derivados.
- Responsabilidad: `src/persistence/` y sus pruebas; conexión visual en FS-07.
- Cierre: ida y vuelta preserva entradas/eventos/seguros; JSON inválido, versión no soportada y almacenamiento inaccesible muestran errores sin reemplazar el escenario válido. No crear migraciones para versiones que todavía no existen.

### Fase 2 — Exploración completa

**FS-07 · Comparación y edición directa.** Depende de FS-04, FS-05 y FS-06.

Preparación de gráficos y editor permitida cuando el motor de eventos y persistencia ya están integrados. El cierre requiere completar la verificación FS-05 y conectar la interfaz FS-04.

- Conectar original/modificado, gráficos mensuales apilados con ejes y escala temporal comunes, selección de mes, controles cercanos y alternativa desde tabla.
- Mostrar capital habitual, extras y su distribución, intereses, seguros, fecha de liquidación, caja total y diferencias con signo. Incluir meses de seguros posteriores y plazos extendidos.
- Conectar guardado/importación/exportación y reflejar fallos sin perder edición.
- Responsabilidad: interfaz de préstamos y composición; no cambiar fórmulas para ajustar la presentación.
- Cierre: editar un evento actualiza tabla, gráficos y totales del mismo resultado; teclado y móvil permiten todo el flujo. Un resultado incompleto no muestra ahorro definitivo. Comprobar con el usuario la interpretación de «click donde editar» y registrar respuesta pendiente si no está disponible.

**FS-08 · Conciliación manual.** Depende de FS-03.

Preparación paralela permitida desde FS-01 usando el contrato `MonthRow` y referencias literales. La comprobación con el cronograma real sigue dependiendo de FS-03.

- Introducir filas seleccionadas de extracto y comparar componentes/saldos por mes, mostrando diferencias y explicaciones de supuestos sin ajustar el motor automáticamente.
- Responsabilidad: `src/features/reconciliation/` y comparación pura con pruebas; registrar integración de navegación para FS-10.
- Cierre: fila coincidente y fila discrepante con valores esperados literales, validación de meses/importes y diferencias visibles. Un caso sintético valida la función; solo un caso real permite afirmar coincidencia con una entidad.

**FS-09 · Interfaz del conversor.** Depende de FS-02.

- Crear conversor independiente con frecuencia explícita, unidades y ejemplo explicado.
- Responsabilidad: `src/features/rates/`; registrar integración de navegación para FS-10.
- Cierre: conversiones visibles coinciden con referencias, distinguen porcentaje/fracción y no pierden precisión interna por redondeo visual; etiquetas y teclado verificados.

### Fase 3 — Integración y entrega

**FS-10 · Aceptación de primera versión.** Depende de FS-07, FS-08 y FS-09.

- Integrar navegación por pestañas o hash. Ejecutar tipos, pruebas del motor y build sobre el conjunto integrado.
- Añadir una suite pequeña de navegador para préstamo, evento y persistencia, con resultados esperados independientes. Revisar manualmente móvil, teclado y lectura accesible de gráficos mediante tabla.
- Responsabilidad: composición, pruebas de aceptación y registro de resultados. Corregir defectos detectados en su módulo responsable.
- Cierre: criterios 1–7 del brief demostrados con evidencia. Para el criterio 8, un miembro de la audiencia explica qué cambió sin ayuda; registrar observación real. Si falta esa sesión, mantener el criterio pendiente, sin simular aprobación.

**FS-11 · Preparar y publicar GitHub Pages.** Depende de FS-10.

- Preparar despliegue estático con base path del repositorio real y comprobaciones previas. Configurar destino cuando se conozca la cuenta/repositorio.
- Responsabilidad: configuración Vite y workflow de publicación.
- Cierre: cuando se solicite publicar, URL real comprobada, recursos cargan bajo su subruta y exportación/importación funciona allí. Antes de esa autorización, dejar artefacto y configuración revisables; no marcar publicación completada.

### Hito posterior

**FS-12 · Comparaciones CDT/ETF.** Depende de FS-11 y priorización explícita del siguiente hito.

Descomponer entonces la entrega: mismo capital/aportes/horizonte, costos, retiros, liquidez, reinversión o efectivo del CDT y cambio editable para ETF. Mantener los rendimientos como supuestos; sin datos en vivo ni replay histórico. La primera versión no depende de esta tarea.

## Paralelismo permitido

1. FS-00 y FS-01 se integran secuencialmente antes de abrir consumidores.
2. Tras FS-01: FS-02 y FS-06 pueden avanzar en worktrees separados.
3. Tras FS-02: FS-03 y FS-09 pueden avanzar en paralelo, junto a FS-06 si sigue abierta.
4. Tras FS-03: FS-04, FS-05 y FS-08 pueden avanzar en paralelo; FS-09 también es independiente. Ejecutar solo tantos frentes como sesiones disponibles.
5. FS-07 reúne interfaz, motor y persistencia; FS-10 reúne todas las funciones.

La independencia exige mantener los límites de archivos. Cambios en contratos, dependencias, lockfile o composición se coordinan antes de editar. Un consumidor bloqueado por un contrato solicita el ajuste; no mantiene una copia divergente. No hacer merges entre ramas hermanas: integrar primero el prerrequisito y actualizar la rama consumidora desde `main`.

## Evidencia y cobertura

| Criterio del brief | Entrega responsable | Verificación final |
| --- | --- | --- |
| 1. Cronograma del préstamo | FS-03, FS-04 | FS-10 |
| 2. Edición de abonos | FS-05, FS-07 | FS-10 |
| 3. Conciliación | FS-08 | FS-10 |
| 4. Omisiones y recuperación | FS-05, FS-07 | FS-10 |
| 5. Tiempo, costos y dinero adicional | FS-05, FS-07 | FS-10 |
| 6. Tasas | FS-02, FS-09 | FS-10 |
| 7. Persistencia completa | FS-06, FS-07 | FS-10 |
| 8. Comprensión sin ayuda | FS-07, FS-10 | Observación con audiencia |

Las comprobaciones financieras de la sección 6 de convenciones son obligatorias en las tareas correspondientes. Los valores esperados se escriben independientemente del motor. El plan no reemplaza ni redefine esas reglas.
