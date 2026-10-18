# T06 — Barra de Navegación Móvil Flotante y Shell Responsivo

- Status: completed
- Depends On: T01, T02
- Blocks: None

## Descripción
Implementar el shell móvil y barra de navegación inferior fija inspirada en la pantalla Stitch `268286d1`:
- Barra de navegación inferior flotante con altura `4rem (64px)`, `backdrop-blur-xl`, `bg-surface/90`, y relleno seguro `pb-safe`.
- 4 destinos principales: `Simulador` (activo por defecto), `Cronograma`, `Comparar` y `Ajustes`.
- Cabecera móvil con isotipo `finsim.` (punto en Radiant Chartreuse `#C4F278`), badge de moneda activa (`COP $`), botón de menú y perfil.
- Soporte para paddings de muesca/isla dinámica (`pt-safe`, `pb-safe`).

## Criterios de Aceptación
- [x] En anchos de pantalla móviles (< 768px), la barra de navegación se ancla al fondo con efecto desenfoque.
- [x] La barra superior muestra el selector rápido de moneda y punto de marca en `#C4F278`.
- [x] La navegación táctil respeta los estándares de accesibilidad con áreas de pulsación mínimas de 44x44px.
