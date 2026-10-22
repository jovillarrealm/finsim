export default [
  {
    id: 'WEB-40-CONDITIONS',
    title: '[WEB-40-CONDITIONS] Configure current balance, nominal rate, agreed payment, insurance and ARS profile through the real form',
    requirement: 'FIN-02 · saldo original/actual, nominal/frecuencia, cuota conocida, seguros y perfil argentino',
    expectation: 'El formulario conserva origen actual, tasa nominal anual 12 % con 12 capitalizaciones, cuota pactada ARS 507.51, seguro fijo ARS 5 y perfil argentino; para principal ARS 1000, tasa mensual resultante 1 %, el mes 1 muestra interés ARS 10, seguro ARS 5 y caja ARS 512.51.',
    expectationSource: 'docs/financial-conventions.md §3 y §5: ejemplo de cuota con interés; la cuota pactada y la adición del seguro son entradas directas, no valores calculados por el motor bajo prueba.',
    platform: 'Chrome · WebAssembly · escritorio 1280×900',
    source: 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'
  },
  {
    id: 'WEB-40-ARS-GATE',
    title: '[WEB-40-ARS-GATE] The Argentine prepayment profile is unavailable outside ARS and cannot persist after switching away',
    requirement: 'FIN-02 · perfil argentino exclusivo para operaciones ARS',
    expectation: 'Los controles del perfil no aparecen con COP; al cambiar COP→ARS aparecen, y al regresar a COP no se guarda perfil argentino.',
    expectationSource: 'docs/financial-conventions.md §3 y criterio FIN-02 de #37: los costos regionales se aplican solo al perfil ARS.',
    platform: 'Chrome · WebAssembly · escritorio 1280×900',
    source: 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'
  },
  {
    id: 'WEB-40-EVENT-BATCH',
    title: '[WEB-40-EVENT-BATCH] Discard and apply a missed payment, recovery and recurring event from touch controls',
    requirement: 'FIN-03 · omisiones, recuperación, eventos recurrentes y edición por lotes aplicar/descartar',
    expectation: 'Se descarta una omisión sin cambiar resultados; al aplicar omisión M1, recuperación ARS 105 en M2 y abono recurrente ARS 10 hasta M3: M1 genera interés ARS 10/seguro ARS 5 sin caja; M2 caja ARS 210 y saldo ARS 820; M3 caja ARS 115 y saldo ARS 718.20.',
    expectationSource: 'docs/financial-conventions.md §3–5, incluyendo la tabla literal de omisión con recuperación completa; M3 se deriva con interés 1 % de ARS 820, seguro ARS 5, cuota ARS 105 y abono recurrente ARS 10.',
    platform: 'Chrome · WebAssembly · navegador con viewport táctil 360×800',
    source: 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'
  },
  {
    id: 'WEB-40-MONTH-SYNC',
    title: '[WEB-40-MONTH-SYNC] Touch month selection stays synchronized across chart, summary and table at 360px',
    requirement: 'UI-02 · selección mensual compartida, interacción táctil, geometría y acceso textual/tabular',
    expectation: 'Cambiar el mes con el control textual actualiza resumen y tabla; la gráfica conserva límites y dimensiones tras seleccionar otro mes; el encabezado tabular incluye el componente de no aplicado.',
    expectationSource: 'Criterio UI-02 de #37 y semántica visible de la interfaz KMP.',
    platform: 'Chrome · WebAssembly · navegador con viewport táctil 360×800',
    source: 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'
  },
  {
    id: 'WEB-40-INCOMPLETE',
    title: 'FS18 360px touch navigation reaches M1200 and both canvases select the common month',
    requirement: 'FIN-01/FIN-03 · un resultado truncado no se presenta como liquidación definitiva',
    expectation: 'Un escenario que no amortiza dentro de 1.200 meses conserva el capital pendiente, se marca incompleto y no afirma una fecha de liquidación ni ahorro definitivo.',
    expectationSource: 'docs/financial-conventions.md §4 y §6: límite operativo de 1.200 meses y estado incompleto.',
    platform: 'Chrome · WebAssembly · navegador con viewport táctil 360×800',
    source: 'kmp/composeApp/browser-tests/loan-results.test.cjs'
  }
];
