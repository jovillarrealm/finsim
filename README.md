# Finsim

Simulador educativo de préstamos colombianos. React y TypeScript con cálculos independientes de la interfaz.

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

Préstamos COP originales o desde saldo actual, tasas EA/EM/nominal, seguros, abonos únicos/recurrentes, omisiones y recuperación. Compara dos cronogramas y edita eventos desde gráficos o tabla. Incluye conciliación manual y guardado/exportación/importación JSON versión 1. Los cálculos son educativos; no reproducen automáticamente las reglas de un banco.

Los datos se guardan solo en el navegador. Exportar genera una copia portátil; importar valida antes de sustituir el escenario. Los resultados se recalculan desde las entradas.

## Publicación preparada

`.github/workflows/pages.yml` se ejecuta manualmente, después de conectar un repositorio y habilitar GitHub Pages con Actions. No publica por cada commit. No se ha desplegado todavía.

Consulta [el plan](docs/implementation-plan.md), [el tracking](docs/tracking.md) y [las convenciones financieras](docs/financial-conventions.md) antes de modificar cálculos.
