# 04: Tipografía Fluida en Resumen KPI y Marcadores de Eje en Gráficos

**What to build:**
1. En `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/components/LoanSummary.kt`:
   - En las tarjetas KPI (`Cuota base`, `Liquidación`, `Total intereses`, `Caja total pagada`), implementar auto-escalado o tamaño de fuente responsive (ej. 14sp - 15sp con peso Bold y salto controlado) para que cifras largas en moneda (como `COP 25.750.306,00`) no toquen los bordes ni desborden.
   - Ajustar el padding interno de las tarjetas (de 12dp a 10dp en pantallas pequeñas) y asegurar espaciado respirable.
2. En `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/components/HorizontalComparisonChart.kt`:
   - Añadir marcas de lectura en los ejes del gráfico en canvas:
     - Eje horizontal: ticks de meses (ej. `M1`, `M12`, `M24`, `M36` o según la duración).
     - Eje vertical o cabecera: saldo inicial y saldo final legibles con el formato de moneda.
     - Leyenda clara y accesible (`Saldo Original`, `Saldo Modificado`, `Ahorro`).

**Blocked by:** 03-collapsible-monthly-table-accordion.md

**Triage Label:** `ready-for-agent`
**Status:** completed

- [x] Tipografía y paddings fluidos en `LoanSummary.kt` para importes largos.
- [x] Ticks de meses y marcas de escala en `HorizontalComparisonChart.kt`.
- [x] Pruebas y verificación con `./gradlew :shared:jvmTest`.
