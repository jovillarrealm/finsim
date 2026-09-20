# Finsim

Simulador educativo de préstamos. React y TypeScript con cálculos independientes de la interfaz.

## Desarrollo

Requiere Node.js compatible con las versiones fijadas en `package-lock.json`.

```sh
npm ci
npm run dev
```

## Verificación

```sh
npm run typecheck
npm test
npm run build
```

Pruebas de navegador (instalación del navegador solo la primera vez):

```sh
npx playwright install chromium
npm run test:e2e
```

El servidor de desarrollo abre `http://127.0.0.1:5173`. Para revisar la compilación usa `npm run preview`.

## Alcance actual

Préstamos en COP, ARS, USD o EUR, originales o desde saldo actual, tasas EA/EM/nominal, seguros, abonos únicos/recurrentes, omisiones y recuperación. Compara dos cronogramas y edita eventos desde gráficos o tabla. Incluye conciliación manual y guardado/exportación/importación JSON versión 1. Los cálculos son educativos; no reproducen automáticamente las reglas de un banco.

La moneda es un parámetro del escenario: cambiarla conserva los importes, sin conversión cambiaria. Se muestran códigos explícitos y números con coma decimal. Los archivos anteriores sin moneda se interpretan en COP.

Los datos se guardan solo en el navegador. Exportar genera una copia portátil; importar valida antes de sustituir el escenario. Los resultados se recalculan desde las entradas.

## Publicación

Sitio: https://jovillarrealm.github.io/finsim/

`.github/workflows/pages.yml` se ejecuta manualmente desde Actions en https://github.com/jovillarrealm/finsim. No publica por cada commit. Instala dependencias, ejecuta las pruebas y construye antes de desplegar.

Consulta [el plan](docs/implementation-plan.md), [el tracking](docs/tracking.md) y [las convenciones financieras](docs/financial-conventions.md) antes de modificar cálculos.
