# ISSUE-34 — Resumen previo a revisión

2026-10-20. Alcance canónico: [#34](https://github.com/jovillarrealm/finsim/issues/34), [Kotlin compatible con LiteRT-LM](https://github.com/jovillarrealm/finsim/issues/34#issuecomment-6301601178) y [política móvil de #37](https://github.com/jovillarrealm/finsim/issues/37#issuecomment-6293230094).

## Código revisable

Implementación `a001654`, desde `523f91d` sobre código publicado `839c69c`. Incorpora integración `e96592d` mediante `97242848` (#36, tests) y `fe291ae` mediante `16bca760` (solo documentación). Para aislar #34, comparar la rama con `fe291ae`. El tablero/SPEC37 siguen bajo coordinación; esta implementación edita su ficha y este informe.

`androidApp` contiene aplicación, identidad, manifiesto, actividad, iconos y firma. `composeApp` y `shared` usan `com.android.kotlin.multiplatform.library`; conservan Android/Wasm, más JVM/JS en shared. Las entradas Android se mueven sin cambios (renombres 100%). No cambia motor, UI, serialización ni ubicación de datos. El catálogo fija AGP 9.1.1 y Kotlin 2.4.21; Compose 1.9.3 se conserva tras compilar Android y Wasm. No se amplía #35. Se elimina el parche R8 de AGP 8 y las supresiones de compatibilidad antiguas. CI/instrucciones usan Gradle 9.3.1, SDK 37.0 y tareas/rutas nuevas.

El APK declara `io.github.jovillarrealm.finsim_alpha`, launcher `com.finsim.app.MainActivity`, versión 1.0.3/código 4, compileSdk 37, minSdk 26 y targetSdk 36. Shared conserva minSdk 24; preferences `finsim_prefs` y clave `finsim.scenario` no cambian. La firma lee el mismo archivo ignorado `kmp/keystore.properties` y el keystore existente; no se publica ni copia el keystore.

Fuentes primarias: [AGP 9.1.1 y requisitos](https://developer.android.com/build/releases/agp-9-1-0-release-notes), [compatibilidad Kotlin/Gradle/AGP](https://kotlinlang.org/docs/gradle-configure-project.html), [plugin Android KMP y separación de aplicación](https://developer.android.com/kotlin/multiplatform/plugin). La tabla Kotlin 2.4.20–2.4.21 cubre Gradle 9.3.1 y AGP 9.1.1.

## Ejecución y evidencia

Worktree: `C:/Users/Lenovo/.codex/worktrees/spec37-issue34/finsim`. Java 21.0.12 se seleccionó solo para cada proceso. Builds con un trabajador, heap 2 GiB y compilador Kotlin en proceso; sin builds pesados simultáneos. Los logs completos y reportes XML son locales, no se confunden con resultados previos.

Reproducir con Gradle 9.3.1 en PATH, Java 21, SDK Platform 37.0 y Build Tools 36.0.0:

```sh
gradle -p kmp :shared:jvmTest :composeApp:testAndroidHostTest :androidApp:assembleDebug --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
gradle -p kmp :shared:jvmTest :composeApp:testAndroidHostTest :androidApp:assembleRelease :composeApp:wasmJsBrowserDistribution --no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
npm run typecheck
# Servir la distribución Wasm nueva, fijar FINSIM_WEB_URL a esa URL y ejecutar npm test.
```

| Comprobación | Resultado y alcance | Evidencia en `.scratch/issue34/` |
| --- | --- | --- |
| Primer build debug/tests | PASS, 115 shared y 8 host antes de incorporar #36; APK debug, 78 tareas/6m13s | `build-android-tests.log`; XML en `shared/build/test-results/jvmTest` y `composeApp/build/test-results/testAndroidHostTest` |
| Build final en `97242848` | PASS, APK release firmado y Wasm de producción; 140 tareas/10m08s; 115 shared del resultado válido de esta misma toolchain y 5 host recompilados/ejecutados tras #36 | `final-build.log`; XML de cada módulo |
| LiteRT-LM 0.18.0 / metadata 2.4 | PASS, compilador Kotlin 2.4.21/JVM 21 genera bytecode que invoca `ConversationConfig(enableResponseFormat = true, enableSpeculativeDecoding = false)` del JAR real; sin ignorar metadata | `litert-compatibility.log`, `litert-compat-classes/LiteRtCompatibilityKt.class` |
| APK release / firma | PASS, R8 9.1.31; apksigner exit 0, firma v2 válida y certificado igual al del AAB local previo | `release-signature.log`, `prior-aab-certificate.log`, `release-manifest.log` |
| `npm ci` / `npm run typecheck` | PASS; npm informa 2 avisos existentes (1 moderado/1 alto); sin actualización fuera de alcance | `npm-ci.log`, `typecheck.log` |
| Los 13 browser existentes | PASS, 13/13, sin omitidos/fallos, 249,55 s; desktop/360px, teclado, transferencias, portapapeles y resultados | `browser.log`, `served-version.json`, `wasm-manifest.json` |
| Launch Android emulado | BLOQUEADO antes de instalar: AVD API36 nuevo no completó inicio y ADB quedó unauthorized en dos intentos; no es un fallo observado del APK | `android-launch.log`, `emulator.log`, `emulator-retry.log`, `avd-screens/Screenshot_1792532553.png` |

El primer intento del probe CLI falló por classpath incompleto (coroutines ausente). Se corrigió el classpath y compiló el mismo código. JAR de clases LiteRT-LM: SHA256 `0ff0098f64407752c115bf2569a7cd3005c894ecb934e7fdbcd744037f398ce8`. La compilación no acredita inferencia ni incorpora el runtime en el APK de #34.

La web se sirvió exclusivamente desde la distribución nueva en `http://127.0.0.1:4178/finsim/`. `version.json` identificó fuente `97242848aca3228064708177288c12b240ecea45`; SHA256 del JS servido coincidió con el local antes de correr la suite: `dde6fa9e8c8cf13581e74911bc451a30812ff7f6a3769325564f1935172df9ef`. Wasm app (`9c5d270777b84ddf0d6b.wasm`): `730f4bf73e3880b31e313ca98d3f2c16f37c19e2338ee6e6cac8bcc37a18b6c1`. Chrome y servidor propio se cerraron al terminar. Los tests existentes incluyen algunos clics forzados; este resultado acredita regresión de esos flujos, sin certificar accesibilidad completa ni hardware móvil.

## Artefactos preservados

APK release: `kmp/androidApp/build/outputs/apk/release/androidApp-release.apk`, 1.896.306 bytes, SHA256 `c22ac17ee92887fe119e0d75a73e564d19a47ac4ebcb112d97c15cece72120bc`. Certificado SHA256 `2a5a930a15b4fe341a43017bff77fd1409b581e5ce7706774bbdbd4398f4f68c`.

APK debug: `kmp/androidApp/build/outputs/apk/debug/androidApp-debug.apk`, 12.387.707 bytes, SHA256 `7613452874b18382d8997b2976e4c9822c7381f6a797744a920d207c14271742`.

Copia controlada para #44: `C:/Users/Lenovo/AppData/Local/Temp/finsim-spec37/issue34-artifacts/`, con `finsim-issue34-1.0.3-4-release.apk`, `identity.json` (fuente, hash, certificado, versiones, límites) y `release-signature.log`. Fuente del build: `97242848aca3228064708177288c12b240ecea45`. La comparación de certificado con el AAB local anterior no acredita su código ni retención de datos al actualizar.

## Criterios sin evidencia completa

El criterio de build/launch tiene APK y Web compilados, tests y Web en ejecución, pero **launch Android BLOQUEADO**. El criterio de identidad/firma/datos conserva configuración y certificado, pero **actualización firmada con escenario guardado NO EJECUTADA**. Ninguno se presenta como aprobado completo. Los restantes cambios de configuración, estructura, instrucciones y evidencia están entregados para revisión; CI remoto aún no se ejecuta.

El AVD `issue34` se creó en `.scratch/issue34/avd`, aislado de los datos de `medium_phone` y del teléfono. Se probaron dos arranques con API36/x86_64, WHPX, dos cores y 1536 MiB. El segundo usó la opción documentada `-skip-adb-auth` y un adb-path explícito; siguió sin autorización durante la inicialización. La captura muestra la pantalla de inicio Google; los logs indican que activity todavía no estaba disponible. No se instaló ni abrió la app. El AVD se detuvo por su consola al terminar; no se reinició el servidor ADB ni se manipuló el teléfono.

Para retomar launch: disponer de AVD que complete arranque y aparezca autorizado (o dispositivo sin edición en riesgo y con autorización de instalación/reinicio). Con el serial exacto: `adb -s SERIAL install -r kmp/androidApp/build/outputs/apk/release/androidApp-release.apk`, luego `adb -s SERIAL shell am start -W -n io.github.jovillarrealm.finsim_alpha/com.finsim.app.MainActivity`. Observar UI, actividad/pid y logcat antes de registrar PASS; no basta que el comando de start devuelva éxito.

## Límites y pendientes

- Actualización firmada desde APK anterior con escenario guardado: NO EJECUTADA. No hay todavía un APK anterior con procedencia acreditada para esa comparación; el teléfono conectado puede tener edición/documentos abiertos y permanece intacto. Para retomar: identificar APK anterior/certificado, guardar/exportar un escenario por UI, instalar el nuevo release con `adb -s SERIAL install -r APK`, relanzar con autorización y comparar por UI/exportación. No desinstalar ni borrar datos. El release controlado preservado sirve como base anterior para futuras actualizaciones.
- Corrida física, lector e inferencia real: NO EJECUTADAS. #32 conserva su verificación funcional propia. No se atribuye ejecución del runtime por haber compilado metadata.
- Shared JS conserva su destino configurado; su ejecución en navegador no se repitió. La web oficial comprobada es Wasm. No se acredita ejecución de todos los destinos por un test JVM/host.
- Quedan avisos existentes de API/interop experimental y deprecaciones al compilar, además de tamaño de bundle Webpack; no se suprimen ni se presentan como fallos. No se hace optimización de rendimiento ni actualización de dependencias fuera de #34.
- Publicación CI remota, Play Console, push, integración de la entrega en la rama coordinadora y cierre: no ejecutados por este implementador.

## Seguimiento físico final

Arranque y actualización firmada: PASS en dispositivo Samsung. Certificado conservado; APK base SHA256 `c22ac17ee92887fe119e0d75a73e564d19a47ac4ebcb112d97c15cece72120bc`, APK integrado `e07a495085d6df1235f5ef26ac1a5fb7be60e3d0eca3c2f7fa1c7ff18ced1956`. Mismo applicationId/versionCode4; instalación con reemplazo, sin desinstalar ni limpiar datos. El escenario distintivo guardado sobrevivió a reinicio previo, actualización y nueva exportación; comparación estructural completa aprobada. [Informe canónico y límites](https://github.com/jovillarrealm/finsim/issues/34#issuecomment-6320519464). Esto resuelve los criterios parciales anteriores de #34, sin cambiar la revisión de código ni atribuir PASS a #43/#44 completos.
