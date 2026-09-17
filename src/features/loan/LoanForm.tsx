import { useId, useState, type FormEvent, type ReactNode } from 'react';
import { D, scenarioSchema, type Insurance, type Scenario } from '../../domain/scenario';

// Spanish input: comma decimals, optional dots separating groups of three.
export function parseSpanishNumber(input: string, percentage = false): string {
  const value = input.trim();
  if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d+)?$/.test(value) || value.length > 128) return '';
  const normalized = value.replaceAll('.', '').replace(',', '.');
  return percentage ? new D(normalized).div(100).toFixed() : normalized;
}

const editable = (value: string, percentage = false) =>
  (percentage ? new D(value).mul(100).toFixed() : value).replace('.', ',');

export function readLoanForm(data: FormData, scenario: Scenario, insurance: Insurance[]) {
  const text = (key: string) => String(data.get(key) ?? '');
  const rateKind = text('rate.kind');
  return scenarioSchema.safeParse({
    ...scenario,
    name: text('name').trim(), source: text('source'), startDate: text('startDate'),
    principal: parseSpanishNumber(text('principal')), termMonths: Number(text('termMonths')),
    payment: text('payment').trim() ? parseSpanishNumber(text('payment')) : undefined,
    rate: { kind: rateKind, value: parseSpanishNumber(text('rate.value'), true),
      ...(rateKind === 'nominal' ? { frequency: Number(text('rate.frequency')) } : {}) },
    insurance: insurance.map((item, index) => ({
      id: item.id, name: text(`insurance.${index}.name`).trim(), kind: item.kind,
      value: parseSpanishNumber(text(`insurance.${index}.value`), item.kind === 'percentage'),
      endsAtPayoff: item.endsAtPayoff,
      ...(!item.endsAtPayoff ? { endMonth: Number(text(`insurance.${index}.endMonth`)) } : {}),
    })),
  });
}

export default function LoanForm(props: { scenario: Scenario; onChange: (scenario: Scenario) => void }) {
  return <LoanFields key={JSON.stringify(props.scenario)} {...props} />;
}

function LoanFields({ scenario, onChange }: { scenario: Scenario; onChange: (scenario: Scenario) => void }) {
  const prefix = useId();
  const [insurance, setInsurance] = useState(scenario.insurance);
  const [rateKind, setRateKind] = useState(scenario.rate.kind);
  const [source, setSource] = useState(scenario.source);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [applied, setApplied] = useState(false);
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const parsed = readLoanForm(new FormData(event.currentTarget), scenario, insurance);
    if (!parsed.success) {
      setErrors(Object.fromEntries(parsed.error.issues.map(issue => [issue.path.join('.'),
        issue.code === 'invalid_type' || issue.code === 'too_small' || issue.code === 'too_big'
          ? 'Revisa el valor y los límites indicados.' : issue.message.replace('con punto decimal', 'con coma decimal')])));
      setApplied(false);
      return;
    }
    setErrors({}); setApplied(true); onChange(parsed.data);
  }
  function field(name: string, label: string, control: ReactNode, hint?: string) {
    return <div className="field"><label htmlFor={`${prefix}-${name}`}>{label}</label>{control}
      {hint && <small className="muted">{hint}</small>}
      {errors[name] && <small className="error" id={`${prefix}-${name}-error`}>{errors[name]}</small>}
    </div>;
  }
  const attrs = (name: string) => ({ name, id: `${prefix}-${name}`, 'aria-invalid': Boolean(errors[name]),
    'aria-describedby': errors[name] ? `${prefix}-${name}-error` : undefined });
  const updateInsurance = (index: number, patch: Partial<Insurance>) =>
    setInsurance(items => items.map((item, position) => position === index ? { ...item, ...patch } : item));

  return <form className="panel loan-form" onSubmit={submit} onChange={() => setApplied(false)} noValidate>
    <div className="section-heading"><div><p className="eyebrow">Punto de partida</p><h2>Tu préstamo</h2></div>
      <span className="badge">COP · tasa fija</span></div>
    <div className="form-grid">
      {field('name', 'Nombre del escenario', <input {...attrs('name')} defaultValue={scenario.name} maxLength={120} />)}
      {field('source', '¿Desde dónde empezamos?', <select {...attrs('source')} value={source}
        onChange={event => setSource(event.target.value as Scenario['source'])}>
        <option value="original">Préstamo original</option><option value="current">Saldo actual al día</option></select>)}
      {field('principal', source === 'current' ? 'Capital pendiente (COP)' : 'Capital inicial (COP)',
        <input {...attrs('principal')} inputMode="decimal" defaultValue={editable(scenario.principal)} />, 'Ej.: 20.000.000,00. Máximo dos decimales.')}
      {field('startDate', 'Mes del primer pago simulado', <input {...attrs('startDate')} type="month" defaultValue={scenario.startDate} />)}
      {field('rate.kind', 'Tipo de tasa', <select {...attrs('rate.kind')} value={rateKind}
        onChange={event => setRateKind(event.target.value as Scenario['rate']['kind'])}>
        <option value="annual">Efectiva anual (EA)</option><option value="monthly">Efectiva mensual (EM)</option>
        <option value="nominal">Nominal anual vencida</option></select>)}
      {field('rate.value', 'Tasa (%)', <input {...attrs('rate.value')} inputMode="decimal" defaultValue={editable(scenario.rate.value, true)} />, 'Ej.: 18 significa 18 %. Usa coma decimal.')}
      {rateKind === 'nominal' && field('rate.frequency', 'Capitalizaciones por año',
        <input {...attrs('rate.frequency')} type="number" min="1" step="1" defaultValue={scenario.rate.kind === 'nominal' ? scenario.rate.frequency : 12} />, '12 = mensual; 4 = trimestral; 1 = anual.')}
      {field('termMonths', source === 'current' ? 'Plazo restante (meses)' : 'Plazo (meses)',
        <input {...attrs('termMonths')} type="number" min="1" max="1200" step="1" defaultValue={scenario.termMonths} />, 'Entre 1 y 1.200 meses.')}
      {field('payment', 'Cuota conocida (COP, opcional)', <input {...attrs('payment')} inputMode="decimal" defaultValue={scenario.payment ? editable(scenario.payment) : ''} placeholder="Calcular según el plazo" />, 'Solo capital e intereses. El seguro se suma aparte.')}
    </div>
    {source === 'current' && <p className="note">El saldo representa capital al inicio del próximo periodo. Suponemos que estás al día; no incluye atrasos previos.</p>}
    <fieldset className="insurance-list"><legend>Seguros mensuales</legend>
      <p className="muted">Los porcentajes se calculan sobre el capital al inicio del mes, antes de pagar.</p>
      {insurance.map((item, index) => <fieldset className="insurance-item" key={item.id}><legend>Seguro {index + 1}</legend>
        <div className="form-grid">
          {field(`insurance.${index}.name`, 'Nombre', <input {...attrs(`insurance.${index}.name`)} defaultValue={item.name} />)}
          {field(`insurance.${index}.kind`, 'Tipo de cargo', <select {...attrs(`insurance.${index}.kind`)} value={item.kind}
            onChange={event => updateInsurance(index, { kind: event.target.value as Insurance['kind'] })}>
            <option value="fixed">Importe fijo (COP)</option><option value="percentage">Porcentaje mensual (%)</option></select>)}
          {field(`insurance.${index}.value`, item.kind === 'fixed' ? 'Importe mensual (COP)' : 'Porcentaje mensual (%)',
            <input {...attrs(`insurance.${index}.value`)} inputMode="decimal" defaultValue={editable(item.value, item.kind === 'percentage')} />)}
          {field(`insurance.${index}.endsAtPayoff`, 'Final del seguro', <select {...attrs(`insurance.${index}.endsAtPayoff`)} value={String(item.endsAtPayoff)}
            onChange={event => updateInsurance(index, { endsAtPayoff: event.target.value === 'true' })}>
            <option value="true">Termina al liquidar el préstamo</option><option value="false">Continúa hasta un mes definido</option></select>)}
          {!item.endsAtPayoff && field(`insurance.${index}.endMonth`, 'Último mes de cobro (inclusive)',
            <input {...attrs(`insurance.${index}.endMonth`)} type="number" min="1" max="1200" step="1" defaultValue={item.endMonth ?? scenario.termMonths} />, 'Mes 1 = primer mes de la simulación.')}
        </div>
        <button className="button ghost" type="button" onClick={() => setInsurance(items => items.filter((_, position) => position !== index))}>Quitar seguro {index + 1}</button>
      </fieldset>)}
      <button className="button secondary" type="button" disabled={insurance.length >= 30} onClick={() => setInsurance(items => [...items,
        { id: crypto.randomUUID(), name: 'Seguro', kind: 'fixed', value: '0', endsAtPayoff: true }])}>+ Añadir seguro</button>
      {errors.insurance && <p className="error">{errors.insurance}</p>}
    </fieldset>
    <div className="form-footer"><button className="button" type="submit">Aplicar cambios</button>
      <span className="muted">Edita y aplica para actualizar la simulación.</span></div>
    {Object.keys(errors).length > 0 && <p role="alert" className="error">Revisa los campos marcados. Conservamos la última simulación válida.</p>}
    {applied && <p role="status" className="success">Cambios aplicados.</p>}
  </form>;
}
