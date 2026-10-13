# 01: Pestañas de Navegación Limpias y Barra de Escenario Adaptable

**What to build:**
1. En `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/App.kt`:
   - Eliminar emojis que causan caracteres rotos (`[]` tofu boxes) en las pestañas (`💳 Préstamo`, `🔄 Conversor`, `📋 Conciliación`) y sustituirlos por texto limpio y tipografía de alto contraste: `Préstamo`, `Conversor`, `Conciliación`.
   - Limpiar el pie de página de caracteres Unicode no estándar (`seguros [] intereses [] capital`), usando separadores tipográficos estándar (`·`, `/`, `-`).
2. Barra de Escenario Adaptable (`scenario-toolbar`):
   - En pantallas compactas (< 600dp), apilar el encabezado del escenario: mostrar el nombre del escenario arriba (`Ejemplo · Crédito de libre inversión`) con el badge de moneda y plazo, y abajo colocar la fila de acciones (`[Guardar]`, `[Exportar]`, `[Ejemplo]`, `[Importar]`) con tamaño táctil cómodo y sin truncar texto.
3. Añadir soporte para detección de ancho de pantalla / `BoxWithConstraints` o `LocalWindowWidth` para adaptar la barra.

**Blocked by:** None

**Triage Label:** `ready-for-agent`
**Status:** ready

- [ ] Pestañas sin emojis rotos en `App.kt`.
- [ ] Pie de página sanitizado de caracteres Unicode incompatibles.
- [ ] Barra de herramientas adaptable para pantallas móviles (< 600dp).
- [ ] Compilación y verificación con `./gradlew :shared:jvmTest` y compilación de `composeApp`.
