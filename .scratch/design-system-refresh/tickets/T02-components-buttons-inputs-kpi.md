# T02 — Botones, Entradas y Contenedores KPI

- Status: completed
- Depends On: T01
- Blocks: T05

## Descripción
Ajustar estilos y radios de botones, campos de entrada y tarjetas KPI:
- Botón Primario: Relleno `#0F382A`, texto `#FFFFFF`, radio `9px`, altura `44px`.
- Botón Secundario: Relleno `#C4F278`, texto `#0F2922`, radio `9px`, altura `44px`.
- Botón Fantasma: Fondo transparente, texto `#0F382A`, borde `1px solid #E2E8DF`.
- Form Inputs: Radio `8px`, borde `1px solid #CBD5C6`, altura `45px`.
- Anillos de foco: `:focus-visible { outline: 3px solid #84E296; outline-offset: 3px; }`.
- Badges de simulación completa (`#ECFDF5` / `#065F46` / borde `#A7F3D0`) e incompleta (`#FEF3C7` / `#92400E` / borde `#FDE68A`).
- Tarjetas KPI con borde `1px solid #E2E8DF` y tipografía `tabular-nums`.

## Criterios de Aceptación
- [x] Botones en `src/styles.css` y `App.kt` reflejan los radios (`9px`), colores y alturas accesibles (`44px`).
- [x] Anillos de foco en `:focus-visible` usan `#84E296` con 3px de grosor y 3px de offset.
- [x] Campos de formulario en web y Compose respetan radio `8px` y borde `1px solid #CBD5C6`.
