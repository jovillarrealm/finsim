# Finsim · Documento de Diseño de Producto y Sistema Visual (DESIGN.md)

> «MENOS INCERTIDUMBRE. MÁS PERSPECTIVA.»  
> **Decide con los números claros.**  
> *Un pequeño cambio hoy. Una historia distinta mañana.*  
> — Tagline y narrativa central de Finsim (`index.html`, `src/App.tsx`, `docs/product-brief.md`)

---

## 1. Propósito del Codebase, Archivos Críticos y Objetivos

### 1.1 Propósito Fundamental
Finsim es un laboratorio y simulador financiero educativo, concebido con enfoque *Spanish-first*, para permitir a las personas comprender las consecuencias cuantitativas de sus decisiones de endeudamiento y financiación a través de **escenarios editables y comparables**.

A diferencia de las herramientas bancarias comerciales o los modelos probabilísticos abstractos, Finsim opera bajo el principio de **prescripción directa de eventos**: la persona no asigna probabilidades a futuros inciertos, sino que programa explícitamente abonos extraordinarios, omisiones de pago o cuotas de recuperación en meses específicos para observar el impacto directo sobre el plazo, los intereses y el dinero total de su bolsillo.

El alcance actual modela préstamos a tasa fija en cuatro monedas (`COP`, `ARS`, `USD`, `EUR`) sin conversión cambiaria implícita, calculando cronogramas mensuales con aritmética decimal de 40 dígitos significativos, reglas de imputación en cascada y trazabilidad completa de seguros y atrasos.

### 1.2 Archivos Críticos del Repositorio

| Archivo / Ruta | Responsabilidad Arquitectónica y Dominio |
| :--- | :--- |
| [`docs/product-brief.md`](file:///C:/Users/Lenovo/Documents/finsim/docs/product-brief.md) | **Especificación Canónica de Producto**: Define el alcance acordado, criterios de aceptación (1–8), filosofía de edición directa y verificación de la audiencia. |
| [`docs/financial-conventions.md`](file:///C:/Users/Lenovo/Documents/finsim/docs/financial-conventions.md) | **Convenciones Financieras y Aritméticas**: Reglas matemáticas inviolables: redondeo *half-up* a dos decimales, prelación de 6 etapas de imputación, tratamiento de seguros y mora ilustrativa. |
| [`docs/adr/0001-canonical-kmp-shared-engine.md`](file:///C:/Users/Lenovo/Documents/finsim/docs/adr/0001-canonical-kmp-shared-engine.md) | **Decisión Arquitectónica Canónica**: Establece el módulo Kotlin Multiplatform `:shared` como la única fuente de verdad para cálculos, congelando el motor legado de TypeScript en `src/engine/`. |
| [`GLOSSARY.md`](file:///C:/Users/Lenovo/Documents/finsim/GLOSSARY.md) / [`CONTEXT.md`](file:///C:/Users/Lenovo/Documents/finsim/CONTEXT.md) | **Lenguaje de Dominio Unificado**: Glosario canónico financiero y arquitectónico (Cronograma, Imputación, Capital vencido, Perfil de precancelación, Margen mensual, Costo de financiación). |
| [`kmp/shared/.../domain/Scenario.kt`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/domain/Scenario.kt) | **Contratos Canónicos de Dominio**: Modelos de datos inmutables y esquemas de validación: `Scenario`, `LoanEvent`, `MonthRow`, `LoanResult`, `Allocation`. |
| [`kmp/shared/.../engine/Loan.kt`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/engine/Loan.kt) | **Motor de Amortización**: Algoritmo mensual puro que ejecuta la simulación cronológica hasta un horizonte máximo de 1.200 meses con conservación exacta de capital y caja. |
| [`kmp/shared/.../engine/AmortizationAccounting.kt`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/engine/AmortizationAccounting.kt) | **Libro Mayor Contable en Proceso**: Rastreo de deuda total, atrasos acumulados, capital vencido y adaptadores normativos (ej. comisiones e IVA del perfil argentino). |
| [`kmp/shared/.../workspace/WorkspaceStore.kt`](file:///C:/Users/Lenovo/Documents/finsim/kmp/shared/src/commonMain/kotlin/com/finsim/workspace/WorkspaceStore.kt) | **Almacén de Estado de Espacio de Trabajo**: Orquestación reactiva de escenarios, cálculo dual (`original` vs `modified`), gestión del lote de eventos en borrador (`EventDraftBatch`) y persistencia. |
| [`kmp/composeApp/.../theme/Theme.kt`](file:///C:/Users/Lenovo/Documents/finsim/kmp/composeApp/src/commonMain/kotlin/com/finsim/app/theme/Theme.kt) | **Sistema de Tokens de Diseño Multiplataforma**: Paleta institucional (`BrandForest`, `BrandLime`, `BrandSage`, `BrandAmber`, `BrandPlum`), tipografía y esquemas de color Compose. |
| [`src/styles.css`](file:///C:/Users/Lenovo/Documents/finsim/src/styles.css) / [`src/App.tsx`](file:///C:/Users/Lenovo/Documents/finsim/src/App.tsx) | **Identidad Visual Web Original**: Variables CSS CSS3 (`--green`, `--line`, `--muted`, `--lime`), jerarquía tipográfica `clamp()`, layout responsivo y estados de interacción web. |

### 1.3 Objetivos del Sistema
1. **Precisión Matemática Sin Concesiones:** Cero imprecisiones de punto flotante en dinero; 40 dígitos de precisión arbitraria en tasas y cuotas (`Decimal`), asegurando que la caja pagada sea exactamente igual a la suma de seguros, intereses y capital amortizado.
2. **Claridad Comparativa Inmediata:** Confrontar permanentemente el escenario base contra el escenario modificado con eventos, haciendo visibles los meses eliminados por pago anticipado o extendidos por omisiones.
3. **Privacidad y Operación Local en Edge:** Cero telemetría, cero dependencias en servidores remotos, guardado en almacenamiento local del dispositivo (`localStorage` / almacenamiento local KMP) y copias portables en formato JSON versión 1 y Apache Arrow IPC.
4. **Ergonomía Adaptativa:** Experiencia táctil y de escritorio fluida con teclados numéricos decimales nativos en móviles, navegación sin glifos rotos y accesibilidad integral por teclado y lector de pantalla.

---

## 2. La Forma Nativa del Producto: La Línea de Tiempo Dual Paralela

La arquitectura visual y de interacción de Finsim no responde a la plantilla tradicional de una *landing page* corporativa ni a un formulario CRUD administrativo. La estructura de datos y el motor financiero dictan su **forma nativa esencial**:

```
                                    EJE TEMPORAL (Mes 1 ... Mes N ... Mes 1.200)
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  ESCENARIO ORIGINAL (Base)                                                                           │
│  [Mes 1][Mes 2][Mes 3][Mes 4] ... [Mes 24][Mes 25] ... [Mes 36: LIQUIDACIÓN ORIGINAL]                │
│  (Cuota habitual: Capital programado + Interés mensual + Seguro mensual)                              │
└───────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  ▲
                        DIFERENCIAS CON SIGNO (+ / -) Y AHORRO REAL
                                                  ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  ESCENARIO MODIFICADO (Con Eventos Prescritos)                                                        │
│  [Mes 1][Mes 2]──[Mes 3: ABONO EXTRA]──[Mes 4] ... [Mes 28: LIQUIDACIÓN ANTICIPADA] ░░░ [Mes 36]       │
│  (Pagos eliminados visibles en el horizonte común; reducción neta de intereses y plazo)               │
└───────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                  ▲
                        INTERACCIÓN DIRECTA: SELECCIÓN DE MES Y LOTE DRAFT
```

### 2.1 Qué Importa Antes de Qué Mide (Dominancia del Feature Principal)
Siguiendo lo que el product brief lidera:
1. **El elemento dominante de la pantalla es la Comparación de Escenarios (`ScenarioComparison`)**: El impacto financiero (cuánto dinero menos sale del bolsillo, cuántos meses antes se termina de pagar) precede a la minucia contable.
2. **Escala Temporal y Eje Monetario Idénticos**: Ambos gráficos comparten de forma estricta los mismos meses y el mismo techo monetario (`ceiling`) para que la desaparición visual de barras en meses futuros transmita inmediatamente la extinción de la deuda.
3. **Imputación Estricta de 6 Etapas**:
   - Etapa 1: Seguros pendientes acumulados de meses omitidos.
   - Etapa 2: Seguros corrientes del mes activo.
   - Etapa 3: Intereses vencidos acumulados.
   - Etapa 4: Interés corriente sobre saldo deudor.
   - Etapa 5: Capital vencido pendiente.
   - Etapa 6: Capital habitual programado de la cuota.
   - *Excedente de abono*: Destinado exclusivamente a reducir saldo de capital (*reduce-term*).

---

## 3. Identidad Visual, Tokens y Sistema de Diseño

El lenguaje visual de Finsim proyecta la atmósfera de un **Laboratorio Financiero Cálido, Natural y Transparente**, alejándose del azul corporativo frío bancario mediante una paleta de verdes de pino y bosque, acentos lima enérgicos y superficies de papel cálido.

### 3.1 Paleta de Color y Semántica Financiera

```
Paleta Institucional:
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   BrandForest   │ │    BrandDark    │ │    BrandLime    │ │  BrandLimeDark  │
│     #173F32     │ │     #203D33     │ │     #D7E8A3     │ │     #839954     │
│   Primario/CTA  │ │  Texto Principal│ │  Acento Secund. │ │   Dot/Subtítulo │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘

Semántica de Cargos y Flujos Financieros:
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   BrandBlue     │ │   BrandTeal     │ │   BrandAmber    │ │   BrandPlum     │
│     #2563A6     │ │     #188570     │ │ #B45309/#C75A25 │ │ #6B4B82/#8B62AC │
│ Capital Habitual│ │ Extra/Recuperac.│ │    Intereses    │ │     Seguros     │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘

Superficies, Estados y Alertas:
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ BackgroundWarm  │ │   SurfaceWarm   │ │    BrandSage    │ │    BrandRed     │
│     #F6F5EE     │ │    #FFFFFEF9    │ │     #285C38     │ │     #A53225     │
│ Fondo Alabastro │ │ Superficie Panel│ │  Éxito / Ahorro │ │  Error / Mora   │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
```

#### Especificación de Tokens de Color
- `--green` / `BrandForest`: `#173F32` (RGB: 23, 63, 50). Botones principales, encabezados de marca, pestañas activas.
- `--dark` / `BrandDark`: `#203D33` (RGB: 32, 61, 51). Color principal de lectura tipográfica de alto contraste.
- `--lime` / `BrandLime`: `#D7E8A3` (RGB: 215, 232, 163). Acento fresco, fondo de botones secundarios.
- `--lime-dark` / `BrandLimeDark`: `#839954` (RGB: 131, 153, 84). Punto distintivo del logo `finsim.`, anillos de foco `:focus-visible` (`#799634`), subtítulos destacados (`#607848`).
- `--muted` / `TextForestMuted`: `#617168` (RGB: 97, 113, 104). Texto secundario, subtítulos, etiquetas *eyebrow*, leyendas de gráficos.
- `--line` / `BorderSage`: `#DCE3D8` (RGB: 220, 227, 216). Bordes de paneles, divisores de tablas, separadores de sección.
- `BorderInput`: `#CBD5C6` (RGB: 203, 213, 198). Contorno de campos de formulario.
- `BackgroundWarm`: `#F6F5EE` (RGB: 246, 245, 238). Fondo general de la aplicación (papel alabastro cálido).
- `SurfaceWarm`: `#FFFFFEF9` (RGB: 255, 254, 249). Superficie de tarjetas y paneles con elevación visual tenue.
- `SurfaceHighlight`: `#F0F5EC` (RGB: 240, 245, 236). Resalte suave para tarjetas KPI principales.
- `Badge Sage` / Éxito: Fondo `#EAF0DF`, texto `#365339` (o `#285C38`). Para «Simulación completa» y deltas favorables.
- `Badge Warning` / Alerta: Fondo `#F9E8BC`, texto `#6B4B0B`. Para «Simulación incompleta» y advertencias de mora.
- `Error`: `#A53225` / `#B64030`. Para mensajes de validación y campos inválidos (`aria-invalid="true"`).

### 3.2 Escala Tipográfica y Tokens de Texto
- **Pila Tipográfica:** `Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`.
- **Comportamiento Numérico Obligatorio:** `font-variant-numeric: tabular-nums` en todas las tablas, KPIs, importes y gráficos para evitar saltos visuales al cambiar dígitos.
- **Jerarquía:**
  - **Hero Display (H1):** `clamp(2rem, 5vw, 3.8rem)`, interlineado `1.1`, espacio entre letras `-0.055em`, grosor `650`.
  - **Marca / Logotipo:** `1.5rem` (`22sp`), espacio entre letras `-0.06em`, grosor `750` / `Black`.
  - **Eyebrow (Sobre-título):** `0.7rem` (`10sp`), mayúsculas, espacio entre letras `0.16em` (`0.5sp`), grosor `750`, color `--muted`.
  - **Títulos de Sección (H2):** `1.35rem`, espacio entre letras `-0.025em`, grosor `650`.
  - **Subtítulos y Encabezados de Módulo (H3):** `1.05rem`, grosor `650`.
  - **Valores Destacados KPI (DD):** `clamp(1rem, 1.6vw, 1.4rem)`, espacio entre letras `-0.03em`, grosor `650`.
  - **Cuerpo y Controles de Formulario:** `0.85rem` – `1rem`, interlineado `1.5`.
  - **Tabla Mensual de Amortización:** `0.76rem` (`12sp`), compacto para máxima densidad de información.
  - **Notas y Disclaimers:** `0.75rem` – `0.80rem`, color `--muted`.

### 3.3 Espaciado, Retículas y Superficies
- **Contenedor Principal (`.shell`):** Ancho máximo de `1440px`, centrado, con relleno dinámico `2rem clamp(1rem, 4vw, 4rem) 4rem`.
- **Paneles (`.panel`):** Radio de borde `20px` (escritorio), `14px` (móviles `< 560px`), borde de `1px solid var(--line)`, relleno `clamp(1.2rem, 3vw, 2rem)`.
- **Objetivos Táctiles Accesibles:** Altura mínima de `44px` / `45px` en todos los botones, selectores y campos de entrada.
- **Retícula de Formulario (`.form-grid`):**
  - Escritorio / Tablet (`>= 600dp`): 2 columnas fluidas (`repeat(2, minmax(0, 1fr))`) con separación de `1.2rem 1.5rem`.
  - Móvil (`< 600dp`): 1 columna apilada para evitar truncamiento de etiquetas y selectores.
- **Retícula de Resumen (`.summary-grid`):**
  - Escritorio: 4 columnas (`repeat(4, minmax(0, 1fr))`).
  - Tablet (`<= 900px`): 2 columnas.
  - Móvil (`<= 560px`): 2 columnas compactas con separación reducida.

---

## 4. Estructura de la Interfaz y Flujo de Pantalla

La página se organiza de acuerdo a la lógica natural de resolución de dudas de la persona, ordenando la experiencia en capas de abstracción progresiva:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│ CABECERA: Marca finsim. · Badge «Tu laboratorio financiero»                      │
├──────────────────────────────────────────────────────────────────────────────────┤
│ HERO: «MENOS INCERTIDUMBRE. MÁS PERSPECTIVA. / Decide con los números claros.»   │
│       Nota lateral: «Un pequeño cambio hoy. Una historia distinta mañana.»       │
├──────────────────────────────────────────────────────────────────────────────────┤
│ PESTAÑAS: [Explorar préstamo]  [Convertir tasas]  [Comparar extracto]            │
├──────────────────────────────────────────────────────────────────────────────────┤
│ BARRA DE ESCENARIO: Nombre del escenario · Moneda y Plazo                        │
│                     [Usar ejemplo] [Importar] [Exportar] [Guardar escenario]     │
├──────────────────────────────────────────────────────────────────────────────────┤
│ 1. PUNTO DE PARTIDA (LoanForm): Parámetros base del crédito y seguros            │
├──────────────────────────────────────────────────────────────────────────────────┤
│ 2. EL PANORAMA (LoanSummary): Cuota base, Liquidación, Intereses, Caja total     │
├──────────────────────────────────────────────────────────────────────────────────┤
│ 3. EL CORAZÓN DEL PRODUCTO: COMPARACIÓN DE ESCENARIOS (ScenarioComparison)       │
│    - Deltas con signo: Intereses (+/-), Seguros (+/-), Caja (+/-), Plazo (+/-)   │
│    - Gráfico 1: Barras apiladas Escenario Original                               │
│    - Gráfico 2: Barras apiladas Escenario Modificado (con línea de mes activo)   │
│    - Selector de mes: «Mes para editar»                                          │
│    - Editor de Eventos en Lote (EventEditor): Borrador atómico                   │
├──────────────────────────────────────────────────────────────────────────────────┤
│ 4. EL DETALLE CONTABLE (MonthlyTable): Cronograma mes a mes                      │
│    - Selector: Modificado vs Original                                            │
│    - Acordeón colapsable en móvil / Tabla con encabezado fijo                    │
├──────────────────────────────────────────────────────────────────────────────────┤
│ PIE DE PÁGINA: Descargo educativo, orden de imputación y aviso de datos locales  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Catálogo Canónico de Contenido, Etiquetas, Copias y Mensajes

Toda etiqueta, texto y mensaje de error en Finsim proviene de las fuentes de verdad del repositorio (`docs/product-brief.md`, `src/App.tsx`, `LoanForm.tsx`, esquemas Zod en `scenario.ts` y contratos en `:shared`):

### 5.1 Textos de Cabecera, Hero y Pie
- **Título de pestaña navegador (`index.html`):** `Finsim · Decisiones con números`
- **Distintivo superior (`badge`):** `Tu laboratorio financiero`
- **Etiqueta superior Hero (`eyebrow`):** `MENOS INCERTIDUMBRE. MÁS PERSPECTIVA.`
- **Encabezado principal Hero (H1):** `Decide con los números claros.`
- **Bajada descriptiva Hero:** `Explora tu préstamo, cambia un mes y entiende qué pasa con tu dinero.`
- **Nota Hero lateral:** `Un pequeño cambio hoy. Una historia distinta mañana.`
- **Metadato Hero:** `{moneda} · pagos mensuales · tasa fija`
- **Descargo de responsabilidad en pie de página:**
  > «**Un espacio para explorar, no una promesa de tu banco.** Modelo educativo mensual. Los extras se aplican después del pago habitual; primero seguros, luego intereses y capital. Las omisiones no generan penalidades ni intereses sobre cargos pendientes.  
  > Los escenarios se guardan solo en este navegador. Exporta una copia para conservarlos o llevarlos a otro dispositivo.»

### 5.2 Barra de Herramientas del Escenario (`scenario-toolbar`)
- **Etiqueta:** `ESCENARIO ACTUAL`
- **Nombre por defecto (`exampleScenario`):** `Ejemplo · Crédito de libre inversión`
- **Acciones:**
  - `Usar ejemplo`: Restablece las condiciones al ejemplo canónico precargado.
  - `Importar`: Abre el selector de archivo (`accept="application/json,.json"`).
  - `Exportar`: Descarga `finsim-escenario.json`.
  - `Guardar escenario`: Almacena el escenario en el almacenamiento local del navegador.
- **Aviso de bloqueo por borrador pendiente:** `Hay cambios de meses pendientes. Aplica o descarta el lote en «Editar meses» antes de guardar, exportar o reemplazar el escenario.`
- **Confirmaciones de estado:**
  - `Escenario actualizado. Guarda para conservarlo en este navegador.`
  - `Escenario guardado en este navegador.`
  - `Copia JSON exportada.`
  - `Escenario importado. Guarda para conservarlo en este navegador.`

### 5.3 Formulario de Parámetros del Crédito (`LoanForm`)
- **Encabezado:** Eyebrow `Punto de partida` · Título `Tu préstamo` · Badge `{moneda} · tasa fija`.
- **Campos:**
  1. `Nombre del escenario`: Texto hasta 120 caracteres.
  2. `Moneda`: Selector (`COP`, `ARS`, `USD`, `EUR`). Texto de ayuda: *«Cambiar la moneda conserva los importes. No hacemos conversión cambiaria.»*
  3. `¿Desde dónde empezamos?`: Selector entre `Préstamo original` y `Saldo actual al día`.
     - *Nota explicativa para saldo actual:* *«El saldo representa capital al inicio del próximo periodo. Suponemos que estás al día; no incluye atrasos previos.»*
  4. `Capital inicial ({moneda})` o `Capital pendiente ({moneda})`: Input numérico decimal con coma. Ayuda: *«Ej.: 20.000.000,00. Máximo dos decimales.»*
  5. `Mes del primer pago simulado`: Input tipo `month` (`YYYY-MM`).
  6. `Tipo de tasa`: Selector:
     - `Efectiva anual (EA)`
     - `Efectiva mensual (EM)`
     - `Nominal anual vencida`
  7. `Tasa (%)`: Input decimal con coma. Ayuda: *«Ej.: 18 significa 18 %. Usa coma decimal.»*
  8. `Capitalizaciones por año` (si la tasa es nominal): Ayuda: *«12 = mensual; 4 = trimestral; 1 = anual.»*
  9. `Plazo (meses)` o `Plazo restante (meses)`: Entero entre 1 y 1.200 meses.
  10. `Cuota conocida ({moneda}, opcional)`: Placeholder: *«Calcular según el plazo»*. Ayuda: *«Solo capital e intereses. El seguro se suma aparte.»*
- **Sección de Seguros Mensuales (`insurance-list`):**
  - Leyenda: `Seguros mensuales`
  - Subtítulo: *«Los porcentajes se calculan sobre el capital al inicio del mes, antes de pagar.»*
  - Campos por seguro: `Nombre`, `Tipo de cargo` (`Importe fijo ({moneda})` o `Porcentaje mensual (%)`), `Importe mensual ({moneda})` / `Porcentaje mensual (%)`, `Final del seguro` (`Termina al liquidar el préstamo` o `Continúa hasta un mes definido`), `Último mes de cobro (inclusive)`.
  - Botones: `+ Añadir seguro` (límite operativo: 30 seguros), `Quitar seguro {n}`.

### 5.4 Resumen del Crédito (`LoanSummary`)
- **Encabezado:** Eyebrow `El panorama` · Título `Tu préstamo en cifras`.
- **Badges de estado:** `Simulación completa` (verde) o `Simulación incompleta` (ámbar de advertencia).
- **Advertencia de truncamiento:** *«No se alcanza la liquidación dentro del horizonte calculado. Los importes son acumulados parciales, no costos definitivos.»*
- **Métricas:**
  - `Cuota base mensual`: Subtexto *«Capital e intereses; seguro aparte»*.
  - `Liquidación del préstamo`: Muestra mes exacto y fecha (`YYYY-MM`), o *«Sin liquidación»*.
  - `Intereses pagados` (o *«Intereses pagados hasta aquí»* si es incompleta).
  - `Seguros pagados`: Subtexto indicando último cargo (`Último cargo: mes N`).
  - `Capital pagado`: Amortización total acumulada.
  - `Dinero total pagado` (o *«Dinero pagado hasta aquí»*): Subtexto *«Incluye recuperación y extras aplicados»*.
  - `Deuda pendiente`: Saldo total restante.
  - `Dinero ofrecido no aplicado`: Subtexto *«No se cuenta como dinero pagado»*.

### 5.5 Comparación de Escenarios y Gráficos (`ScenarioComparison`)
- **Encabezado:** Título `Original y modificado`.
- **Regla de interpretación:** *«Diferencias = modificado − original. Signo positivo: más costo o más tiempo; negativo: menos. Los extras conservan la cuota y reducen plazo.»*
- **Tarjetas de diferencias con signo (`comparison-deltas`):**
  - `Diferencia de intereses pagados` (ej. `-COP 2.450.000,00`)
  - `Diferencia de seguros pagados`
  - `Diferencia de caja pagada`
  - `Diferencia de plazo` (ej. `-8 meses`)
- **Series de Gráficos de Barras Apiladas:**
  - `Capital habitual` (Azul: `#2563A6`)
  - `Capital extra y recuperación` (Verde azulado: `#188570`)
  - `Intereses pagados` (Ámbar cálido: `#C75A25`)
  - `Seguros pagados` (Ciruela: `#8B62AC`)
- **Línea de Referencia:** Mes activo seleccionado (`stroke="#111827"`).

### 5.6 Editor de Eventos en Lote (`EventEditor`)
- **Comportamiento UX (2026-09-20 / FS-14):** Permite añadir, modificar o eliminar eventos en múltiples meses sin recalcular inmediatamente, evitando saltos de pantalla hasta que la persona presiona explícitamente `Aplicar cambios al escenario` o `Descartar cambios`.
- **Tipos de Eventos soportados:**
  1. `Abono extraordinario único` (`extra`): Campo de importe monetario mayor a cero.
  2. `Abono extraordinario recurrente` (`recurring`): Campos de importe, frecuencia de meses y mes final.
  3. `Omitir cuota habitual` (`missed`): No cobra la cuota corriente; genera atrasos de seguro e interés sin penalidad usuraria.
  4. `Pago de recuperación` (`catchup`): Cuota extraordinaria orientada a amortizar atrasos y regularizar el crédito.
- **Eventos no aplicados:** Lista colapsable si existen abonos prescritos para meses posteriores a la liquidación anticipada del préstamo.

### 5.7 Cronograma de Amortización Mensual (`MonthlyTable`)
- **Selector de modo:** `Escenario modificado` vs `Escenario original · sin eventos`.
- **Columnas de la tabla (con formato `tabular-nums`):**
  1. `Mes` (Índice y fecha `YYYY-MM`)
  2. `Capital inicial`
  3. `Interés del mes`
  4. `Seguro del mes`
  5. `Pago habitual`
  6. `Recuperación`
  7. `Abono extra`
  8. `Capital amortizado`
  9. `Capital vencido`
  10. `Interés pendiente`
  11. `Seguro pendiente`
  12. `Atrasos acumulados`
  13. `Deuda total`
  14. `Saldo final de capital`
- **Interacción de selección:** Hacer clic en cualquier fila de la tabla enfoca automáticamente el mes en el editor de eventos y sincroniza los gráficos.

### 5.8 Reglas y Mensajes de Error Literales (Validación de Esquema)
- *«Usa como máximo dos decimales.»*
- *«Usa un número no negativo con coma decimal.»*
- *«El importe debe ser mayor que cero.»*
- *«El número debe ser finito.»*
- *«El máximo admitido es 1.000.000.000.000.000 en la moneda seleccionada.»*
- *«Entre 1 y 1.200 meses.»*
- *«Usa año-mes válido.»*
- *«Los identificadores no deben repetirse.»*
- *«El mes final debe ser igual o posterior al inicial.»*
- *«El archivo supera el máximo de 2 MB.»*
- *«Aplica o descarta los cambios de meses antes de importar.»*

---

## 6. Arquitectura de Navegación e Integración Multiplataforma

```
                     ┌────────────────────────────────────────┐
                     │             Pestañas (Tabs)            │
                     │  [Préstamo]  [Conversor]  [Extracto]   │
                     └───────────────────┬────────────────────┘
                                         │
         ┌───────────────────────────────┼──────────────────────────────┐
         ▼                               ▼                              ▼
┌──────────────────┐           ┌──────────────────┐           ┌──────────────────┐
│ Explorar         │           │ Convertir        │           │ Comparar         │
│ Préstamo         │           │ Tasas            │           │ Extracto         │
│ (Loan Feature)   │           │ (RateConverter)  │           │ (Reconciliation) │
├──────────────────┤           ├──────────────────┤           ├──────────────────┤
│ - LoanForm       │           │ - Conversor EA,  │           │ - Carga de filas │
│ - LoanSummary    │           │   EM y Nominal   │           │   de extracto    │
│ - Comparison     │           │ - Frecuencia     │           │ - Cuadre de sal- │
│ - EventEditor    │           │   explícita      │           │   dos y cargos   │
│ - MonthlyTable   │           │ - Ejemplo con    │           │ - Diferencias    │
│                  │           │   fórmulas       │           │   sin auto-ajuste│
└──────────────────┘           └──────────────────┘           └──────────────────┘
```

1. **Pestaña «Explorar préstamo» (Principal):** El simulador y comparador integral descrito en este documento.
2. **Pestaña «Convertir tasas» (`RateConverter`):** Herramienta matemática independiente que permite transformar entre Efectiva Anual (EA), Efectiva Mensual (EM) y Nominal Anual con periodicidad configurable (`EA = (1+EM)^12 - 1`), manteniendo una tolerancia de error inferior a `1e-12`.
3. **Pestaña «Comparar extracto» (`Reconciliation`):** Módulo de auditoría donde la persona introduce filas de un extracto bancario real para contrastarlas contra el cronograma teórico, evidenciando desfases sin maquillar ni ajustar automáticamente el motor.

---

## 7. Directrices de Implementación y Continuidad para Agentes

Al implementar o modificar cualquier elemento visual o funcional en Finsim:
1. **Conserva la Fidelidad Semántica:** No sustituyas los términos acordados (*Abono a capital*, *Caja pagada*, *Cuota habitual*, *Atrasos*, *Capital vencido*) por sinónimos genéricos de banca internacional.
2. **Invarianza Aritmética:** Cualquier cambio de diseño debe garantizar que los importes continúen calculándose mediante `:shared` o `decimal.js` con redondeo *half-up*; nunca uses `Math.round` ni tipos `float` primitivos para representar dinero o tasas.
3. **Validación tras Cambios:** Toda modificación en código Kotlin o TypeScript debe validarse ejecutando:
   - TypeScript / Web: `npm run typecheck && npm test`
   - Kotlin Multiplatform: `./gradlew :shared:jvmTest` (o `cargo test` si se tocase código Rust según la memoria global).
