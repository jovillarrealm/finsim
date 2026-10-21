# Issue #43 — perfil de verificación Android

- Responsable: `/root/issue43_implement`; coordinadora `/root`.
- Estado: checkpoint de implementación listo para revisión independiente; aceptación Android completa pendiente de un APK construido desde la integración de #40/#45. No cerrar issue.
- Rama/worktree: `codex/spec37-issue43` · `C:/Users/Lenovo/.codex/worktrees/spec37-issue43/finsim`.
- Base reservada: `e9317d83baaaebf0e3b4c4092e7543da916b3c1b`; #45 debe integrarse antes de entregar la punta.
- Alcance: runner Android KMP invocable, identidad APK instalada/candidata, dispositivo y densidad real, arranque, navegación observada, controles táctiles, foco/teclado, scroll y criterios financieros independientes. No modifica el producto Kotlin ni código React.

## Implementación

- `scripts/verify-device.mjs`: perfil Android basado en stdlib/ADB. Selecciona destino únicamente con `--device`; las acciones de UI necesitan `--allow-ui-actions`; `--restart-app` requiere ambos, hace force-stop/start y nunca borra datos. Por defecto no inicia la app. Comprueba todas las rutas `pm path` y compara los bytes instalados con `--apk`; lee los metadatos laterales únicamente si su hash coincide. Informa modelo, API, pantalla, densidad, MemTotal y PSI sin inferir elegibilidad ni rendimiento Gemma.
- Los gestos requieren una jerarquía reciente, bounds visibles y un ancla semántica observada. Cada página se desplaza como máximo una vez (préstamo, dos veces); después intenta recuperar navegación mediante un desplazamiento observado hacia arriba. Si no reaparece, se detiene sin tocar otro control. El presupuesto total es 16 dumps.
- Cifras de referencia son literales y provienen de `docs/financial-conventions.md` o de una prueba compartida identificada. Una referencia financiera solo puede pasar con acción ejecutada y cifra independiente observada; rótulos de pantalla no aprueban cálculos.
- `scripts/verify-device.test.mjs` cubre parsing de jerarquía/foco/bounds, densidad, selección explícita, estados mixtos, comparación base/splits, referencias financieras, inicio y memoria. `package.json` agrega `verify:device` y el selfcheck a `test:verification`.

## Verificación del runner

- `npm run test:verification`: PASS, 14/14.
- `node --check scripts/verify-device.mjs` y `node --check scripts/verify-device.test.mjs`: PASS.
- `npm run verify:device -- --help`: PASS.
- `git diff --check`: PASS.
- No se ejecutó Gradle, AVD ni inferencia. El runner no instala paquetes ni borra estado.

## Evidencia física parcial

- Destino de prueba: Samsung SM-S942B, Android 17/API 37, 1080×2340 @480 dpi, un único APK instalado. MemTotal observado 12,196,470,784 bytes (11.36 GiB). PSI de memoria disponible: `some/full avg10=0` en el último registro; es una lectura instantánea y no prueba el gate Android low-memory ni rendimiento de Gemma.
- APK base #34: `io.github.jovillarrealm.finsim_alpha` 1.0.3 (código 4), un `base.apk` de 1,896,306 bytes, SHA-256 `c22ac17ee92887fe119e0d75a73e564d19a47ac4ebcb112d97c15cece72120bc`; SHA de firma `2a5a930a15b4fe341a43017bff77fd1409b581e5ce7706774bbdbd4398f4f68c`. `identity.json` coincide con los bytes y declara fuente `97242848aca3228064708177288c12b240ecea45` (implementación `a001654`). `pm path` devolvió una sola ruta base, sin splits; ruta completa y hashes constan fuera de Git en `installed-apk-identity.txt` junto al informe. Este binario no contiene ni acredita #40, #45 o #32.
- Primera corrida, `2026-10-21T06-10-45-565Z-3d1ec8ec`, global `FAIL`: `UI-STARTUP-RENDER PASS`; `UI-TOUCH-TARGETS FAIL`, solo Conversor quedó visible (292×69.33 dp), faltaron Préstamo/Conciliación/Ofertas en viewport y no se midió ningún objetivo insuficiente (`undersized=[]`). Se conserva el resultado original y no se atribuye el fallo a cálculos no ejercitados.
- Corrida de reparación, `2026-10-21T06-16-54-185Z-b29e6fcb`, global `FAIL`: `UI-STARTUP-RENDER PASS`; `UI-TOUCH-TARGETS PASS` para scope explícito de las cuatro pestañas (Préstamo, Conversor y Conciliación 77×88 dp; Ofertas 77×52 dp); `UI-SURFACE-RECONCILIATION PASS`; `UI-SURFACE-RATE-CONVERTER PASS`; `UI-SCROLL PASS`, con ancla semántica y bounds observados. En Ofertas faltó `Aplicar comparación`; se dejó `NOT_RUN` porque el APK identificado es anterior a #40. `UI-FOCUS-KEYBOARD FAIL` observó `campo editable enfocado=false; teclado antes=false; después de cierre=false`. Préstamo, origen, seguros/eventos y cálculos financieros quedaron `NOT_RUN` por revisión antigua/no introducir datos. No se ingresaron ni guardaron importes. El formulario no se actualizó y SharedPreferences no se borró.
- Los informes inmutables, capturas y hashes están fuera del repo en `C:/Users/Lenovo/.finsim/verification/android/`; la última carpeta es `2026-10-21T06-16-54-185Z-b29e6fcb`. Incluye identidad de fuente/dispositivo, estado de cada criterio, resultado y evidencia PNG. Corridas intermedias (`06-13-04-614Z-b681dd24`, `06-16-14-390Z-e630e3fb`) detuvieron el recorrido cuando pestañas observadas salieron del viewport; la recuperación posterior resolvió ese supuesto del runner.
- Las pestañas midieron targets táctiles; esto no acredita todos los controles de la UI. La pantalla del conversor tenía un valor visible `1` y EA preexistente, pero no se ejecutó la acción de referencia ni se cotejó como aceptación financiera: `ANDROID-RATE NOT_RUN`. Los seis casos financieros permanecen pendientes.

## Retomar

1. Integrar #40/#45 en esta rama y coordinar con `/root` el APK exacto, firma/versión y custodia del móvil antes de instalarlo. No asumir que otro APK con versión/código iguales corresponde a la integración.
2. Con ese candidato identificado, repetir el perfil físico con `npm run verify:device -- --device <serial explícito> --apk <APK> --allow-ui-actions --restart-app`. Conservar datos; nunca desinstalar ni borrar almacenamiento. Revisar foco/teclado frente al resultado base.
3. En el APK integrado, ejercitar original/actual, seguros/eventos, conversor, conciliación y ofertas con cifras literales independientes, acciones observables y reportes fuera de Git. No declarar global `PASS` con criterios pendientes.
4. Repetir `npm run test:verification`, documentar nuevos IDs/resultados y hashes, publicar resumen prereview en #43, comprometer la ficha y código. #40 también modifica scripts de `package.json`; al integrar, conservar sus entradas `test:e2e`/`verify:web` además de las de este ticket. No editar el tablero compartido ni cerrar issue.
