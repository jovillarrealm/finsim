import type { Currency, LoanResult } from '../../domain/scenario';
import { formatMoney, formatMonth } from '../../format';

export default function LoanSummary({ result, currency }: { result: LoanResult; currency: Currency }) {
  return <section className="panel" aria-label="Resumen del préstamo">
    <div className="section-heading"><div><p className="eyebrow">El panorama</p><h2>Tu préstamo en cifras</h2></div>
      <span className={`badge ${result.complete ? '' : 'warning'}`}>{result.complete ? 'Simulación completa' : 'Simulación incompleta'}</span></div>
    {!result.complete && <p className="note" role="status">No se alcanza la liquidación dentro del horizonte calculado. Los importes son acumulados parciales, no costos definitivos.</p>}
    <dl className="summary-grid">
      <div className="summary-feature"><dt>Cuota base mensual</dt><dd>{formatMoney(result.basePayment, currency)}</dd><small>Capital e intereses; seguro aparte</small></div>
      <div><dt>Liquidación del préstamo</dt><dd>{result.payoffDate ? formatMonth(result.payoffDate) : 'Sin liquidación'}</dd><small>{result.payoffMonth ? `Mes ${result.payoffMonth}` : `${result.rows.length} meses calculados`}</small></div>
      <div><dt>{result.complete ? 'Intereses pagados' : 'Intereses pagados hasta aquí'}</dt><dd>{formatMoney(result.totals.interest, currency)}</dd></div>
      <div><dt>Seguros pagados</dt><dd>{formatMoney(result.totals.insurance, currency)}</dd><small>{result.lastInsuranceMonth ? `Último cargo: mes ${result.lastInsuranceMonth}` : 'Sin cargos de seguro'}</small></div>
      <div><dt>Capital pagado</dt><dd>{formatMoney(result.totals.principal, currency)}</dd></div>
      <div><dt>{result.complete ? 'Dinero total pagado' : 'Dinero pagado hasta aquí'}</dt><dd>{formatMoney(result.totals.cashPaid, currency)}</dd><small>Incluye recuperación y extras aplicados</small></div>
      <div><dt>Deuda pendiente</dt><dd>{formatMoney(result.remainingDebt, currency)}</dd></div>
      <div><dt>Dinero ofrecido no aplicado</dt><dd>{formatMoney(result.totals.unapplied, currency)}</dd><small>No se cuenta como dinero pagado</small></div>
    </dl>
  </section>;
}
