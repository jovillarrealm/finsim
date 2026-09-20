import { useEffect, useState } from 'react';
import { MAX_MONTHS, scenarioSchema, type LoanEvent, type Scenario } from '../../domain/scenario';
import { parseSpanishNumber } from './LoanForm';

const names = { extra: 'Pago extra único', recurring: 'Pago extra recurrente', missed: 'Omitir cuota', catchup: 'Recuperación' };
export type EventDraft = { id: string; kind: LoanEvent['kind']; month: string; amount: string; endMonth: string; every: string };

function draftsFrom(events: LoanEvent[]): EventDraft[] {
  return events.map(event => ({ id: event.id, kind: event.kind, month: String(event.month),
    amount: 'amount' in event ? event.amount.replace('.', ',') : '',
    endMonth: String(event.kind === 'recurring' ? event.endMonth : event.month),
    every: String(event.kind === 'recurring' ? event.every : 1) }));
}

export function readEventDrafts(drafts: EventDraft[], scenario: Scenario) {
  return scenarioSchema.safeParse({ ...scenario, events: drafts.map(draft => ({
    id: draft.id, kind: draft.kind, month: Number(draft.month),
    ...(draft.kind !== 'missed' ? { amount: parseSpanishNumber(draft.amount) } : {}),
    ...(draft.kind === 'recurring' ? { endMonth: Number(draft.endMonth), every: Number(draft.every) } : {}),
  })) });
}

export default function EventEditor({ scenario, month, onChange, onPendingChange }: {
  scenario: Scenario; month: number; onChange: (value: Scenario) => boolean; onPendingChange?: (pending: boolean) => void;
}) {
  const [drafts, setDrafts] = useState(() => draftsFrom(scenario.events));
  const [error, setError] = useState('');
  const pending = JSON.stringify(drafts) !== JSON.stringify(draftsFrom(scenario.events));
  useEffect(() => { onPendingChange?.(pending); }, [pending, onPendingChange]);
  function update(id: string, change: Partial<EventDraft>) {
    setDrafts(items => items.map(item => item.id === id ? { ...item, ...change } : item));
    setError('');
  }
  function apply() {
    if (!pending) return;
    const next = readEventDrafts(drafts, scenario);
    if (!next.success) {
      const index = next.error.issues[0]?.path[1];
      setError(`${typeof index === 'number' ? `Cambio ${index + 1}: ` : ''}Revisa los meses (1 a 1.200) y el importe positivo, con coma decimal y máximo dos decimales. El mes final debe ser igual o posterior al inicial. No se aplicó ningún cambio.`);
      return;
    }
    if (onChange(next.data)) { setDrafts(draftsFrom(next.data.events)); setError(''); }
  }
  return <section aria-labelledby="events-title" className="event-editor">
    <h3 id="events-title" tabIndex={-1}>Editar meses</h3>
    <p>Añade los meses que quieras cambiar y aplica todo el lote al terminar. Primero se paga la cuota habitual; después la recuperación y los extras. Cada pago cubre seguros, intereses y finalmente capital.</p>
    <p role="status">{pending ? 'Cambios pendientes. Los gráficos y totales muestran la última simulación aplicada.' : 'Todos los cambios están aplicados.'}</p>
    <form noValidate aria-describedby={error ? 'event-error' : undefined} onSubmit={event => { event.preventDefault(); apply(); }}>
      <div className="event-drafts">
        {drafts.map((draft, index) => <fieldset key={draft.id} className="event-draft">
          <legend>Cambio {index + 1}</legend>
          <label>Tipo de evento<select value={draft.kind} onChange={event => update(draft.id, { kind: event.target.value as LoanEvent['kind'] })}>
            {Object.entries(names).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label>Mes inicial<input type="number" min="1" max={MAX_MONTHS} required value={draft.month} onChange={event => update(draft.id, { month: event.target.value })} /></label>
          {draft.kind !== 'missed' && <div className="field"><label htmlFor={`amount-${draft.id}`}>Importe ofrecido ({scenario.currency})</label><input id={`amount-${draft.id}`} inputMode="decimal" required value={draft.amount} onChange={event => update(draft.id, { amount: event.target.value })} aria-describedby={`amount-help-${draft.id}`} /><small id={`amount-help-${draft.id}`}>Ej.: 100.000,50</small></div>}
          {draft.kind === 'recurring' && <><label>Mes final inclusivo<input type="number" min="1" max={MAX_MONTHS} required value={draft.endMonth} onChange={event => update(draft.id, { endMonth: event.target.value })} /></label>
            <label>Cada cuántos meses<input type="number" min="1" max={MAX_MONTHS} required value={draft.every} onChange={event => update(draft.id, { every: event.target.value })} /></label></>}
          <button className="button ghost" type="button" aria-label={`Quitar cambio ${index + 1}`} onClick={() => { setDrafts(items => items.filter(item => item.id !== draft.id)); setError(''); }}>Quitar</button>
        </fieldset>)}
      </div>
      {drafts.length === 0 && <p>Sin eventos en este lote.</p>}
      <button className="button secondary" type="button" onClick={() => { setDrafts(items => [...items, { id: crypto.randomUUID(), kind: 'extra', month: String(month), amount: '', endMonth: String(month), every: '1' }]); setError(''); }}>Añadir mes</button>
      <span>Mes seleccionado: {month}</span>
      {error && <p id="event-error" role="alert">{error}</p>}
      <div className="event-actions">
        <button className="button" type="submit" disabled={!pending}>Aplicar cambios</button>
        <button className="button ghost" type="button" disabled={!pending} onClick={() => { setDrafts(draftsFrom(scenario.events)); setError(''); }}>Descartar cambios</button>
      </div>
    </form>
  </section>;
}
