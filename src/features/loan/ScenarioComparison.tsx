import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { D, MAX_MONTHS, money, type Currency, type LoanResult, type Scenario } from '../../domain/scenario';
import { formatMoney, formatMonth } from '../../format';
import EventEditor from './EventEditor';
import './Comparison.css';

export function chartRows(result: LoanResult, months: number) {
  const rows = new Map(result.rows.map(row => [row.month, row]));
  return Array.from({ length: months }, (_, index) => {
    const row = rows.get(index + 1);
    const payments = row ? [row.scheduled, row.catchup, row.extra] : [];
    const exact = {
      scheduled: row?.scheduled.principal ?? '0.00',
      extra: money(new D(row?.extra.principal ?? 0).plus(row?.catchup.principal ?? 0)),
      interest: money(payments.reduce((sum, payment) => sum.plus(payment.interest), new D(0))),
      insurance: money(payments.reduce((sum, payment) => sum.plus(payment.insurance), new D(0))),
      cash: row?.cashPaid ?? '0.00',
    };
    // Floating point is only for chart coordinates; labels retain decimal strings.
    return { month: index + 1, exact, scheduled: Number(exact.scheduled), extra: Number(exact.extra),
      interest: Number(exact.interest), insurance: Number(exact.insurance), cash: Number(exact.cash) };
  });
}
export const signedMoney = (value: string, currency: Currency = 'COP') => `${new D(value).gt(0) ? '+' : ''}${formatMoney(value, currency)}`;
export const chartMoney = (row: ReturnType<typeof chartRows>[number], key: keyof ReturnType<typeof chartRows>[number]['exact'], currency: Currency = 'COP') => formatMoney(row.exact[key], currency);

export default function ScenarioComparison({ scenario, original, modified, onChange, selectedMonth, onSelectMonth }: {
  scenario: Scenario; original: LoanResult; modified: LoanResult; onChange: (value: Scenario) => void;
  selectedMonth?: number; onSelectMonth?: (month: number) => void;
}) {
  const [internalMonth, setInternalMonth] = useState(1);
  const month = selectedMonth ?? internalMonth;
  const select = (next: number) => { if (Number.isInteger(next) && next >= 1 && next <= MAX_MONTHS) { setInternalMonth(next); onSelectMonth?.(next); } };
  const months = Math.max(original.rows.at(-1)?.month ?? 1, modified.rows.at(-1)?.month ?? 1);
  const originalData = chartRows(original, months), modifiedData = chartRows(modified, months);
  const ceiling = Math.max(1, ...originalData.map(row => row.cash), ...modifiedData.map(row => row.cash));
  const complete = original.complete && modified.complete;
  const row = modified.rows.find(item => item.month === month);
  const deltaMonths = original.payoffMonth !== null && modified.payoffMonth !== null ? modified.payoffMonth - original.payoffMonth : null;
  return <section className="panel scenario-comparison" aria-labelledby="comparison-title">
    <h2 id="comparison-title">Original y modificado</h2>
    <p>Diferencias = modificado − original. Signo positivo: más costo o más tiempo; negativo: menos. Los extras conservan la cuota y reducen plazo.</p>
    {!complete && <p role="status">Resultado incompleto: los importes son acumulados hasta el límite simulado, no costos finales ni ahorros definitivos.</p>}
    <dl className="comparison-deltas">
      {(['interest', 'insurance', 'cashPaid'] as const).map((key, index) => <div key={key}><dt>{['Diferencia de intereses pagados', 'Diferencia de seguros pagados', 'Diferencia de caja pagada'][index]}{!complete ? ' (parcial)' : ''}</dt>
        <dd>{signedMoney(money(new D(modified.totals[key]).minus(original.totals[key])), scenario.currency)}</dd></div>)}
      <div><dt>Diferencia de plazo</dt><dd>{complete && deltaMonths !== null ? `${deltaMonths > 0 ? '+' : ''}${deltaMonths} meses` : 'Sin liquidación completa'}</dd></div>
    </dl>
    <p>Liquidación original: {original.payoffDate ? formatMonth(original.payoffDate) : 'No alcanzada'} · Modificado: {modified.payoffDate ? formatMonth(modified.payoffDate) : 'No alcanzada'}.</p>
    <p>Selecciona una barra o elige un mes abajo. Ambos gráficos comparten meses y escala de {scenario.currency}; incluyen seguros posteriores a la liquidación. La tabla mensual ofrece los valores exactos.</p>
    <div className="comparison-charts">{[{ name: 'Original', data: originalData }, { name: 'Modificado', data: modifiedData }].map(chart => <figure key={chart.name}>
      <figcaption>{chart.name} · pagos mensuales ({scenario.currency})</figcaption>
      <ResponsiveContainer width="100%" height={300}><BarChart data={chart.data} accessibilityLayer onClick={state => { if (state.activeLabel !== undefined) select(Number(state.activeLabel)); }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="month" label={{ value: 'Mes', position: 'insideBottomRight', offset: -4 }} />
        <YAxis width={68} domain={[0, ceiling]} tickFormatter={value => new Intl.NumberFormat('es-CO', { notation: 'compact' }).format(Number(value))} />
        <Tooltip formatter={(_value, _name, item) => chartMoney(item.payload, item.dataKey as keyof typeof originalData[number]['exact'], scenario.currency)} labelFormatter={label => `Mes ${label}`} /><Legend />
        <Bar dataKey="scheduled" name="Capital habitual" stackId="cash" fill="#2563a6" isAnimationActive={false} />
        <Bar dataKey="extra" name="Capital extra y recuperación" stackId="cash" fill="#188570" isAnimationActive={false} />
        <Bar dataKey="interest" name="Intereses pagados" stackId="cash" fill="#c75a25" isAnimationActive={false} />
        <Bar dataKey="insurance" name="Seguros pagados" stackId="cash" fill="#8b62ac" isAnimationActive={false} />
        <ReferenceLine x={month} stroke="#111827" />
      </BarChart></ResponsiveContainer>
    </figure>)}</div>
    <label className="month-selector">Mes para editar<input aria-label="Mes para editar" type="number" min="1" max={MAX_MONTHS} value={month} onChange={event => select(Number(event.target.value))} /></label>
    <p aria-live="polite">Mes {month}{row ? ` (${formatMonth(row.date)}): habitual ${formatMoney(row.scheduled.applied, scenario.currency)}, recuperación ${formatMoney(row.catchup.applied, scenario.currency)}, extra ${formatMoney(row.extra.applied, scenario.currency)}. Capital extra y recuperación: ${formatMoney(money(new D(row.extra.principal).plus(row.catchup.principal)), scenario.currency)}. No aplicado: ${formatMoney(money(new D(row.extra.unapplied).plus(row.catchup.unapplied)), scenario.currency)}.` : ': sin pagos en el cronograma modificado.'}</p>
    <EventEditor key={`${month}-${JSON.stringify(scenario.events)}`} scenario={scenario} month={month} onChange={onChange} />
    {modified.unappliedEvents.length > 0 && <details><summary>Eventos posteriores no aplicados ({modified.unappliedEvents.length})</summary><ul>
      {modified.unappliedEvents.map(event => <li key={`${event.eventId}-${event.month}`}>Mes {event.month}: {formatMoney(event.amount, scenario.currency)}. {event.reason}</li>)}
    </ul></details>}
  </section>;
}
