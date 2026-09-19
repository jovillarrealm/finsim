import { useState } from 'react';
import { MAX_MONTHS, scenarioSchema, type LoanEvent, type Scenario } from '../../domain/scenario';
import { formatMoney } from '../../format';

const names = { extra: 'Pago extra único', recurring: 'Pago extra recurrente', missed: 'Omitir cuota', catchup: 'Recuperación' };

export default function EventEditor({ scenario, month, onChange }: { scenario: Scenario; month: number; onChange: (value: Scenario) => void }) {
  const [editing, setEditing] = useState<LoanEvent | null>(null);
  const [kind, setKind] = useState<LoanEvent['kind']>('extra');
  const [error, setError] = useState('');
  function save(form: HTMLFormElement) {
    const values = new FormData(form);
    const event = { id: editing?.id ?? crypto.randomUUID(), kind, month: Number(values.get('month')),
      ...(kind !== 'missed' ? { amount: String(values.get('amount')).replace(',', '.') } : {}),
      ...(kind === 'recurring' ? { endMonth: Number(values.get('endMonth')), every: Number(values.get('every')) } : {}) };
    const next = scenarioSchema.safeParse({ ...scenario, events: editing
      ? scenario.events.map(item => item.id === editing.id ? event : item) : [...scenario.events, event] });
    if (!next.success) { setError(next.error.issues.map(issue => issue.message).join(' ')); return; }
    onChange(next.data); setEditing(null); setError('');
  }
  return <section aria-labelledby="events-title" className="event-editor">
    <h3 id="events-title">Editar mes {month}</h3>
    <p>Primero se paga la cuota habitual; después la recuperación y los extras. Cada pago cubre seguros, intereses y finalmente capital. Un pago extra solo es abono a capital por la parte aplicada a capital.</p>
    <form key={`${editing?.id ?? 'new'}-${month}`} onSubmit={event => { event.preventDefault(); save(event.currentTarget); }}>
      <label>Tipo de evento<select value={kind} onChange={event => setKind(event.target.value as LoanEvent['kind'])}>
        {Object.entries(names).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>
      <label>Mes inicial<input name="month" type="number" min="1" max={MAX_MONTHS} required defaultValue={editing?.month ?? month} /></label>
      {kind !== 'missed' && <label>Importe ofrecido (COP)<input name="amount" inputMode="decimal" required defaultValue={editing && 'amount' in editing ? editing.amount : ''} aria-describedby={error ? 'event-error' : undefined} /></label>}
      {kind === 'recurring' && <><label>Mes final inclusivo<input name="endMonth" type="number" min="1" max={MAX_MONTHS} required defaultValue={editing?.kind === 'recurring' ? editing.endMonth : month} /></label>
        <label>Cada cuántos meses<input name="every" type="number" min="1" max={MAX_MONTHS} required defaultValue={editing?.kind === 'recurring' ? editing.every : 1} /></label></>}
      {error && <p id="event-error" role="alert">{error}</p>}
      <button type="submit">{editing ? 'Guardar evento' : 'Agregar evento'}</button>
      {editing && <button type="button" onClick={() => { setEditing(null); setError(''); }}>Cancelar edición</button>}
    </form>
    {scenario.events.length === 0 ? <p>Sin eventos. Ambos escenarios coinciden.</p> : <ul className="event-list">
      {scenario.events.map(event => <li key={event.id}><span>{names[event.kind]} · mes {event.month}
        {event.kind === 'recurring' ? ` al ${event.endMonth}, cada ${event.every}` : ''}
        {'amount' in event ? ` · ${formatMoney(event.amount)}` : ''}</span>
        <button type="button" aria-label={`Editar ${names[event.kind]} del mes ${event.month}`} onClick={() => { setEditing(event); setKind(event.kind); setError(''); }}>Editar</button>
        <button type="button" aria-label={`Quitar ${names[event.kind]} del mes ${event.month}`} onClick={() => { onChange({ ...scenario, events: scenario.events.filter(item => item.id !== event.id) }); if (editing?.id === event.id) setEditing(null); }}>Quitar</button>
      </li>)}
    </ul>}
  </section>;
}
