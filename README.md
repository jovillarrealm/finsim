# Finsim

Simulador educativo de préstamos y comparación de ofertas construido sobre Kotlin Multiplatform (WebAssembly y Android).

## Desarrollo

Requiere Node.js compatible con las versiones fijadas en `package-lock.json`, Java 21 y Gradle 8.11.1.

```sh
npm ci
npm run dev
```

## Verificación

```sh
# Verificación de TypeScript y scripts
npm run typecheck

# Pruebas de integración de navegador KMP (Playwright)
npm test

# Pruebas unitarias de Kotlin Multiplatform (Shared + Compose App)
npm run test:kmp

# Compilación WebAssembly
npm run build
```

El servidor local abre `http://127.0.0.1:5173`. Para servir la distribución WebAssembly usa `npm run preview`.

## Alcance actual

Préstamos en COP, ARS, USD o EUR, originales o desde saldo actual, tasas EA/EM/nominal, seguros, abonos únicos/recurrentes, omisiones y recuperación. Compara dos cronogramas y edita eventos desde gráficos o tabla. Incluye conciliación manual y guardado/exportación/importación JSON versión 1. Los cálculos son educativos; no reproducen automáticamente las reglas de un banco.

La moneda es un parámetro del escenario: cambiarla conserva los importes, sin conversión cambiaria. Se muestran códigos explícitos y números con coma decimal. Los archivos anteriores sin moneda se interpretan en COP.

Los datos se guardan solo en el navegador. Exportar genera una copia portátil; importar valida antes de sustituir el escenario. Los resultados se recalculan desde las entradas.

## Publicación

Sitio: https://jovillarrealm.github.io/finsim/

`.github/workflows/pages.yml` se ejecuta en `push` a la rama `main` y soporta ejecución manual desde Actions. Construye la aplicación KMP con Java 21 y Gradle 8.11.1, ejecuta las pruebas shared/UI y los recorridos de navegador bajo `/finsim/` antes de desplegar. `version.json` identifica el commit publicado.

Para construir KMP directamente con Gradle: `gradle -p kmp :composeApp:wasmJsBrowserDistribution`; para verificar: `gradle -p kmp :shared:jvmTest :composeApp:testDebugUnitTest`.

Consulta [el plan](docs/implementation-plan.md), [el tracking](docs/tracking.md) y [las convenciones financieras](docs/financial-conventions.md) antes de modificar cálculos.
