# Finsim

Simulador educativo de préstamos y comparación de ofertas construido sobre Kotlin Multiplatform (WebAssembly y Android).

## Desarrollo

Requiere Node.js compatible con las versiones fijadas en `package-lock.json`, Java 21, Gradle 9.3.1 y Android SDK Platform 37.0 / Build Tools 36.0.0 para compilar Android. El catálogo fija AGP 9.1.1, Kotlin 2.4.21 y Compose Multiplatform 1.9.3.

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

El APK Android se genera con `gradle -p kmp :androidApp:assembleDebug` en `kmp/androidApp/build/outputs/apk/debug/androidApp-debug.apk`. La entrada, el manifiesto, los iconos y la firma Android están en `:androidApp`; la UI multiplataforma permanece en `:composeApp` y el motor en `:shared`, ambos con el plugin Android KMP de biblioteca. Para un APK o AAB firmado usa `:androidApp:assembleRelease` o `:androidApp:bundleRelease`; requieren el `kmp/keystore.properties` ignorado y el keystore existente. No se incluyen claves en CI. La aplicación conserva minSdk 26 y targetSdk 36; compileSdk 37 no modifica esa política.

## Alcance actual

Préstamos en COP, ARS, USD o EUR, originales o desde saldo actual, tasas EA/EM/nominal, seguros, abonos únicos/recurrentes, omisiones y recuperación. Compara dos cronogramas y edita eventos desde gráficos o tabla. Incluye conciliación manual y guardado/exportación/importación JSON versión 1. Los cálculos son educativos; no reproducen automáticamente las reglas de un banco.

La moneda es un parámetro del escenario: cambiarla conserva los importes, sin conversión cambiaria. Se muestran códigos explícitos y números con coma decimal. Los archivos anteriores sin moneda se interpretan en COP.

Los datos se guardan solo en el navegador. Exportar genera una copia portátil; importar valida antes de sustituir el escenario. Los resultados se recalculan desde las entradas.

## Publicación

Sitio: https://jovillarrealm.github.io/finsim/

`.github/workflows/pages.yml` se ejecuta en `push` a la rama `main` y soporta ejecución manual desde Actions. Construye con Java 21 y Gradle 9.3.1, verifica las pruebas shared/UI, genera el APK debug y la distribución Wasm, y ejecuta los recorridos de navegador bajo `/finsim/` antes de desplegar. `version.json` identifica el commit publicado.

Para construir KMP directamente con Gradle: `gradle -p kmp :composeApp:wasmJsBrowserDistribution`; para verificar: `gradle -p kmp :shared:jvmTest :composeApp:testAndroidHostTest`.

Consulta [el plan](docs/implementation-plan.md), [el tracking](docs/tracking.md) y [las convenciones financieras](docs/financial-conventions.md) antes de modificar cálculos.
