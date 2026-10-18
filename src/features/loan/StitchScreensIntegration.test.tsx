import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import { exampleScenario, type Scenario } from '../../domain/scenario';
import { simulateLoan } from '../../engine/loan';
import LoanSummary from './LoanSummary';
import MonthlyTable from './MonthlyTable';
import ScenarioComparison from './ScenarioComparison';

describe('Stitch Design System Screens Integration (T06 - T10)', () => {
  const scenarioWithPrepayments: Scenario = {
    ...exampleScenario,
    events: [
      { id: 'ev1', kind: 'extra', month: 6, amount: '5000000.00' },
      { id: 'ev2', kind: 'extra', month: 18, amount: '3000000.00' },
    ],
  };

  const original = simulateLoan({ ...scenarioWithPrepayments, events: [] });
  const modified = simulateLoan(scenarioWithPrepayments);

  it('LoanSummary renders the executive Hero Savings Card and 2x2 secondary KPI grid with prepayments', () => {
    const markup = renderToStaticMarkup(
      createElement(LoanSummary, {
        result: modified,
        original,
        currency: scenarioWithPrepayments.currency,
      })
    );

    // Hero Savings Card (T07)
    expect(markup).toContain('Ahorro Neto Proyectado');
    expect(markup).toContain('hero-savings-card');
    expect(markup).toContain('Tiempo ganado:');
    expect(markup).toContain('Libre en Mes');
    expect(markup).toContain('% intereses');

    // 2x2 Secondary KPI Grid (T07)
    expect(markup).toContain('kpi-grid-2x2');
    expect(markup).toContain('Cuota mensual fija');
    expect(markup).toContain('Intereses acumulados');
    expect(markup).toContain('Seguros totales');
    expect(markup).toContain('Total desembolso');
    expect(markup).toContain('Pactados:');
  });

  it('ScenarioComparison renders Trajectory Curve with extinction zone and active month card', () => {
    const markup = renderToStaticMarkup(
      createElement(ScenarioComparison, {
        scenario: scenarioWithPrepayments,
        original,
        modified,
        onChange: () => true,
        selectedMonth: 6,
      })
    );

    // Mode toolbar & Trajectory SVG (T08)
    expect(markup).toContain('Trayectoria de saldo');
    expect(markup).toContain('trajectory-svg');
    expect(markup).toContain('LIBRE DE DEUDA');
    expect(markup).toContain('$0 pagos requeridos');
    expect(markup).toContain('amortDeltaGradient');

    // Active Month Inspection Card (T08 / T09)
    expect(markup).toContain('active-month-card');
    expect(markup).toContain('Detalle del Mes 6');
    expect(markup).toContain('Modificar este mes');
    expect(markup).toContain('Abono extraordinario a capital:');
  });

  it('MonthlyTable renders curated Key Milestones with prepayments and full schedule toggle', () => {
    const markup = renderToStaticMarkup(
      createElement(MonthlyTable, {
        result: modified,
        currency: scenarioWithPrepayments.currency,
      })
    );

    // Milestones view (T10)
    expect(markup).toContain('Cronograma Clave');
    expect(markup).toContain('Hitos Clave');
    expect(markup).toContain('Tabla Completa');
    expect(markup).toContain('milestones-schedule-list');
    expect(markup).toContain('Mes 01');
    expect(markup).toContain('Extra');
    expect(markup).toContain('Fin de Crédito');
    expect(markup).toContain('Descargar cronograma completo (.CSV)');
  });

  it('MonthlyTable renders clean fallback without prepayments when no events exist', () => {
    const noEventsResult = simulateLoan({ ...exampleScenario, events: [] });
    const markup = renderToStaticMarkup(
      createElement(MonthlyTable, {
        result: noEventsResult,
        currency: exampleScenario.currency,
      })
    );

    expect(markup).toContain('Cronograma Clave');
    expect(markup).toContain('Mes 01');
    expect(markup).toContain('Fin de Crédito');
    expect(markup).not.toContain('badge-extra');
  });
});

