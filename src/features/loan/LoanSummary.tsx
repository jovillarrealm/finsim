import { D, money, type Currency, type LoanResult } from '../../domain/scenario';
import { formatMoney, formatMonth } from '../../format';

export default function LoanSummary({
  result,
  original,
  currency,
}: {
  result: LoanResult;
  original?: LoanResult;
  currency: Currency;
}) {
  const hasComparison = !!original;
  const interestDelta = original ? new D(result.totals.interest).minus(original.totals.interest) : new D(0);
  const insuranceDelta = original ? new D(result.totals.insurance).minus(original.totals.insurance) : new D(0);
  const cashDelta = original ? new D(result.totals.cashPaid).minus(original.totals.cashPaid) : new D(0);
  const origPayoff = original?.payoffMonth ?? original?.rows.length ?? result.rows.length;
  const modPayoff = result.payoffMonth ?? result.rows.length;
  const monthsSaved = origPayoff - modPayoff;
  const interestPctSaved =
    original && new D(original.totals.interest).gt(0) && interestDelta.lt(0)
      ? Math.round(interestDelta.abs().dividedBy(original.totals.interest).times(100).toNumber())
      : null;

  const firstRow = result.rows[0];
  const monthlyInstallment = firstRow
    ? money(new D(result.basePayment).plus(firstRow.insuranceCharged))
    : result.basePayment;

  return (
    <section className="panel" aria-label="Resumen del préstamo">
      <div className="section-heading">
        <div>
          <p className="eyebrow">El panorama</p>
          <h2>Tu préstamo en cifras</h2>
        </div>
        <span className={`badge ${result.complete ? '' : 'warning'}`}>
          {result.complete ? 'Simulación completa' : 'Simulación incompleta'}
        </span>
      </div>

      {!result.complete && (
        <p className="note" role="status">
          No se alcanza la liquidación dentro del horizonte calculado. Los importes son acumulados parciales, no costos definitivos.
        </p>
      )}

      {hasComparison && (
        <>
          {/* Tarjeta Hero Principal: Ahorro Neto Proyectado */}
          <div className="hero-savings-card">
            <div className="hero-savings-header">
              <span className="eyebrow" style={{ color: '#065f46', margin: 0, fontWeight: 700 }}>
                Ahorro Neto Proyectado
              </span>
              {interestPctSaved !== null && interestPctSaved > 0 && (
                <span className="savings-badge">
                  <span aria-hidden="true">↓</span> -{interestPctSaved}% intereses
                </span>
              )}
            </div>

            <div className="hero-savings-amount">
              {cashDelta.lt(0)
                ? `- ${formatMoney(cashDelta.abs().toFixed(2), currency)}`
                : cashDelta.gt(0)
                ? `+ ${formatMoney(cashDelta.toFixed(2), currency)}`
                : `${formatMoney('0.00', currency)}`}
            </div>

            <div className="hero-savings-footer">
              <div className="time-saved">
                <span aria-hidden="true">✓</span>
                <span>
                  {monthsSaved > 0
                    ? `Tiempo ganado: ${monthsSaved} ${monthsSaved === 1 ? 'mes' : 'meses'}${
                        monthsSaved >= 12
                          ? ` (${Math.floor(monthsSaved / 12)} ${Math.floor(monthsSaved / 12) === 1 ? 'año' : 'años'})`
                          : ''
                      }`
                    : 'Al día con el plazo contractual'}
                </span>
              </div>
              <span className="status-pill">
                {result.complete && result.payoffMonth
                  ? `Libre en Mes ${result.payoffMonth}/${origPayoff}`
                  : `En curso · Mes ${result.rows.length}`}
              </span>
            </div>
          </div>

          {/* Cuadrícula Secundaria 2x2 */}
          <div className="kpi-grid-2x2">
            {/* KPI 1: Cuota mensual fija */}
            <div className="kpi-card">
              <div className="kpi-card-header">
                <span className="eyebrow">Cuota mensual fija</span>
              </div>
              <div>
                <span className="kpi-value">{formatMoney(monthlyInstallment, currency)}</span>
                <span className="kpi-subtext">Capital + Intereses + Seguros</span>
              </div>
            </div>

            {/* KPI 2: Intereses acumulados */}
            <div className="kpi-card">
              <div className="kpi-card-header">
                <span className="eyebrow">Intereses acumulados</span>
                {interestDelta.lt(0) && (
                  <span className="kpi-delta-pill">
                    -{formatMoney(interestDelta.abs().toFixed(2), currency)}
                  </span>
                )}
              </div>
              <div>
                <span className="kpi-value interest">{formatMoney(result.totals.interest, currency)}</span>
                <span className="kpi-subtext">
                  {original && interestDelta.lt(0) ? (
                    <span className="kpi-strikethrough">
                      Pactados: {formatMoney(original.totals.interest, currency)}
                    </span>
                  ) : (
                    'Costo neto de financiación'
                  )}
                </span>
              </div>
            </div>

            {/* KPI 3: Seguros totales acumulados */}
            <div className="kpi-card">
              <div className="kpi-card-header">
                <span className="eyebrow">Seguros totales</span>
                {insuranceDelta.lt(0) && (
                  <span className="kpi-delta-pill">
                    -{formatMoney(insuranceDelta.abs().toFixed(2), currency)}
                  </span>
                )}
              </div>
              <div>
                <span className="kpi-value insurance">{formatMoney(result.totals.insurance, currency)}</span>
                <span className="kpi-subtext">
                  {monthsSaved > 0
                    ? `-${monthsSaved} cuotas de seguro ahorradas`
                    : result.lastInsuranceMonth
                    ? `Último cargo: mes ${result.lastInsuranceMonth}`
                    : 'Sin cargos de seguro'}
                </span>
              </div>
            </div>

            {/* KPI 4: Total desembolso de bolsillo */}
            <div className="kpi-card">
              <div className="kpi-card-header">
                <span className="eyebrow">Total desembolso</span>
              </div>
              <div>
                <span className="kpi-value">{formatMoney(result.totals.cashPaid, currency)}</span>
                <span className="kpi-subtext">
                  {original && cashDelta.lt(0) ? (
                    <span className="kpi-strikethrough">
                      Antes: {formatMoney(original.totals.cashPaid, currency)}
                    </span>
                  ) : (
                    'Capital, intereses y seguros'
                  )}
                </span>
              </div>
            </div>
          </div>
        </>
      )}

      {/* Desglose complementario de auditoría */}
      <details className="additional-metrics" style={{ marginTop: '0.8rem' }}>
        <summary style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--muted)', cursor: 'pointer' }}>
          Consultar desglose contable granular
        </summary>
        <dl className="summary-grid" style={{ marginTop: '1rem' }}>
          <div className="summary-feature">
            <dt>Cuota base mensual</dt>
            <dd>{formatMoney(result.basePayment, currency)}</dd>
            <small>Capital e intereses; seguro aparte</small>
          </div>
          <div>
            <dt>Liquidación del préstamo</dt>
            <dd>{result.payoffDate ? formatMonth(result.payoffDate) : 'Sin liquidación'}</dd>
            <small>{result.payoffMonth ? `Mes ${result.payoffMonth}` : `${result.rows.length} meses calculados`}</small>
          </div>
          <div>
            <dt>{result.complete ? 'Intereses pagados' : 'Intereses pagados hasta aquí'}</dt>
            <dd>{formatMoney(result.totals.interest, currency)}</dd>
          </div>
          <div>
            <dt>Seguros pagados</dt>
            <dd>{formatMoney(result.totals.insurance, currency)}</dd>
            <small>
              {result.lastInsuranceMonth
                ? `Último cargo: mes ${result.lastInsuranceMonth}`
                : 'Sin cargos de seguro'}
            </small>
          </div>
          <div>
            <dt>Capital pagado</dt>
            <dd>{formatMoney(result.totals.principal, currency)}</dd>
          </div>
          <div>
            <dt>{result.complete ? 'Dinero total pagado' : 'Dinero pagado hasta aquí'}</dt>
            <dd>{formatMoney(result.totals.cashPaid, currency)}</dd>
            <small>Incluye recuperación y extras aplicados</small>
          </div>
          <div>
            <dt>Deuda pendiente</dt>
            <dd>{formatMoney(result.remainingDebt, currency)}</dd>
          </div>
          <div>
            <dt>Dinero ofrecido no aplicado</dt>
            <dd>{formatMoney(result.totals.unapplied, currency)}</dd>
            <small>No se cuenta como dinero pagado</small>
          </div>
        </dl>
      </details>
    </section>
  );
}

