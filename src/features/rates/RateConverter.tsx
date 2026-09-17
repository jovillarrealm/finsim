import { useId, useState } from 'react';
import { D, decimalSchema, rateSchema, type Rate } from '../../domain/scenario';
import { convertRate } from '../../engine/rates';

export function percentageRate(kind: Rate['kind'], percentage: string, frequency: string): Rate {
  const value = new D(decimalSchema.parse(percentage.trim().replace(',', '.'))).div(100).toFixed();
  return rateSchema.parse(kind === 'nominal'
    ? { kind, value, frequency: Number(frequency) }
    : { kind, value });
}

const percent = (fraction: string) => new D(fraction).times(100).toFixed(10).replace('.', ',');

export default function RateConverter() {
  const id = useId();
  const [kind, setKind] = useState<Rate['kind']>('monthly');
  const [percentage, setPercentage] = useState('1');
  const [frequency, setFrequency] = useState('12');
  const validPercentage = decimalSchema.safeParse(percentage.trim().replace(',', '.')).success;
  const validFrequency = rateSchema.safeParse({ kind: 'nominal', value: '0', frequency: Number(frequency) }).success;
  let result: { annual: string; monthly: string; nominal: string } | undefined;
  let error = '';
  if (validPercentage && validFrequency) {
    try {
      const rate = percentageRate(kind, percentage, frequency);
      result = {
        annual: convertRate(rate, 'annual'),
        monthly: convertRate(rate, 'monthly'),
        nominal: convertRate(rate, 'nominal', Number(frequency)),
      };
    } catch {
      error = 'No fue posible convertir estos valores. Revisa la tasa y la frecuencia.';
    }
  }

  return <section className="panel" aria-labelledby={`${id}-title`}>
    <h2 id={`${id}-title`}>Conversor de tasas</h2>
    <p className="muted">Tasas equivalentes: producen el mismo crecimiento para el mismo periodo.</p>
    <div className="form-grid">
      <label className="field" htmlFor={`${id}-kind`}>Tasa de entrada
        <select id={`${id}-kind`} value={kind} onChange={event => {
          const value = event.target.value;
          if (value === 'annual' || value === 'monthly' || value === 'nominal') setKind(value);
        }}>
          <option value="annual">Efectiva anual (EA)</option>
          <option value="monthly">Efectiva mensual (EM)</option>
          <option value="nominal">Nominal anual</option>
        </select>
      </label>
      <label className="field" htmlFor={`${id}-value`}>Tasa (%)
        <input id={`${id}-value`} type="text" inputMode="decimal" value={percentage}
          maxLength={128} onChange={event => setPercentage(event.target.value)}
          aria-invalid={!validPercentage} aria-describedby={`${id}-value-help`} />
        <span id={`${id}-value-help`} className="muted">{validPercentage
          ? 'Escribe 1 para 1 % (fracción interna: 0,01). Usa coma o punto decimal, sin separadores de miles.'
          : 'Ingresa un porcentaje no negativo, con coma o punto decimal.'}</span>
      </label>
      <label className="field" htmlFor={`${id}-frequency`}>Capitalizaciones por año
        <input id={`${id}-frequency`} type="number" min="1" step="1" value={frequency}
          onChange={event => setFrequency(event.target.value)} aria-invalid={!validFrequency}
          aria-describedby={`${id}-frequency-help`} />
        <span id={`${id}-frequency-help`} className="muted">{validFrequency
          ? 'Frecuencia de la tasa nominal de entrada y de salida. 12 = mensual; 4 = trimestral.'
          : 'Ingresa un número entero positivo de capitalizaciones por año.'}</span>
      </label>
    </div>
    <div aria-live="polite">
      {error && <p role="alert">{error}</p>}
      {result && <>
        <dl>
          <dt>Efectiva anual (EA)</dt><dd>{percent(result.annual)} %</dd>
          <dt>Efectiva mensual (EM)</dt><dd>{percent(result.monthly)} %</dd>
          <dt>Nominal anual · {frequency} capitalizaciones/año</dt><dd>{percent(result.nominal)} %</dd>
        </dl>
        <p className="muted">Resultados visuales redondeados a 10 decimales; el cálculo conserva la precisión del motor.</p>
      </>}
    </div>
    <details>
      <summary>Ejemplo explicado: 1 % mensual</summary>
      <p>1 % EM se ingresa como 1 y se calcula como la fracción 0,01.</p>
      <p>EA = (1 + 0,01)¹² − 1 = 0,126825030131969720661201, equivalente a 12,6825030132 % EA.</p>
      <p>Con 12 capitalizaciones al año, la nominal anual es 12 × 1 % = 12 %. No es 12 % EA: cada mes los intereses también generan intereses.</p>
      <p>Para una nominal anual j expresada como fracción y m capitalizaciones por año: EM = (1 + j/m)^(m/12) − 1.</p>
    </details>
  </section>;
}
