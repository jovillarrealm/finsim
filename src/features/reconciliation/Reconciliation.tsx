import { useState, type FormEvent } from 'react';
import { type Currency, type LoanResult } from '../../domain/scenario';
import { formatMoney } from '../../format';
import { fieldLabels, reconcileRow, statementSchema, type StatementField, type StatementRow } from './reconcile';

export default function Reconciliation({ result, currency }: { result: LoanResult; currency: Currency }) {
  const [statements, setStatements] = useState<StatementRow[]>([]);
  const [error, setError] = useState('');
  function addRow(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const input: Record<string, unknown> = { month: Number(data.get('month')) };
    for (const field of Object.keys(fieldLabels)) {
      const value = String(data.get(field) ?? '').trim();
      if (value) input[field] = value.replace(',', '.');
    }
    const parsed = statementSchema.safeParse(input);
    if (!parsed.success) { setError(parsed.error.issues.map(issue => issue.message).join(' ')); return; }
    try {
      reconcileRow(parsed.data, result.rows);
      setStatements(previous => [...previous.filter(row => row.month !== parsed.data.month), parsed.data].sort((a, b) => a.month - b.month));
      setError('');
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'No se pudo comparar la fila.'); }
  }
  return <section className="panel" aria-labelledby="reconciliation-title">
    <p className="eyebrow">COMPRUEBA TUS NÚMEROS</p>
    <h2 id="reconciliation-title">Compara con tu extracto</h2>
    <p className="muted">Introduce una o varias filas del extracto. Comparamos con el escenario original, sin modificar sus supuestos.</p>
    <form onSubmit={addRow} className="form-grid">
      <label className="field">Mes del extracto<input name="month" type="number" min="1" max="1200" defaultValue="1" required /></label>
      {(Object.keys(fieldLabels) as StatementField[]).map(field => <label className="field" key={field}>
        {fieldLabels[field]} ({currency})<input name={field} inputMode="decimal" placeholder="Opcional · sin separador de miles" />
      </label>)}
      <button className="button" type="submit">Comparar fila</button>
    </form>
    {error && <p role="alert" className="error">{error}</p>}
    <p className="muted">Diferencia = extracto − simulación. Interés y seguro son cargos generados; capital es dinero aplicado al saldo. Una diferencia puede venir de fechas, redondeos, seguros o reglas de pago distintas.</p>
    {statements.map(statement => {
      let comparisons;
      try { comparisons = reconcileRow(statement, result.rows); }
      catch { return <div key={statement.month} role="status">El mes {statement.month} ya no existe tras cambiar el préstamo. <button onClick={() => setStatements(rows => rows.filter(row => row.month !== statement.month))}>Quitar fila</button></div>; }
      return <div key={statement.month} className="statement-row">
        <div className="section-heading"><h3>Mes {statement.month}</h3><button className="button secondary" onClick={() => setStatements(rows => rows.filter(row => row.month !== statement.month))}>Quitar mes {statement.month}</button></div>
        <div className="table-scroll"><table><caption className="sr-only">Comparación del mes {statement.month}</caption>
          <thead><tr><th>Concepto</th><th>Extracto</th><th>Simulación</th><th>Diferencia</th></tr></thead>
          <tbody>{comparisons.map(item => <tr key={item.field}><th scope="row">{fieldLabels[item.field]}</th><td>{formatMoney(item.actual, currency)}</td><td>{formatMoney(item.simulated, currency)}</td><td>{item.difference === '0.00' ? 'Coincide' : formatMoney(item.difference, currency)}</td></tr>)}</tbody>
        </table></div>
      </div>;
    })}
    {!statements.length && <p className="empty-state">Añade una fila para ver las diferencias aquí.</p>}
  </section>;
}
