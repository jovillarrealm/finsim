# Especificación: Pantallas Stitch — Ergonomía Móvil y Comparación de Curvas Duales

**Proyecto Stitch:** Finsim Design System Refresh (`6070787729708745637`)  
**Pantallas Analizadas:**
1. **Simulador Móvil** (`268286d1e6574c7f9e4de2301f8045a5`) — Viewport: 780x5332 (Mobile)
2. **Simulador con Comparación de Curvas y Composición** (`5b0f011c635c4817a2d9f9f2c587fd78`) — Viewport: 2560x6694 (Desktop)
3. **Simulador y Comparación de Escenarios** (`f56d84d89cdb4bdb9642f951fa1ab40c`) — Viewport: 2560x5994 (Desktop Base)

**Estado:** `ready-for-agent`  
**Fecha:** 2026-10-17  

---

## 1. Problem Statement

La implementación previa de Finsim cuenta con una base de cálculo financiero exacta (motor decimal de 40 dígitos en KMP y TypeScript), pero presenta dos brechas de ergonomía y visualización entre la experiencia de escritorio y la móvil:

1. **Comprensión visual del impacto de prepagos:** El usuario necesita ver de forma simultánea cómo un abono a capital (por ejemplo, $5M en el mes 6) genera un "escalón" hacia abajo en la curva de saldo, abre una brecha de capital amortizado respecto al cronograma original y crea una ventana libre de deuda al final del crédito. Actualmente solo se mostraba el saldo final sin ilustrar la brecha sombreada ni la zona de extinción anticipada.
2. **Sobrecarga de información en móviles:** En pantallas compactas, mostrar una tabla de 48 o 120 filas completas dificulta la lectura de los momentos decisivos del crédito. Se requiere un modo curado de *Hitos Clave* (inicio, mes previo al abono, abonos extraordinarios y liquidación) junto con una barra inferior de navegación flotante (`pb-safe`) y tarjetas KPI de impacto inmediato.
3. **Composición de cuota mensual y torres de abono:** El gráfico de barras apiladas debe ilustrar con claridad las "torres" de prepago en los meses con eventos extraordinarios, la cuña decreciente de intereses en los meses posteriores y el tramo final con cuotas de $0.

---

## 2. Pantallas Generadas en Stitch y Modelos de Interacción

### 2.1 Pantalla Móvil (`268286d1e6574c7f9e4de2301f8045a5`)

```
┌─────────────────────────────────────────────────────────────┐
│  [menu]  finsim. [COP $]           [search] [profile]       │
├─────────────────────────────────────────────────────────────┤
│  • MENOS INCERTIDUMBRE. MÁS PERSPECTIVA                     │
│  Decide con los números claros.                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Escenario actual: Ejemplo · Libre inversión [Sinc]    │  │
│  │ [ Usar ejemplo ]       [ Guardar copia ]              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─ EL PANORAMA: AHORRO NETO PROYECTADO ─────────────────┐  │
│  │ AHORRO NETO PROYECTADO               [-36% intereses] │  │
│  │ - COP 3.810.150,00                                    │  │
│  │ [✓] Tiempo ganado: 12 meses      [Libre en Mes 36/48] │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─ CUADRÍCULA 2x2 KPIs SECUNDARIOS ─────────────────────┐  │
│  │ [ Cuota mensual fija ]     │ [ Intereses -COP 3.42M ] │  │
│  │   COP 773.718,50           │   COP 7.158.338          │  │
│  │ ───────────────────────────┼───────────────────────── │  │
│  │ [ Seguros totales ]        │ [ Total desembolso ]     │  │
│  │   COP 1.170.000 (-12m)     │   COP 33.328.338         │  │
│  └────────────────────────────┴──────────────────────────┘  │
│                                                             │
│  ┌─ CURVA DE SALDO & ZONA DE EXTINCIÓN ──────────────────┐  │
│  │ [Curva de Saldo] [Flujo Cuota]     -- Orig  ─ Abonos  │  │
│  │  $25M ┌───────────────────────┐                       │  │
│  │       │ \                     │                       │  │
│  │       │   \  [M6: -$5M]       │ ┌───────────────────┐ │  │
│  │       │     └──┐              │ │  LIBRE DE DEUDA   │ │  │
│  │   $0M └────────┴──────────────┴─┴───────────────────┘ │  │
│  │      M0      M6     M18       M36 (Deuda Cero)   M48  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─ INSPECCIÓN DEL MES ACTIVO (M6) ──────────────────────┐  │
│  │ Mes 06: Agosto 2026           [Abono Extraordinario]  │  │
│  │ Desembolso: $5.773.718,50     Impacto Capital: -$5.0M │  │
│  │ Cuota $773k + Abono $5.0M     Saldo: COP 17.842.110   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ▼ Parámetros del Crédito (details/summary colapsable)      │
│                                                             │
│  ┌─ GESTOR DE EVENTOS (2 Activos) ───────────────────────┐  │
│  │ [M6]  Abono extra a capital  COP 5.000.000    [x]     │  │
│  │ [M18] Abono extra a capital  COP 3.000.000    [x]     │  │
│  │ [+ Programar nuevo hito en Mes __ Importe __]         │  │
│  │ [ Aplicar cambios al escenario ]                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─ CRONOGRAMA CLAVE (Hitos Decisivos) ──────────────────┐  │
│  │ M01: Inicio           Mar 2026   Cuota: $773.718      │  │
│  │ M05: Previo al abono  Jul 2026   Saldo: $23.16M       │  │
│  │ M06: Abono Extra      Ago 2026   $5.773.718 (Saldo 17)│  │
│  │ M18: Abono Extra      Ago 2027   $3.773.718 (Saldo 9M)│  │
│  │ M36: Liquidación      Feb 2029   SALDO $0 (100% Pago) │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [Simulador]    [Cronograma]    [Comparar]    [Ajustes]     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Pantalla Escritorio con Curvas y Composición (`5b0f011c635c4817a2d9f9f2c587fd78`)

1. **Toolbar Superior Dockeada:** Título de escenario editable inline con input sin bordes visibles hasta foco, badge de sincronización local, botones de acción (`Usar ejemplo`, `Importar`, `Exportar`, `Guardar escenario`).
2. **Layout de 12 Columnas:**
   - **Col 1-5 (Izquierda):** Formulario "Tu préstamo" con entradas de 44px, selector de moneda COP/USD/EUR, tasa E.A. y plazo, seguido por la tarjeta educativa de *La cascada legal de amortización* (imputación obligatoria: seguros -> intereses -> capital).
   - **Col 6-12 (Derecha):** Tarjetas de resumen y KPIs con el desglose del costo de financiación.
3. **El Corazón del Producto (Comparador de Escenarios Dual):**
   - **4 Tarjetas Delta:** Ahorro en intereses (`-COP 3.420.150`), Ahorro en seguros (`-COP 390.000`), Ahorro neto total (`-COP 3.810.150` con contenedor `#0F382A` y acento `#C4F278`), y Tiempo ganado (`-12 meses`).
   - **Gráfico A (Trayectoria de Saldo con Brecha y Zona de Extinción):**
     - Curva original punteada (`#717974`, 48 meses).
     - Curva modificada verde esmeralda (`#0D9488`, grosor 3.5px) con escalones en M6 y M18.
     - Polígono de brecha de amortización sombreado con gradiente `#0D9488` (22% a 4% de opacidad).
     - Zona de extinción rectangular sombreada en `#ECFDF5` con borde discontinuo `#A7F3D0` y marca de agua `LIBRE DE DEUDA · $0 pagos requeridos`.
     - Badges flotantes en coordenadas exactas (`-$5M en saldo`, `M18: -$3M prepago`, `Mes 36: Saldo $0`).
   - **Gráfico B (Composición de Cuota Mensual y Torres de Abono):**
     - Capas de seguros (`#7C3AED`, 4px de altura base).
     - Cuña de intereses (`#D97706`).
     - Base de capital ordinario (`#2563EB`).
     - Torre de prepago en M6 (altura equivalente a $5.77M con bloque esmeralda `#0D9488` de 52px).
     - Zona de meses 37 a 48 vacía con recuadro `$0 CUOTAS RESTANTES · Ahorro: $9.284.622 COP en cuotas`.
   - **Cursor Interactivo Sincronizado (`#111827`):** Guía vertical que se desplaza simultáneamente en ambos gráficos.
   - **Tarjeta de Inspección de Mes Activo:** Muestra el detalle del mes apuntado por el cursor con botón "Modificar este mes".

---

## 3. Especificación de Tokens y Estilos Visuales

| Token Semántico | Código Hex / Valor | Rol en Interfaz |
| --- | --- | --- |
| `primary` | `#002217` / `#0F382A` | Pino botánico: barra superior, títulos, botón primario |
| `secondary` | `#466800` / `#C4F278` | Radiant Chartreuse: botón secundario, punto de logo, badges de ahorro |
| `tertiary` / `mint` | `#84E296` | Financial Mint: anillo `:focus-visible` (3px outline, 3px offset) |
| `background` | `#F9FAF6` / `#F8F9F5` | Fondo general de porcelana tibia |
| `surface-card` | `#FFFFFF` | Tarjetas y contenedores de panel |
| `surface-highlight` | `#F0FDF4` | Fondo de inspección de mes activo y resalte de abonos |
| `border-sage` | `#E2E8DF` | Delineados estructurales y bordes de tarjetas |
| `border-input` | `#CBD5C6` | Borde de cajas de texto y campos numéricos (radio 8px) |
| `flow-regular-principal` | `#2563EB` | Capital habitual programado |
| `flow-extra-principal` | `#0D9488` | Capital extraordinario y amortización acelerada |
| `flow-interest` | `#D97706` | Intereses corrientes |
| `flow-insurance` | `#7C3AED` | Primas de seguro mensual |
| `flow-reference-cursor` | `#111827` | Línea vertical de inspección sincronizada |
| `extinction-zone-bg` | `#ECFDF5` | Zona de meses extinguidos (Libre de deuda) |
| `extinction-zone-border` | `#A7F3D0` | Borde punteado de la zona de extinción |

---

## 4. User Stories

1. **US-01 (Móvil — Navegación y Resumen Rápido):** Como usuario en teléfono móvil, quiero una barra inferior fija con pestañas accesibles y un resumen superior que me indique de inmediato cuánto dinero y tiempo ahorro, para tomar decisiones en segundos sin desplazarme por tablas infinitas.
2. **US-02 (Móvil — Curva Táctil y Zona de Paz):** Como usuario explorando prepagos en móvil, quiero ver gráficamente cuándo termina mi crédito antes de tiempo con una zona resaltada de "Libre de deuda", para dimensionar el beneficio emocional y financiero de mis abonos.
3. **US-03 (Móvil — Hitos Clave):** Como usuario revisando el cronograma en pantalla pequeña, quiero ver únicamente los meses donde ocurren cambios trascendentales (inicio, abonos y mes de cancelación final), con opción de descargar el CSV completo.
4. **US-04 (Escritorio — Brecha de Capital Amortizado):** Como usuario analítico en pantalla grande, quiero ver el polígono de brecha sombreado entre la curva original y la curva modificada, para comprender cómo se amplía la amortización tras cada prepago.
5. **US-05 (Escritorio — Composición de Cuota y Torres de Abono):** Como usuario analítico, quiero ver las torres de desembolso mensual en el gráfico de composición, identificando con claridad la reducción posterior de intereses y la ausencia total de pagos tras la liquidación.

---

## 5. Tickets de Implementación

- **T06 — Barra de Navegación Móvil Flotante y Shell Responsivo**: Implementar barra inferior fija con clases `pb-safe`, `backdrop-blur-xl`, estados activos de pestañas y contenedor móvil optimizado.
- **T07 — Tarjeta Hero de Ahorro y Cuadrícula KPI 2x2**: Renderizar tarjeta con gradiente difuso de fondo, badge de porcentaje de ahorro, tiempo ganado y cuadrícula secundaria de 4 métricas con tipografía `tabular-nums`.
- **T08 — Gráfico de Trayectoria de Saldo con Zona de Extinción**: Renderizar SVG/Canvas responsivo con curva original punteada, curva modificada escalonada, polígono sombreado de brecha y zona `LIBRE DE DEUDA` (`#ECFDF5`).
- **T09 — Gráfico de Composición Mensual con Torres de Abono**: Renderizar gráfico apilado con las 4 series semánticas, torres de abono en meses de evento extraordinario y bloque de cuotas restantes a $0.
- **T10 — Cronograma de Hitos Decisivos**: Implementar vista de hitos clave (M01, M05, M06, M18, M36) como alternativa compacta para dispositivos móviles, preservando el acceso a la tabla mensual exhaustiva y exportación CSV.

---

## 6. Marco de Evaluación y Suite de Pruebas

Para garantizar que la implementación conserve la exactitud matemática y la fidelidad ergonómica requerida, se define una estrategia de validación en **6 pilares objetivos y automatizables**:

### 6.1 Los 6 Pilares de Evaluación

1. **Rigor e Invariantes Financieras (*Zero-Mock Math Verification*):**
   - **Conservación exacta de caja:** Toda cifra en pantalla proviene determinísticamente de `simulateLoan` (aritmética decimal de 40 dígitos). Cero valores estáticos, redondeados con sesgo o ficticios.
   - **Invariante mensual:** En cada mes $m$: $\text{Saldo final} = \text{Saldo inicial} - \text{Capital habitual} - \text{Capital extraordinario}$.
   - **Cascada de pagos contractual:** Imputación prioritaria estricta en 6 etapas (seguros pendientes $\rightarrow$ seguros del mes $\rightarrow$ intereses vencidos $\rightarrow$ intereses del mes $\rightarrow$ capital vencido $\rightarrow$ capital ordinario).
   - **Paridad multiplataforma:** Coincidencia exacta entre el motor TypeScript (`src/engine/loan.ts`) y el canónico en Kotlin Multiplatform (`kmp/shared/.../Loan.kt`).

2. **Pruebas de Componentes y Estado Reactivo:**
   - **Sincronización bidireccional:** Pulsar o hacer hover sobre un punto de la curva SVG o barra apilada actualiza de inmediato el cursor `#111827`, el panel de inspección activa (`Mes XX Seleccionado`) y enfoca el gestor de eventos.
   - **Conmutación de modos:** Alternar entre `Trayectoria de saldo` y `Composición mensual` preserva el mes activo sin recargar la simulación.
   - **Selector de cronograma:** Alternar entre `Hitos Clave` y `Tabla Completa` conmuta la presentación sin alterar los valores subyacentes.

3. **Auditoría de Tokens y Conformidad Visual (*Design System Audit*):**
   - **Alineación con Warm Botanical Precision:** Verificación de tokens de diseño (`#C4F278`, `#002217`, `#F0FDF4`, `#0D9488`, `#D97706`, `#7C3AED`, `#111827`).
   - **Estabilidad de layout con `tabular-nums`:** Todos los importes monetarios y cronológicos emplean `font-variant-numeric: tabular-nums` para evitar desplazamientos de layout durante interacciones dinámicas.
   - **Ergonomía táctil móvil:** Áreas de contacto $\ge 44 \times 44\text{ px}$ en todos los botones e ítems de la barra de navegación flotante.
   - **Márgenes de seguridad:** Respeto estricto a las clases `pb-safe` y `pt-safe` (`env(safe-area-inset-*)`).

4. **Pruebas de Estrés y Casos de Borde Financieros (*Boundary Fuzzing*):**
   - **Escenario base sin prepagos:** La curva original y modificada se superponen con brecha cero. La zona `LIBRE DE DEUDA` permanece oculta y no se generan falsos positivos de ahorro.
   - **Prepagos masivos tempranos:** Liquidación anticipada en mes 1 o mes 2 renderiza adecuadamente la zona de extinción sin divisiones por cero o coordenadas SVG NaN.
   - **Horizonte de 360 meses (Hipotecario):** Escalamiento del eje temporal y SVG fluido sin congelamiento del hilo principal.
   - **Multi-divisa:** Formateo riguroso en `COP`, `USD`, `EUR`, `MXN` y `PEN`.

5. **Auditoría de Accesibilidad (WCAG 2.1 AA):**
   - **Contraste de color:** Ratio $\ge 4.5:1$ en todos los estados y badges (ej. `#065F46` sobre `#ECFDF5`).
   - **Semántica y lectores de pantalla:** `role="region"`, `role="tab"`, `aria-selected`, `aria-live="polite"`, `aria-current="page"` y `role="status"`.
   - **Navegación por teclado:** Foco visible inconfundible con outline de 3px en Financial Mint (`#84E296`).

6. **Integridad de Datos y Exportabilidad (*Round-Trip Interoperability*):**
   - **Exportación CSV:** Formato tabular estándar con delimitadores y columnas completas, importable directamente en Excel o Google Sheets.
   - **Idempotencia JSON:** Exportación e importación de escenarios produce árboles de objetos idénticos al 100%.

---

### 6.2 Suites Automatizadas de Integración

#### Web (`StitchScreensIntegration.test.tsx`)
La suite implementada en [`src/features/loan/StitchScreensIntegration.test.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/features/loan/StitchScreensIntegration.test.tsx) valida directamente contra los criterios de aceptación en el navegador:

| Caso de Prueba | Componente Evaluado | Comprobaciones Clave |
| :--- | :--- | :--- |
| `LoanSummary renders the executive Hero Savings Card and 2x2 secondary KPI grid with prepayments` | [`LoanSummary.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/features/loan/LoanSummary.tsx) | - Presencia de `hero-savings-card` y `Ahorro Neto Proyectado`<br/>- Cálculo dinámico de `% intereses` y `Tiempo ganado:`<br/>- Presencia de `kpi-grid-2x2` (Cuota mensual fija, Intereses con valor pactado tachado, Seguros con cuotas ahorradas, Total desembolso). |
| `ScenarioComparison renders Trajectory Curve with extinction zone and active month card` | [`ScenarioComparison.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/features/loan/ScenarioComparison.tsx) | - Selector `Trayectoria de saldo`<br/>- SVG con clase `trajectory-svg` y gradiente `amortDeltaGradient`<br/>- Activación de zona `LIBRE DE DEUDA` y `$0 pagos requeridos`<br/>- Presencia de `active-month-card` con detalle del mes y botón `Modificar este mes`. |
| `MonthlyTable renders curated Key Milestones with prepayments and full schedule toggle` | [`MonthlyTable.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/features/loan/MonthlyTable.tsx) | - Alternancia de vistas (`Hitos Clave` vs `Tabla Completa`)<br/>- Lista `milestones-schedule-list` con `Mes 01`<br/>- Badges `+X Extra` en meses de abono y `Fin de Crédito` en liquidación<br/>- Botón de exportación `Descargar cronograma completo (.CSV)`. |
| `MonthlyTable renders clean fallback without prepayments when no events exist` | [`MonthlyTable.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/features/loan/MonthlyTable.tsx) | - Escenario sin eventos omite badges `badge-extra`<br/>- Conserva `Mes 01` e hito final de finalización pactada. |

#### Pruebas Kotlin Multiplatform vigentes

- [`ScheduleCsvGenerationTest.kt`](../../kmp/composeApp/src/commonTest/kotlin/com/finsim/app/ui/components/ScheduleCsvGenerationTest.kt) verifica el BOM, la cabecera y la primera fila de la exportación CSV.
- [`LoanPrepaymentFinancialInvariantsTest.kt`](../../kmp/composeApp/src/commonTest/kotlin/com/finsim/app/LoanPrepaymentFinancialInvariantsTest.kt) verifica invariantes del motor y efectos financieros de los abonos.
- Estas pruebas no verifican hitos visibles de la tabla ni el estado del resumen de ahorro; los tests tautológicos que afirmaban esa cobertura fueron retirados.

---

### 6.3 Comandos de Verificación en Integración Continua (CI)

```bash
# 1. Pruebas unitarias e integración Web (54 tests en 12 suites)
npm test

# 2. Comprobación estricta de tipos TypeScript (cero errores)
npm run typecheck

# 3. Compilación de producción Vite
npm run build

# 4. Pruebas de motor canónico Kotlin Multiplatform (115 tests JVM)
gradle -p kmp :shared:jvmTest

# 5. Pruebas unitarias de componentes Compose Multiplatform
gradle -p kmp :composeApp:testDebugUnitTest
```

