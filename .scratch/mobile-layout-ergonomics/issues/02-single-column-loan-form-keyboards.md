# 02: Formulario a 1 Columna en Móvil y Teclados Numéricos Nativos

**What to build:**
1. En `kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/components/LoanForm.kt`:
   - Detectar ancho de pantalla o usar diseño adaptativo: en pantallas móviles (< 600dp), los campos de entrada deben apilarse en 1 sola columna ocupando el 100% del ancho (igual que en la web original: `.form-grid { grid-template-columns: 1fr; }`), en lugar de forzar 2 columnas estrechas.
   - En pantallas amplias (>= 600dp), mantener el layout de 2 columnas.
2. Configuración de Teclados Nativos en Android/Compose:
   - Configurar `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)` para monto de capital, tasa de interés y cuota.
   - Configurar `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)` para el plazo en meses.
   - Asegurar que la acción de teclado no oculte botones ni campos críticos.

**Blocked by:** 01-tabs-and-scenario-toolbar.md

**Triage Label:** `ready-for-agent`
**Status:** completed

- [x] Formulario adaptativo: 1 columna en móvil (< 600dp), 2 columnas en pantallas anchas.
- [x] `KeyboardOptions` configurado para `Decimal` y `Number`.
- [x] Pruebas unitarias de `LoanConditionsDraft` pasando al 100%.
- [x] Compilación limpia en Android y WasmJs.
