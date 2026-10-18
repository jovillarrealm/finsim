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

## 6. Testing & Validaciones

- **Pruebas de Invarianza Numérica:** Todas las métricas presentadas en los hitos y tarjetas KPI deben provenir de `simulateLoan` y sus componentes `paidComponents`, manteniendo paridad de 40 dígitos en `:shared:jvmTest` y Vitest.
- **Comprobación de Tipos:** `npm run typecheck` (`tsc --noEmit`) sin advertencias ni errores.
- **Accesibilidad y Contraste:** Contrastes de texto WCAG AA sobre fondos `#F8F9F5`, `#FFFFFF` y `#ECFDF5`.
- **Pruebas de Regresión:** `npm test` (50 pruebas web) y `gradle :composeApp:testDebugUnitTest`.
