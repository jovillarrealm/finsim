# 03: Cronograma de Amortización Colapsable (Acordeón) y Desplazamiento Fluido

**What to build:**
1. En `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/components/MonthlyTable.kt`:
   - Reemplazar el contenedor rígido siempre abierto por un acordeón colapsable (como en la web: `<details className="panel monthly-table">`):
     - En estado cerrado (por defecto en móvil), muestra una tarjeta limpia con el título `"Detalle mes a mes"`, el badge de cantidad de meses (ej. `"36 meses"`), un resumen explicativo y un botón/icono interactivo claro: `[Ver cronograma completo ▾]`.
     - Al pulsar el botón, se expande con animación suave (`AnimatedVisibility`).
   - Evitar el atrapamiento de gestos táctiles (*scroll trap*):
     - Cuando está expandido, no meter un `LazyColumn` anidado con scroll vertical dentro de otro `verticalScroll`. Renderizar las filas de forma acotada o con altura adaptable para que el scroll principal de la pantalla funcione naturalmente.
   - Desplazamiento horizontal fluido (`horizontalScroll`):
     - Cabecera fija y celdas con espaciado adecuado.
     - Indicador visual o sombra que indique que la tabla puede deslizarse horizontalmente.

**Blocked by:** 02-single-column-loan-form-keyboards.md

**Triage Label:** `ready-for-agent`
**Status:** ready

- [ ] Acordeón colapsable cerrado por defecto con botón `[Ver cronograma completo ▾]`.
- [ ] Transición fluida con `AnimatedVisibility`.
- [ ] Eliminación de conflictos de scroll anidado.
- [ ] Desplazamiento horizontal claro para las 9 columnas.
- [ ] Compilación y verificación con `./gradlew :shared:jvmTest`.
