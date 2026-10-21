# Issue #43 — perfil de verificación Android

- Responsable: `/root/issue43_implement`; coordinadora `/root`.
- Estado: runner y recetas ejecutables listos en checkpoint; la aceptación de recorridos en APK integrado sigue pendiente. No iniciar revisión independiente hasta coordinar el binario #40/#45. No cerrar issue.
- Rama/worktree: `codex/spec37-issue43` · `C:/Users/Lenovo/.codex/worktrees/spec37-issue43/finsim`.
- Base reservada: `e9317d83baaaebf0e3b4c4092e7543da916b3c1b`; punta ya integra #45 mediante `89f4f6127be38bbf40e0e47a6cab77bf2e7adf1d` (merge local `7682cf8`).
- Alcance: runner Android KMP invocable, identidad APK instalada/candidata, dispositivo y densidad real, arranque, navegación observada, controles táctiles, foco/teclado, scroll y criterios financieros independientes. No modifica el producto Kotlin ni código React.

## Implementación

- `scripts/verify-device.mjs`: perfil Android basado en stdlib/ADB. Selecciona destino únicamente con `--device`; las acciones requieren `--allow-ui-actions`. Las recetas financieras además exigen `--restart-app`, hash byte a byte del candidato frente a la instalación y un `identity.json` que vincule ese hash con un commit productor no vacío. El commit productor del APK y el commit fuente del runner se registran por separado; no se exige igualdad entre ellos. Informa modelo, API, pantalla, densidad, fontScale, MemTotal y PSI sin inferir elegibilidad ni rendimiento Gemma.
- Siete recetas cubren préstamo original, saldo actual, seguro porcentual con abono extraordinario, omisión y recuperación, conversión EM→EA, conciliación M1 y comparación de ofertas. Editan controles reales por semántica, confirman foco/teclado, entrada, selección, activación y cada cifra literal. Un caso solo pasa cuando se completan todas sus acciones y coinciden todas sus salidas numéricas. Las recetas no pulsan Guardar; al terminar una mutación, el runner fuerza stop/start sin limpiar almacenamiento. El check final solo confirma reapertura y contenido visible; no compara igualdad del escenario persistido.
- Los gestos requieren jerarquía reciente, bounds visibles y ancla semántica observada. La búsqueda usa hasta dos desplazamientos por acción; el perfil total admite 512 dumps, cada receta 64 dumps y 24 desplazamientos, y cada salida admite hasta cuatro gestos de tabla. Si una acción falla antes del cálculo, el caso queda `NOT_RUN`; si ya se ejecutó el cálculo y falta una acción o salida, queda `FAIL`.
- Cifras de referencia son literales y provienen de `docs/financial-conventions.md` o de la prueba compartida identificada. `scripts/verify-device.test.mjs` cubre parsing de jerarquía/foco/bounds, densidad, selección explícita, estados mixtos, comparación base/splits, preparación financiera, salidas numéricas, acciones incompletas y restauración tras una mutación fallida.

## Verificación del runner

- `npm run test:verification`: PASS, 19/19. Estos selfchecks validan contratos del runner y no acreditan interacción ni cálculos del APK.
- `node --check scripts/verify-device.mjs` y `node --check scripts/verify-device.test.mjs`: PASS.
- `npm run verify:device -- --help`: PASS.
- `git diff --check`: PASS.
- No se ejecutó Gradle, AVD ni inferencia. El runner no instala paquetes ni borra estado.

## Evidencia física parcial

- Destino de prueba: Samsung SM-S942B, Android 17/API 37, 1080×2340 @480 dpi, un único APK instalado. MemTotal observado 12,196,470,784 bytes (11.36 GiB). El fontScale leído en modo solo lectura después de las corridas base fue 1.5; los informes originales no lo incluían, pero el runner actual lo registrará. PSI `some/full avg10=0` en el último informe es una lectura instantánea y no prueba el gate Android low-memory ni rendimiento de Gemma.
- APK base #34: `io.github.jovillarrealm.finsim_alpha` 1.0.3 (código 4), un `base.apk` de 1,896,306 bytes, SHA-256 `c22ac17ee92887fe119e0d75a73e564d19a47ac4ebcb112d97c15cece72120bc`; SHA de firma `2a5a930a15b4fe341a43017bff77fd1409b581e5ce7706774bbdbd4398f4f68c`. `identity.json` coincide con los bytes y declara fuente `97242848aca3228064708177288c12b240ecea45` (implementación `a001654`). `pm path` devolvió una sola ruta base, sin splits; ruta completa y hashes constan fuera de Git en `installed-apk-identity.txt` junto al informe. Este binario no contiene ni acredita #40, #45 o #32.
- Primera corrida, `2026-10-21T06-10-45-565Z-3d1ec8ec`, global `FAIL`: `UI-STARTUP-RENDER PASS`; `UI-TOUCH-TARGETS FAIL`, solo Conversor quedó visible (292×69.33 dp), faltaron Préstamo/Conciliación/Ofertas en viewport y no se midió ningún objetivo insuficiente (`undersized=[]`). Se conserva el resultado original y no se atribuye el fallo a cálculos no ejercitados.
- Corrida de reparación, `2026-10-21T06-16-54-185Z-b29e6fcb`, global `FAIL`: `UI-STARTUP-RENDER PASS`; `UI-TOUCH-TARGETS PASS` para scope explícito de las cuatro pestañas (Préstamo, Conversor y Conciliación 77×88 dp; Ofertas 77×52 dp); `UI-SURFACE-RECONCILIATION PASS`; `UI-SURFACE-RATE-CONVERTER PASS`; `UI-SCROLL PASS`, con ancla semántica y bounds observados. En Ofertas faltó `Aplicar comparación`; se dejó `NOT_RUN` porque el APK identificado es anterior a #40. `UI-FOCUS-KEYBOARD FAIL` observó `campo editable enfocado=false; teclado antes=false; después de cierre=false`. Préstamo, origen, seguros/eventos y cálculos financieros quedaron `NOT_RUN` por revisión antigua/no introducir datos. No se ingresaron ni guardaron importes. El formulario no se actualizó y SharedPreferences no se borró.
- Los informes inmutables, capturas y hashes están fuera del repo en `C:/Users/Lenovo/.finsim/verification/android/`; la última carpeta es `2026-10-21T06-16-54-185Z-b29e6fcb`. Incluye identidad de fuente/dispositivo, estado de cada criterio, resultado y evidencia PNG. Corridas intermedias (`06-13-04-614Z-b681dd24`, `06-16-14-390Z-e630e3fb`) detuvieron el recorrido cuando pestañas observadas salieron del viewport; la recuperación posterior resolvió ese supuesto del runner.
- Las pestañas midieron targets táctiles; esto no acredita todos los controles de la UI. La pantalla del conversor tenía un valor visible `1` y EA preexistente, pero no se ejecutó la acción de referencia ni se cotejó como aceptación financiera: `ANDROID-RATE NOT_RUN`. Las siete recetas ejecutables siguen pendientes de un APK integrado; no se atribuyen funciones de #40/#45 al APK base #34.

## Retomar

1. Esperar la integración de #40 y coordinar con `/root` el APK exacto, firma/hash/fuente y custodia del móvil. La versión/código de paquete no identifican por sí solos un binario.
2. Comparar todos los bytes del APK instalado con `--apk <APK integrado>` y verificar que `identity.json` vincule esos bytes con el commit productor declarado. La identidad de fuente del runner es independiente. Luego usar `npm run verify:device -- --device <serial explícito> --apk <APK integrado> --allow-ui-actions --restart-app --run-financial-cases`. Conservar datos; nunca desinstalar ni borrar almacenamiento. Revisar foco/teclado frente al resultado base.
3. Ejercitar las siete recetas con cifras literales independientes. Mantener estados, omisiones, hashes, capturas e informe fuera de Git; los selfchecks y el arranque de #34 no sustituyen esas corridas.
4. Resumen prereview actualizado en https://github.com/jovillarrealm/finsim/issues/43#issuecomment-6310014834. El código de recetas y selfchecks está en `0ad05cf`; no se hizo push ni se cerró el issue. Al integrar, conservar las entradas `test:e2e`/`verify:web` de #40 junto a las de este ticket. No editar el tablero compartido.
