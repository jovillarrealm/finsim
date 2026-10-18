# ADR 0002: Incidente y Deuda por Duplicación de Pila Web (React en `src/` vs Compose WasmJs en `kmp/`)

## Estado
Aceptado / Incidente Registrado

## Fecha
2026-10-18

## Contexto del Incidente
Durante la ejecución de las tareas de adopción del rediseño visual de Stitch (Screens `268286d1` [Mobile] y `5b0f011c` [Desktop]), se incurrió en un **error arquitectónico grave de divergencia y duplicación de código**:

1. **La arquitectura canónica del proyecto** (acordada en [ADR 0001](0001-canonical-kmp-shared-engine.md), configurada en [`.github/workflows/pages.yml`](../../.github/workflows/pages.yml) y documentada en [`docs/tracking.md`](../tracking.md)):
   - Establece que la interfaz de usuario de Finsim debe ser desarrollada en **Compose Multiplatform** dentro de `kmp/composeApp/`.
   - El objetivo Android compila a DEX nativo (`:composeApp:assembleDebug` / `installDebug`).
   - El objetivo Web compila a WebAssembly vía Skiko/Canvas (`:composeApp:wasmJsBrowserDistribution`), el cual es el artefacto que GitHub Pages sirve en producción (`kmp/composeApp/build/dist/wasmJs/productionExecutable`).
   - La aplicación React original en `src/` (proveniente de las entregas FS-00 a FS-14) fue congelada y catalogada formalmente como `@deprecated`.

2. **La desviación cometida por el agente**:
   - Al recibir la instrucción de validar en navegador mediante Chrome DevTools MCP y en emulador Android, el agente inició el servidor de desarrollo de Vite sobre la raíz (`npx vite --host 127.0.0.1 --port 5173`), el cual sirve la aplicación legacy de **React / TypeScript (`src/`)**.
   - En lugar de compilar o previsualizar el objetivo Web WasmJs de Compose (`:composeApp:wasmJsBrowserDistribution`), el agente modificó y enriqueció componentes en **React / Tailwind** (`src/features/loan/ScenarioComparison.tsx`, `src/features/loan/LoanSummary.tsx`, `src/features/loan/MonthlyTable.tsx`, `src/App.tsx`, `src/styles.css`).
   - En paralelo, se desarrollaron componentes en **Kotlin Compose** (`kmp/composeApp/src/commonMain/kotlin/com/finsim/app/ui/components/`): `HorizontalComparisonChart.kt`, `LoanSummary.kt`, `MonthlyTable.kt`.
   - **Consecuencia**: Se construyeron dos aplicaciones web completas en paralelo, desaprovechando la premisa fundamental de Kotlin Multiplatform (código de UI único compartido entre Android y Web WasmJs) y reintroduciendo deuda técnica crítica por divergencia de UI.

## Consecuencias y Riesgos
- **Pérdida de palanca (Leverage)**: Toda mejora realizada en la web en React no beneficia a la web en WasmJs ni a Android, duplicando el costo de diseño, mantenimiento y pruebas.
- **Riesgo de divergencia de comportamiento y estética**: Los usuarios en Android / Wasm ven una interfaz (Compose) y quienes abren el servidor Vite de React ven otra, con posibles discrepancias en redondeos, animaciones o flujos de interacción.
- **Confusión en CI/CD**: Mientras CI en GitHub Actions despliega el binario `wasmJs` de KMP, las pruebas locales de desarrollo se estaban ejecutando y validando visualmente sobre la app de React.

## Decisiones y Mandatos de Remediación
1. **Reafirmación de la Pila Única**:
   - `kmp/composeApp` es la **única** interfaz oficial de Finsim para todos los entornos (Android y Web).
   - El objetivo Web oficial es `wasmJs` (`:composeApp:wasmJsBrowserDistribution`).
2. **Prohibición de Desarrollo en `src/`**:
   - Queda terminantemente prohibido crear nuevas características, vistas o componentes de presentación en `src/` (React / TypeScript).
   - Los archivos de `src/` quedan congelados exclusivamente como referencia histórica o banco de pruebas legado de regresión de Vitest mientras se completa su retiro.
3. **Retiro y Eliminación de `src/` Completados**:
   - Se eliminó completamente el directorio `src/` (React / TypeScript), sus pruebas vitest y las dependencias npm redundantes (`react`, `react-dom`, `recharts`, `@vitejs/plugin-react`).
   - La aplicación Kotlin Multiplatform Compose Web (`wasmJs`) y Android (`composeApp`) asume el 100% de la funcionalidad y de la estética Warm Botanical Precision.
4. **Protocolo para Validación en Navegador**:
   - Toda validación web en Chrome DevTools, Playwright o navegador real debe realizarse contra el binario Web de Compose:
     ```bash
     gradle -p kmp :composeApp:wasmJsBrowserDistribution
     npm run preview -- --port 4174 --outDir kmp/composeApp/build/dist/wasmJs/productionExecutable --base /finsim/
     ```
   - El script `npm run dev` ahora sirve la distribución WebAssembly de KMP.
