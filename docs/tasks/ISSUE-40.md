# Issue #40 — estado y handoff de implementación

- Responsable de implementación: `/root/issue40_resume_luna`; coordinación: `/root`.
- Rama/worktree: `codex/spec37-issue40` en `C:/Users/Lenovo/.codex/worktrees/spec37-issue40/finsim`.
- Estado: implementación y registro listos para revisión independiente; #40 sigue abierto. La aceptación global no está limpia porque el perfil oficial `verify:web` conserva dos fallos y no se volvió a ejecutar tras la corrección del helper.
- Fuente canónica: [#40](https://github.com/jovillarrealm/finsim/issues/40), [#37](https://github.com/jovillarrealm/finsim/issues/37) y sus comentarios. La integración no modifica el tablero compartido.

## Cambio y evidencia

El helper browser ahora prepara los dos ejes de la tabla mediante desplazamientos reales sobre la instrucción de página, las cabeceras y una fila visible. Al volver a la primera columna considera todas las instancias visibles de cada texto de cabecera, evitando quedar anclado en una coincidencia de 1 px fuera del área útil. No se modifica producto ni se inyecta scroll/estado en el DOM.

- El perfil oficial `2026-10-22T03-59-28-777Z-fe9b5f63` se construyó desde `ab68e2d29d6bd910dbe038a2cbe6f8c5628581fb`, sin cambios locales, con Gradle 9.3.1 y Chrome 156. El árbol servido tuvo SHA-256 `781f439ea25a33a6a39b88d3392c7b0362e3176e50cdfba4db15a87d5916fc30`; el Wasm principal `a389cbef275f757f26ca.wasm` tuvo SHA-256 `d77056d69c692e7c84fabc5f96e88c5fad5fcadebbf8b1ed859beaddfa4e681a`. Los ocho archivos servidos se cotejaron byte a byte con el manifiesto.
- Resultado oficial, que se conserva como FAIL: `npm test` 21/23 y `npm run test:e2e` 17/19; ambos fallaron en CONDITIONS y EVENT-BATCH antes del ajuste final del helper. Los 13 casos heredados y los self-checks del verificador pasaron en esa ejecución.
- Retest dirigido posterior, con el mismo artefacto y fuente de producto, sin rebuild: 2/2 PASS. CONDITIONS duró 125.347 s; EVENT-BATCH 235.832 s frente al límite de 240 s (margen estrecho). El archivo de prueba usado antes de iniciar el proceso tuvo SHA-256 `820ed10de1bcb1542713eb015dc2d30ae3566ed46d5ed89abad23466bbf379e`.
- Trazas y salida del retest: `C:/Users/Lenovo/.finsim/verification/web/targeted-issue40-table-axis-final-20261022/`. Corridas fallidas previas se preservan en sus directorios de diagnóstico; no se sobrescribieron.

## Límites pendientes para la revisión

- No se repitió el perfil completo después del cambio del helper; el estado oficial del perfil continúa FAIL hasta una ejecución completa futura.
- El uso de artefacto reusable sigue siendo un riesgo sin resolver: `scripts/verify-web.mjs` filtra por `mtime` posterior al perfil previo y compara el inventario/hash de diferencias; un archivo de producto cambiado conservando una fecha anterior podría quedar fuera. Los self-checks de esta ejecución no demuestran ese caso. La revisión debe decidir retirar esa ruta o verificar identidad de entradas por contenido antes de aceptar reuse.
- Shared host: NOT_RUN después del cambio. Android físico/lector de pantalla: NOT_RUN. El recorrido táctil de navegador sí se probó; algunas pruebas usan rueda/puntero junto con Tab y no acreditan navegación exclusivamente por teclado. La tabularidad de dígitos pintados en Canvas es NOT_RUN: las mediciones 75/75/68 acreditan solo geometría de layout, no rasterización.

## Siguiente acción

Revisión independiente Luna/max del delta de esta rama, verificando el helper y el riesgo de reuse. Después, el coordinador decidirá las correcciones y una ejecución completa final; no cerrar #40 hasta integrar y verificar ese resultado.
