# FS-18 — Experiencia de Resultados del Préstamo, Estética y Exportación Avanzada

Status: ready-for-agent
Triage Label: ready-for-agent
Fecha: 2026-10-17
Tracker: Markdown local, conforme al patrón existente del proyecto.
Alcance: Rediseño estético y de ergonomía visual, gráficos interactivos con leyendas inferiores y escala en eje Y, tabla comparativa mensual compacta, scroll fluido del cronograma y exportación dual (Apache Arrow IPC + CSV/TSV).

---

## Problem Statement

Una persona que utiliza Finsim para planificar y comparar escenarios de su préstamo se enfrenta a importantes barreras de legibilidad, ergonomía y reutilización de datos:

1. **Monotonía visual y falta de jerarquía:** La interfaz presenta una paleta de colores deslavada y plana (dominada por fondos beige/alabastro y grises de bajo contraste), lo que dificulta escanear visualmente las métricas críticas y diluye la identidad amigable de laboratorio financiero.
2. **Ruido visual en las acciones de copia:** Cada número monetario en el resumen, gráficos y detalle muestra de forma permanente y repetitiva el texto explícito `· Copiar`, saturando la vista y compitiendo con la lectura de las cifras.
3. **Gráficos distantes y con escasa señalización:**
   - Los controles de visibilidad (toggles de escenarios y componentes) se encuentran ubicados muy arriba, desconectados de los gráficos que manipulan por selectores de tramo y cajas de texto.
   - El eje vertical carece de escala numérica en el dibujo, relegando la magnitud a un texto plano superior (`Escala: COP 0 a COP...`), y no ofrece retroalimentación interactiva (tooltips o cursor de seguimiento) al pasar el cursor sobre los puntos.
   - El gráfico inferior de pagos mensuales por componente resulta estrecho y difícil de interpretar ante múltiples series simultáneas.
4. **Sobrecarga vertical en el detalle mensual:** Al seleccionar un mes, la interfaz despliega una lista vertical no estructurada de más de 18 filas consecutivas por escenario (36 en total), demandando un desplazamiento vertical excesivo de más de 800 píxeles para comparar el escenario original contra el modificado.
5. **Rigidez en la tabla de amortización y exportación limitada:** La tabla mes a mes se encuentra restringida a una paginación fija de 12 cuotas por bloque sin desplazamiento continuo, y no dispone de mecanismos de exportación columnar moderna (Apache Arrow) para analítica de datos en Python/Pandas/Polars ni exportación delimitada limpia para hojas de cálculo.

---

## Solution

Una experiencia integral de resultados y cronograma que combina alta ergonomía visual, interactividad analítica y exportación versátil:

1. **Paleta Esmeralda y Menta Financiera:** Fondo limpio y fresco (`#F8FAF8`), tarjetas blancas nítidas con bordes sutiles, verde bosque institucional (`#144436`) para títulos y elementos estructurales, y verde menta/esmeralda vivo (`#059669` / `#10B981`) para estados activos, curvas destacadas y confirmaciones.
2. **Acción de copia sutil y sin ruido:** Las cifras numéricas se muestran limpias en reposo. Al pasar el cursor (hover), emerge suavemente un icono de portapapeles; al hacer clic, el valor numérico sin formatear se transfiere al portapapeles del sistema y el icono se transforma temporalmente en una marca de verificación verde (**✓**) con confirmación visual sutil, manteniendo la semántica y accesibilidad para lectores de pantalla.
3. **Gráficos coordinados con leyendas integradas y eje Y rotulado:**
   - Toggles reubicados al pie de cada gráfico respectivo, operando como leyendas interactivas directas (Original y Modificado bajo el gráfico de Saldo; componentes con su muestra de color bajo el gráfico de Pagos).
   - Eje vertical izquierdo con 3 a 4 niveles de referencia numérica formateada de manera compacta (ej. `$20M`, `$10M`, `$0`).
   - Cursor de seguimiento vertical que, al mover el puntero o interactuar táctilmente, resalta el mes activo y despliega un tooltip flotante con el desglose exacto de las series visibles.
   - Altura del lienzo ampliada a 220dp para mayor claridad visual.
4. **Detalle del mes en tabla compacta a 3 columnas:**
   - Presentación estructurada `Métrica | Escenario Original | Escenario Modificado`.
   - Cifras fundamentales (Saldo de capital, Total pagado y componentes de cuota) visibles en un formato denso y de rápida comparación.
   - Acordeón colapsable discreto para conceptos contables y regulatorios secundarios (Comisión pagada, IVA pagado, Interés generado, Seguro generado, Deuda total, Mora, Excedente no aplicado).
5. **Cronograma con scroll continuo y exportación dual:**
   - Tabla mes a mes con desplazamiento vertical fluido (hasta 500dp) y desplazamiento horizontal con cabecera fija de columnas, eliminando la paginación rígida de 12 meses.
   - **Exportación Apache Arrow IPC (`.arrow`):** Archivo columnar nativo con tipos estrictos (`Int32`, `Utf8`, `Float64`) para importación instantánea en Python (Pandas/Polars) y DuckDB sin inconsistencias de tipos.
   - **Exportación CSV / TSV:** Archivo delimitado estándar con codificación UTF-8 con BOM para apertura inmediata sin desconfiguración de separadores en Excel y Google Sheets.

---

## User Stories

1. Como persona que explora un crédito, quiero una paleta de colores fresca y con buen contraste, para distinguir fácilmente títulos, tarjetas y series sin fatiga visual.
2. Como persona que consulta resultados, quiero ver cifras limpias sin la palabra 'Copiar' repetida permanentemente, para leer mis números sin saturación visual.
3. Como persona que pasa el cursor sobre una métrica, quiero ver aparecer un icono de portapapeles, para reconocer de inmediato que el valor es copiable.
4. Como persona que hace clic en copiar, quiero ver una marca de verificación verde instantánea en el lugar del icono, para tener confirmación visual inmediata de la copia.
5. Como persona que usa lector de pantalla o teclado, quiero que los botones de copia mantengan su nombre accesible completo, para operar de forma equivalente sin depender del cursor.
6. Como persona que copia un importe monetario, quiero recibir un valor numérico estandarizado con dos decimales, para pegarlo en hojas de cálculo o campos sin necesidad de eliminar símbolos monetarios a mano.
7. Como persona que analiza el gráfico de saldo, quiero ver los toggles de 'Original' y 'Modificado' directamente debajo del lienzo, para entender la leyenda y filtrar escenarios en el mismo punto de atención.
8. Como persona que analiza el gráfico de componentes, quiero ver los toggles de cada serie con su respectivo código de color justo al pie del gráfico, para identificar y alternar series sin buscar controles lejanos.
9. Como persona que evalúa magnitudes en los gráficos, quiero ver marcas de escala con valores compactos en el eje vertical izquierdo, para dimensionar el saldo y las cuotas sin leer textos descriptivos largos.
10. Como persona que mueve el cursor sobre los gráficos, quiero que una línea guía vertical y un tooltip flotante me muestren los importes exactos del mes señalado, para inspeccionar cifras precisas sin salir del gráfico.
11. Como persona que compara curvas, quiero que los lienzos tengan al menos 220dp de altura, para que las líneas de pagos múltiples no se encimen ni se vean comprimidas.
12. Como persona que inspecciona un mes concreto, quiero ver una tabla comparativa de 3 columnas (Métrica, Original, Modificado), para evaluar las diferencias entre ambos escenarios de un solo vistazo.
13. Como persona que revisa la cuota de un mes, quiero ver saldo, total pagado y componentes habituales inmediatamente visibles, para no tener que buscar entre filas secundarias.
14. Como persona que analiza aspectos regulatorios o atrasos, quiero un acordeón plegable para comisiones, IVA, mora y saldos pendientes, para acceder a datos avanzados sin saturar el espacio vertical cotidiano.
15. Como persona que consulta un préstamo de largo plazo (ej. 36 o 120 meses), quiero desplazarme verticalmente por la tabla completa sin pasar páginas de 12 en 12, para revisar la evolución temporal con fluidez.
16. Como persona que se desplaza hacia abajo en la tabla de amortización, quiero que la fila de encabezados permanezca visible en la parte superior, para saber siempre qué columna estoy leyendo.
17. Como analista de datos o usuario técnico, quiero exportar el cronograma en formato Apache Arrow IPC (`.arrow`), para cargarlo directamente en Python, Pandas, Polars o DuckDB con tipos numéricos nativos sin pérdidas de precisión.
18. Como persona usuaria de Excel o Google Sheets, quiero exportar el cronograma en formato delimitado con UTF-8 BOM, para abrir la tabla directamente sin que se alteren las tildes ni se confundan los separadores decimales.
19. Como persona que oculta todas las series de un gráfico, quiero ver un aviso explícito y un botón de restablecimiento, para recuperar la visualización rápidamente.
20. Como persona que usa dispositivos táctiles o móviles, quiero poder interactuar con los gráficos y tablas mediante toques precisos, para disponer de la misma capacidad de inspección que en escritorio.

---

## Implementation Decisions

### 1. Sistema de Diseño y Tokens de Color
- La paleta se centraliza en el tema visual de la aplicación:
  - Estructura y títulos: `BrandForest` (`#144436`) y `TextMain` (`#0F2922`).
  - Fondo general: `BackgroundClean` (`#F8FAF8`).
  - Superficies y contenedores: `SurfaceCard` (`#FFFFFF`) con bordes ligeros `BorderSubtle` (`#E2E8F0` / `#D1DCD0`).
  - Acentos interactivos y positivos: `BrandEmerald` (`#059669`) y `BrandMint` (`#10B981`) con contenedor tenue `BrandMintLight` (`#ECFDF5`).
  - Señalización de alertas y cargos: se preservan ámbar cálido para intereses y ciruela para seguros, con contraste refinado.

### 2. Módulo de Copia Discreta (Copy UX)
- Se desacopla la presentación visual del botón respecto a su nombre semántico:
  - El texto visible omite el sufijo `· Copiar`.
  - El contenedor reacciona al estado de puntero (hover) en escritorio, exhibiendo un glifo vectorial de portapapeles.
  - Al desencadenarse la copia, el icono conmuta a un check verde (`✓`) durante 1.500 ms y se restituye de forma autónoma.
  - El nodo de accesibilidad preserva inalterado el atributo semántico `contentDescription = "Copiar $label"` para garantizar total paridad en lectores de pantalla y pruebas automatizadas existentes.

### 3. Módulo de Gráficos de Resultados y Navegación
- Reestructuración del flujo vertical:
  1. Controles de navegación de tramo (Anterior, Siguiente, Selector de mes numérico).
  2. Gráfico 1: Saldo de capital al cierre (220dp).
  3. Leyenda inferior y toggles interactivos de Gráfico 1 (Filtro Original / Filtro Modificado).
  4. Gráfico 2: Pagos mensuales por componente (220dp).
  5. Leyenda inferior y toggles interactivos de Gráfico 2 (Filtros por componente con indicadores de color).
- Escala de Eje Y:
  - Generación de 3 a 4 niveles horizontales de referencia calculados dinámicamente a partir del valor máximo visible.
  - Rotulación alineada a la izquierda con cifras compactas legibles (ej. `$20M`, `$10M`, `$0`).
- Interactividad y Tooltip:
  - Detección de puntero sobre el lienzo para proyectar una línea vertical sobre el mes señalado.
  - Tarjeta flotante que enumera las series activas y sus importes exactos en el mes señalado.

### 4. Módulo de Detalle Mensual Compacto
- Sustitución de las 36 filas verticales individuales por una matriz tabular a 3 columnas:
  - Columna 1: Nombre del concepto.
  - Columna 2: Importe escenario Original con acción de copia integrada.
  - Columna 3: Importe escenario Modificado con acción de copia integrada.
- Sección principal visible: Saldo de capital, Total pagado, Capital habitual, Capital adicional, Intereses pagados, Seguros pagados, Otros cargos.
- Sección secundaria colapsable (Acordeón): Comisión pagada, IVA pagado, Interés generado, Seguro generado, Interés pendiente, Seguro pendiente, Capital vencido, Mora, Deuda total, Excedente no aplicado.

### 5. Módulo de Cronograma Mes a Mes y Motor de Exportación
- Visualización:
  - Contenedor con altura delimitada (hasta 500dp) con desplazamiento vertical y horizontal libre.
  - Fila superior de cabecera fijada (`sticky`) durante el desplazamiento vertical.
- Exportación Apache Arrow:
  - Definición de esquema columnar estricto:
    - `month`: Int32
    - `date`: Utf8
    - `opening_balance`, `scheduled_principal`, `extra_principal`, `interest_charged`, `insurance_charged`, `interest_paid`, `insurance_paid`, `other_charges_paid`, `total_paid`, `closing_balance`, `pending_interest`, `pending_insurance`, `overdue_principal`, `arrears`, `debt`, `unapplied`: Float64 (o Decimal128 serializado).
  - Web (Wasm): serialización de RecordBatch Arrow IPC Stream mediante puente JavaScript.
  - Android: serialización de VectorSchemaRoot mediante integración con el motor Apache Arrow.
- Exportación Tabular (CSV / TSV):
  - Serializador en Kotlin puro multiplataforma.
  - Generación de texto delimitado con cabecera en español, codificación UTF-8 con BOM (`\uFEFF`) para compatibilidad directa con Microsoft Excel y hojas de cálculo.

---

## Testing Decisions

### Criterios de Calidad de Pruebas
- Las pruebas deben evaluar comportamiento externo observable (renderizado de elementos accesibles, respuesta a eventos de puntero/clic, contenido copiado en el portapapeles y bytes/texto exportados), evitando acoplarse a detalles internos de implementación o estilos privados.
- El canal de pruebas prioritario y de mayor nivel de integración es el arnés de pruebas de navegador Playwright contra la distribución real de la aplicación compilada en Web Wasm.

### Módulos y Costuras a Evaluar
1. **Costura E2E de Navegador (Playwright / Chromium):**
   - Verificación de renderizado de la nueva paleta y visibilidad de controles.
   - Activación de acciones de copia en resumen, tabla y detalle mensual, validando la lectura real de `navigator.clipboard.readText()`.
   - Interacción con los toggles al pie de los gráficos y verificación de la actualización de series.
   - Apertura y cierre del acordeón de detalles secundarios en la tabla comparativa de 3 columnas.
   - Descarga y consistencia de los archivos exportados (`.arrow` y `.csv`/`.tsv`).
2. **Costura de Presentación y Serialización (Kotlin Multiplatform Test):**
   - Validación unitaria de la generación del esquema y los registros del exportador de Apache Arrow IPC.
   - Validación de consistencia del formateador CSV/TSV ante valores nulos, meses de gracia y valores con decimales.

### Arte Previo en el Repositorio
- Pruebas existentes en `kmp/composeApp/browser-tests/loan-results.test.cjs`.
- Pruebas de presentación en `kmp/composeApp/src/commonTest/kotlin/com/finsim/app/ui/components/`.

---

## Out of Scope

- Modificaciones a las fórmulas matemáticas del motor financiero (`:shared`), reglas de prelación de pagos o perfiles regulatorios.
- Sincronización multiusuario en la nube o almacenamiento en bases de datos externas.
- Alteraciones al código de la aplicación prototipo anterior basada en React (`src/`).
- Importación o lectura inversa de archivos Apache Arrow en la aplicación (el alcance contempla exclusivamente la exportación).

---

## Further Notes

- Se mantiene estricto apego al ADR-0001: el motor Kotlin Multiplatform es la única fuente de verdad para los cálculos financieros.
- Los cambios respetan el vocabulario canónico establecido en `GLOSSARY.md` (Cronograma de amortización, Imputación, Capital vencido, Mora, Escenario).
