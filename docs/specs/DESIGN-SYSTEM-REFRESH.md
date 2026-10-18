# Especificación: Actualización del Sistema de Diseño «Warm Botanical Precision» (Stitch Refresh)

**ID de Proyecto Stitch:** `6070787729708745637`  
**Título del Proyecto Stitch:** Finsim Design System Refresh  
**Nombre del Sistema de Diseño:** Warm Botanical Precision  
**Estado:** `ready-for-agent`  
**Fecha:** 2026-10-17  

---

## 1. Problem Statement

La identidad visual previa de Finsim contenía ligeras discrepancias entre la especificación inicial en `DESIGN.md`, los estilos web en `src/styles.css` y los tokens de Compose Multiplatform en `Theme.kt`:
1. **Discrepancia en tonos de fondo y superficies:** El fondo web utilizaba alabastro antiguo (`#F6F5EE`), mientras que la especificación de diseño y la interfaz KMP migraron hacia porcelana fresca (`#F8F9F5` / `#F8FAF8`), generando una falta de paridad visual entre objetivos Web y Android.
2. **Desalineación en el color primario de marca y botones:** Se utilizaba `#173F32` / `#144436` de forma dispar, requiriendo unificación en el tono de pino botánico `#0F382A` acordado en Stitch.
3. **Falta de estandarización en acentos interactivos y foco:** Los acentos secundarios alternaban entre limas deslavados (`#D7E8A3`) y verdes genéricos (`#059669`). La especificación Stitch define con precisión el acento *Chartreuse* (`#C4F278`) para acciones secundarias y el menta financiero (`#84E296`) para anillos de foco accesibles (`outline: 3px solid #84E296`).
4. **Armonización de la cascada de gráficos:** Las series de gráficos de barras apiladas y líneas deben reflejar exactamente la secuencia semántica de Stitch: Capital habitual (`#2563EB`), Capital extra/recuperación (`#0D9488`), Intereses (`#D97706`) y Seguros (`#7C3AED`).

---

## 2. Solution

Adoptar e implementar de manera estricta y unificada el sistema de diseño **Warm Botanical Precision** generado en Stitch (Proyecto `6070787729708745637`) tanto en la capa web (`src/styles.css`, `Comparison.css`, `ScenarioComparison.tsx`) como en la capa Compose Multiplatform (`Theme.kt`, `HorizontalComparisonChart.kt`, `App.kt`):

1. **Tokens Fundacionales:**
   - Primario / Estructura: `#0F382A` (Botanical Pine)
   - Texto principal: `#0F2922`
   - Texto secundario / Muted: `#52665D`
   - Acento secundario: `#C4F278` (Radiant Chartreuse)
   - Foco / Acento terciario: `#84E296` (Financial Mint)
   - Fondo general: `#F8F9F5` (Warm Porcelain Paper)
   - Superficie de tarjetas / Paneles: `#FFFFFF`
   - Borde perimetral: `#E2E8DF`
   - Borde de inputs: `#CBD5C6`
   - Resalte de mes inspeccionado: `#F0FDF4`
2. **Cascada Semántica del Cronograma (4 Series + Cursor):**
   - Capital ordinario programado: `#2563EB` (Dark: `#1D4ED8`)
   - Capital extra y recuperación: `#0D9488` (Dark: `#059669`)
   - Interés corriente: `#D97706` (Dark: `#EA580C`)
   - Primas de seguros: `#7C3AED` (Dark: `#8B5CF6`)
   - Cursor de inspección: `#111827`
3. **Componentes y Estados Táctiles:**
   - Botón Primario: Relleno `#0F382A`, texto `#FFFFFF`, radio `9px`, altura `44px`.
   - Botón Secundario: Relleno `#C4F278`, texto `#0F2922`, radio `9px`, altura `44px`.
   - Botón Fantasma: Fondo transparente, texto `#0F382A`, borde `1px solid #E2E8DF`.
   - Foco accesible en todos los interactivos: `:focus-visible { outline: 3px solid #84E296; outline-offset: 3px; }`.
   - Cabecera fija de la tabla de amortización: `#E6ECE2` con tipografía de `11px` mayúscula.

---

## 3. User Stories

1. Como usuario del simulador en web o móvil, quiero ver una paleta unificada de alta legibilidad basada en porcelana y pino botánico, para analizar mis números sin fatiga visual.
2. Como usuario navegando con teclado, quiero ver anillos de foco accesibles en verde menta `#84E296`, para identificar con total nitidez el control activo en todo momento.
3. Como usuario que inspecciona gráficos de deuda, quiero que las series de capital, extra, intereses y seguros compartan exactamente los mismos códigos de color en todas las vistas y plataformas.
4. Como usuario que consulta la tabla mensual, quiero ver encabezados fijados en tono salvia claro (`#E6ECE2`) con filas cebra para escanear los 14 conceptos contables con rapidez.

---

## 4. Implementation Decisions & Tickets

El trabajo se desglosa en 5 tickets verticales modulares:
- **T01 — Tokens de Color y Tema Base (`Theme.kt` y `styles.css`)**: Unificación de variables CSS y tokens de Color en Compose.
- **T02 — Botones, Entradas y Contenedores KPI**: Estilos de botones primarios/secundarios/fantasma, campos de formulario con radio de 8px y tarjetas KPI.
- **T03 — Cascada Semántica de Gráficos (Web y Compose)**: Colores sincronizados para las series del cronograma y línea de cursor.
- **T04 — Tabla de Amortización y Anillos de Foco**: Estilos de cabecera `#E6ECE2`, fila hover `#F4F6F1` y foco `:focus-visible` de 3px en `#84E296`.
- **T05 — Verificación Técnica y Suites de Pruebas**: Ejecución de `npm run typecheck`, `npm test` y Gradle `:shared:jvmTest`.

---

## 5. Testing Decisions

- Las pruebas unitarias de contratos y lógica financiera (`:shared:jvmTest` y Vitest) deben continuar pasando al 100% sin alteraciones en importes ni redondeos.
- La comprobación de tipos (`tsc --noEmit`) debe validar que no se introdujeron errores de sintaxis en los componentes TypeScript/React.
- Verificación visual de contraste en la paleta para cumplir con WCAG AA.
