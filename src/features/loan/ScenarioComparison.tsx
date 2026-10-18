import { useCallback, useEffect, useRef, useState } from 'react';
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
    return {
      month: index + 1,
      exact,
      scheduled: Number(exact.scheduled),
      extra: Number(exact.extra),
      interest: Number(exact.interest),
      insurance: Number(exact.insurance),
      cash: Number(exact.cash),
    };
  });
}

export const signedMoney = (value: string, currency: Currency = 'COP') =>
  `${new D(value).gt(0) ? '+' : ''}${formatMoney(value, currency)}`;

export const chartMoney = (
  row: ReturnType<typeof chartRows>[number],
  key: keyof ReturnType<typeof chartRows>[number]['exact'],
  currency: Currency = 'COP'
) => formatMoney(row.exact[key], currency);

export default function ScenarioComparison({
  scenario,
  original,
  modified,
  onChange,
  selectedMonth,
  onSelectMonth,
  onPendingChange,
}: {
  scenario: Scenario;
  original: LoanResult;
  modified: LoanResult;
  onChange: (value: Scenario) => boolean;
  onPendingChange?: (pending: boolean) => void;
  selectedMonth?: number;
  onSelectMonth?: (month: number) => void;
}) {
  const [internalMonth, setInternalMonth] = useState(1);
  const [chartMode, setChartMode] = useState<'trajectory' | 'stacked'>('trajectory');
  const month = selectedMonth ?? internalMonth;

  const select = (next: number) => {
    if (Number.isInteger(next) && next >= 1 && next <= MAX_MONTHS) {
      setInternalMonth(next);
      onSelectMonth?.(next);
    }
  };

  const months = Math.max(
    original.rows.at(-1)?.month ?? 1,
    modified.rows.at(-1)?.month ?? 1,
    original.payoffMonth ?? 1,
    modified.payoffMonth ?? 1
  );

  const originalData = chartRows(original, months);
  const modifiedData = chartRows(modified, months);
  const ceiling = Math.max(1, ...originalData.map(r => r.cash), ...modifiedData.map(r => r.cash));
  const complete = original.complete && modified.complete;
  const row = modified.rows.find(item => item.month === month);
  const deltaMonths =
    original.payoffMonth !== null && modified.payoffMonth !== null
      ? modified.payoffMonth - original.payoffMonth
      : null;

  const [containerWidth, setContainerWidth] = useState(720);
  const observerRef = useRef<ResizeObserver | null>(null);

  const containerCallbackRef = useCallback((el: HTMLDivElement | null) => {
    if (observerRef.current) {
      observerRef.current.disconnect();
      observerRef.current = null;
    }
    if (el && typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(entries => {
        for (const entry of entries) {
          const w = Math.round(entry.contentRect.width);
          if (w > 0) setContainerWidth(w);
        }
      });
      observer.observe(el);
      observerRef.current = observer;
    }
  }, []);

  useEffect(() => {
    return () => {
      observerRef.current?.disconnect();
    };
  }, []);

  // Trajectory SVG coordinates calculation
  const principalNum = Number(scenario.principal) || 1;
  const svgLeft = 60;
  const svgRight = 25;
  const svgTop = 20;
  const svgBottom = 35;
  const svgWidth = Math.max(300, containerWidth);
  const svgHeight = 240;
  const chartW = Math.max(10, svgWidth - svgLeft - svgRight);
  const chartH = svgHeight - svgTop - svgBottom;

  const getX = (m: number) => svgLeft + (m / months) * chartW;
  const getY = (bal: number) => svgTop + (1 - Math.min(Math.max(bal, 0), principalNum) / principalNum) * chartH;

  // Build original points
  const origPts = [{ x: getX(0), y: getY(principalNum) }];
  for (const r of original.rows) {
    origPts.push({ x: getX(r.month), y: getY(Number(r.closingBalance)) });
  }
  if (original.payoffMonth && original.payoffMonth < months) {
    for (let m = original.payoffMonth + 1; m <= months; m++) {
      origPts.push({ x: getX(m), y: getY(0) });
    }
  }

  // Build modified points
  const modPts = [{ x: getX(0), y: getY(principalNum) }];
  for (const r of modified.rows) {
    modPts.push({ x: getX(r.month), y: getY(Number(r.closingBalance)) });
  }
  if (modified.payoffMonth && modified.payoffMonth < months) {
    for (let m = modified.payoffMonth + 1; m <= months; m++) {
      modPts.push({ x: getX(m), y: getY(0) });
    }
  }

  const origPath = 'M ' + origPts.map(p => `${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' L ');
  const modPath = 'M ' + modPts.map(p => `${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' L ');
  const gapPolygonPoints = [...modPts, ...origPts.slice().reverse()]
    .map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(' ');

  // Extinction area
  const hasExtinction =
    modified.payoffMonth !== null &&
    original.payoffMonth !== null &&
    modified.payoffMonth < original.payoffMonth;
  const extXStart = hasExtinction ? getX(modified.payoffMonth!) : 0;
  const extXEnd = hasExtinction ? getX(original.payoffMonth!) : 0;
  const extWidth = Math.max(0, extXEnd - extXStart);

  // Prepayment milestones on modified curve
  const prepaymentMilestones = modified.rows.filter(r => new D(r.extra.principal).gt(0));

  const cursorX = getX(Math.min(month, months));
  const activeModRow = modified.rows.find(r => r.month === month);
  const activeBal = activeModRow
    ? Number(activeModRow.closingBalance)
    : month > (modified.payoffMonth ?? months)
    ? 0
    : principalNum;
  const cursorY = getY(activeBal);

  return (
    <section className="panel scenario-comparison" aria-labelledby="comparison-title">
      <div className="section-heading">
        <div>
          <p className="eyebrow">El corazón del simulador</p>
          <h2 id="comparison-title">Original y modificado</h2>
        </div>
      </div>
      <p>
        Diferencias = modificado − original. Signo positivo: más costo o más tiempo; negativo: menos.
        Los extras conservan la cuota y reducen plazo.
      </p>
      {!complete && (
        <p role="status" className="note warning">
          Resultado incompleto: los importes son acumulados hasta el límite simulado, no costos finales ni ahorros definitivos.
        </p>
      )}

      {/* Tarjetas de Diferencias (Deltas) */}
      <dl className="comparison-deltas">
        {(['interest', 'insurance', 'cashPaid'] as const).map((key, index) => (
          <div key={key}>
            <dt>
              {['Diferencia de intereses pagados', 'Diferencia de seguros pagados', 'Diferencia de caja pagada'][index]}
              {!complete ? ' (parcial)' : ''}
            </dt>
            <dd>
              {signedMoney(
                money(new D(modified.totals[key]).minus(original.totals[key])),
                scenario.currency
              )}
            </dd>
          </div>
        ))}
        <div>
          <dt>Diferencia de plazo</dt>
          <dd>
            {complete && deltaMonths !== null
              ? `${deltaMonths > 0 ? '+' : ''}${deltaMonths} meses`
              : 'Sin liquidación completa'}
          </dd>
        </div>
      </dl>

      <p className="muted" style={{ fontSize: '0.84rem' }}>
        Liquidación original: {original.payoffDate ? formatMonth(original.payoffDate) : 'No alcanzada'} · Modificado:{' '}
        {modified.payoffDate ? formatMonth(modified.payoffDate) : 'No alcanzada'}.
      </p>

      {/* Selector de Modo de Gráfico */}
      <div className="chart-mode-toolbar">
        <div className="chart-mode-toggles" role="tablist" aria-label="Modo de gráfico">
          <button
            type="button"
            role="tab"
            aria-selected={chartMode === 'trajectory'}
            className={chartMode === 'trajectory' ? 'active' : ''}
            onClick={() => setChartMode('trajectory')}
          >
            <span aria-hidden="true">📈</span>
            <span>Trayectoria de saldo</span>
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={chartMode === 'stacked'}
            className={chartMode === 'stacked' ? 'active' : ''}
            onClick={() => setChartMode('stacked')}
          >
            <span aria-hidden="true">📊</span>
            <span>Composición mensual</span>
          </button>
        </div>
        <div className="trajectory-legend">
          <span className="trajectory-legend-item">
            <span className="legend-line-original"></span> Original ({original.payoffMonth ?? months}m)
          </span>
          <span className="trajectory-legend-item">
            <span className="legend-line-modified"></span> Con abonos ({modified.payoffMonth ?? months}m)
          </span>
          {hasExtinction && (
            <span className="trajectory-legend-item">
              <span className="legend-box-gap"></span> Brecha amortizada
            </span>
          )}
        </div>
      </div>

      {/* VISTA 1: Gráfico de Trayectoria SVG (T08) */}
      {chartMode === 'trajectory' && (
        <div className="trajectory-card">
          <div className="trajectory-svg-container" ref={containerCallbackRef}>
            <svg
              className="trajectory-svg"
              viewBox={`0 0 ${svgWidth} ${svgHeight}`}
              onClick={e => {
                const rect = e.currentTarget.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const scaleX = rect.width > 0 ? svgWidth / rect.width : 1;
                const chartSvgX = clickX * scaleX;
                const ratio = (chartSvgX - svgLeft) / chartW;
                const targetM = Math.round(ratio * months);
                if (targetM >= 1 && targetM <= months) select(targetM);
              }}
            >
              <defs>
                <linearGradient id="amortDeltaGradient" x1="0" x2="0" y1="0" y2="1">
                  <stop offset="0%" stopColor="#0D9488" stopOpacity="0.22" />
                  <stop offset="100%" stopColor="#10B981" stopOpacity="0.04" />
                </linearGradient>
                <linearGradient id="extinctionGradient" x1="0" x2="1" y1="0" y2="1">
                  <stop offset="0%" stopColor="#ECFDF5" stopOpacity="0.95" />
                  <stop offset="100%" stopColor="#F0FDF4" stopOpacity="0.95" />
                </linearGradient>
              </defs>

              {/* Guías Horizontales y Eje Y */}
              {[1, 0.75, 0.5, 0.25, 0].map(ratio => {
                const yPos = svgTop + (1 - ratio) * chartH;
                const val = principalNum * ratio;
                return (
                  <g key={ratio}>
                    <line
                      x1={svgLeft}
                      y1={yPos}
                      x2={svgLeft + chartW}
                      y2={yPos}
                      stroke="#CBD5C6"
                      strokeDasharray="3 3"
                      strokeWidth="1"
                      opacity="0.6"
                    />
                    <text
                      x={svgLeft - 8}
                      y={yPos + 4}
                      fill="#717974"
                      fontSize="10"
                      textAnchor="end"
                      fontFamily="Inter, sans-serif"
                    >
                      {new Intl.NumberFormat('es-CO', { notation: 'compact' }).format(val)}
                    </text>
                  </g>
                );
              })}

              {/* Zona de Extinción Anticipada (T08) */}
              {hasExtinction && (
                <g>
                  <rect
                    x={extXStart}
                    y={svgTop}
                    width={extWidth}
                    height={chartH}
                    fill="url(#extinctionGradient)"
                    stroke="#A7F3D0"
                    strokeDasharray="3 3"
                    strokeWidth="1.5"
                    rx="4"
                  />
                  {extWidth >= 70 && (
                    <>
                      <text
                        x={extXStart + extWidth / 2}
                        y={svgTop + chartH / 2 - 6}
                        fill="#065F46"
                        fontSize="11"
                        fontWeight="700"
                        letterSpacing="0.06em"
                        textAnchor="middle"
                      >
                        LIBRE DE DEUDA
                      </text>
                      <text
                        x={extXStart + extWidth / 2}
                        y={svgTop + chartH / 2 + 10}
                        fill="#047857"
                        fontSize="9"
                        fontWeight="500"
                        textAnchor="middle"
                      >
                        $0 pagos requeridos
                      </text>
                    </>
                  )}
                  {extWidth >= 40 && extWidth < 70 && (
                    <text
                      x={extXStart + extWidth / 2}
                      y={svgTop + chartH / 2 + 4}
                      fill="#065F46"
                      fontSize="9"
                      fontWeight="700"
                      textAnchor="middle"
                    >
                      LIBRE
                    </text>
                  )}
                </g>
              )}

              {/* Polígono de Brecha Amortizada */}
              <polygon points={gapPolygonPoints} fill="url(#amortDeltaGradient)" />

              {/* 1. Curva Original (Punteada) */}
              <path
                d={origPath}
                fill="none"
                stroke="#717974"
                strokeDasharray="5 4"
                strokeWidth="2"
                opacity="0.85"
              />

              {/* 2. Curva Modificada (Sólida Verde Teal) */}
              <path
                d={modPath}
                fill="none"
                stroke="#0D9488"
                strokeWidth="3.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              />

              {/* Badges de Hito de Abono */}
              {prepaymentMilestones.map(r => {
                const mx = getX(r.month);
                const my = getY(Number(r.closingBalance));
                const badgeWidth = 64;
                const badgeHeight = 16;
                const badgeX = Math.max(svgLeft, Math.min(mx - badgeWidth / 2, svgWidth - svgRight - badgeWidth));
                return (
                  <g key={r.month}>
                    <circle cx={mx} cy={my} r="5" fill="#0D9488" stroke="#FFFFFF" strokeWidth="2" />
                    <rect
                      x={badgeX}
                      y={Math.max(my - 22, svgTop)}
                      width={badgeWidth}
                      height={badgeHeight}
                      rx="3"
                      fill="#002217"
                      opacity="0.9"
                    />
                    <text
                      x={badgeX + badgeWidth / 2}
                      y={Math.max(my - 11, svgTop + 11)}
                      fill="#c4f278"
                      fontSize="8.5"
                      fontWeight="700"
                      textAnchor="middle"
                    >
                      M{r.month}: -{new Intl.NumberFormat('es-CO', { notation: 'compact' }).format(Number(r.extra.principal))}
                    </text>
                  </g>
                );
              })}

              {/* Marcador de Liquidación Anticipada */}
              {modified.payoffMonth && (
                <g>
                  <circle
                    cx={getX(modified.payoffMonth)}
                    cy={getY(0)}
                    r="5.5"
                    fill="#059669"
                    stroke="#FFFFFF"
                    strokeWidth="2"
                  />
                </g>
              )}

              {/* Cursor Interactivo de Referencia */}
              <line
                x1={cursorX}
                y1={svgTop}
                x2={cursorX}
                y2={svgTop + chartH}
                stroke="#111827"
                strokeDasharray="3 2"
                strokeWidth="1.5"
                opacity="0.75"
              />
              <circle cx={cursorX} cy={cursorY} r="5" fill="#0D9488" stroke="#FFFFFF" strokeWidth="2" />
            </svg>
          </div>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: '0.4rem 0.8rem',
              fontSize: '0.72rem',
              color: 'var(--muted)',
              paddingLeft: '55px',
              paddingRight: '20px',
              marginTop: '4px',
            }}
          >
            <span>Mes 0 (Inicio)</span>
            {prepaymentMilestones.map(r => (
              <span key={r.month} style={{ color: '#0d9488', fontWeight: 700 }}>
                ▲ Mes {r.month}
              </span>
            ))}
            {modified.payoffMonth && (
              <span style={{ color: '#065f46', fontWeight: 700 }}>
                ★ Mes {modified.payoffMonth} (Cero Deuda)
              </span>
            )}
            <span style={{ textDecoration: 'line-through' }}>Mes {months}</span>
          </div>
        </div>
      )}

      {/* VISTA 2: Composición Mensual Apilada (T09) */}
      {chartMode === 'stacked' && (
        <div className="comparison-charts">
          {[
            { name: 'Original', data: originalData },
            { name: 'Modificado', data: modifiedData },
          ].map(chart => (
            <figure key={chart.name}>
              <figcaption>
                {chart.name} · pagos mensuales ({scenario.currency})
              </figcaption>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={chart.data}
                  accessibilityLayer
                  onClick={state => {
                    if (state.activeLabel !== undefined) select(Number(state.activeLabel));
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="month" label={{ value: 'Mes', position: 'insideBottomRight', offset: -4 }} />
                  <YAxis
                    width={68}
                    domain={[0, ceiling]}
                    tickFormatter={value =>
                      new Intl.NumberFormat('es-CO', { notation: 'compact' }).format(Number(value))
                    }
                  />
                  <Tooltip
                    formatter={(_value, _name, item) =>
                      chartMoney(item.payload, item.dataKey as keyof typeof originalData[number]['exact'], scenario.currency)
                    }
                    labelFormatter={label => `Mes ${label}`}
                  />
                  <Legend />
                  <Bar dataKey="scheduled" name="Capital habitual" stackId="cash" fill="#2563EB" isAnimationActive={false} />
                  <Bar dataKey="extra" name="Capital extra y recuperación" stackId="cash" fill="#0D9488" isAnimationActive={false} />
                  <Bar dataKey="interest" name="Intereses pagados" stackId="cash" fill="#D97706" isAnimationActive={false} />
                  <Bar dataKey="insurance" name="Seguros pagados" stackId="cash" fill="#7C3AED" isAnimationActive={false} />
                  <ReferenceLine x={month} stroke="#111827" />
                </BarChart>
              </ResponsiveContainer>
            </figure>
          ))}
        </div>
      )}

      {/* Panel de Inspección Táctil del Mes Activo (Stitch 268286d1 / 5b0f011c) */}
      <div className="active-month-card">
        <div className="active-month-header">
          <div className="active-month-badge">
            <span className="eyebrow">MES</span>
            <span className="active-month-num">{String(month).padStart(2, '0')}</span>
          </div>
          <div className="active-month-info">
            <h4>
              Detalle del Mes {month} {row ? `(${formatMonth(row.date)})` : ''}
            </h4>
            <p className="muted">
              {row
                ? `Cuota: ${formatMoney(row.scheduled.applied, scenario.currency)} · Interés: ${formatMoney(
                    row.interestCharged,
                    scenario.currency
                  )} · Seguro: ${formatMoney(row.insuranceCharged, scenario.currency)}`
                : 'Sin pagos en el cronograma modificado.'}
            </p>
          </div>
        </div>

        <div className="active-month-actions">
          {row && new D(row.extra.principal).gt(0) && (
            <div className="extra-callout">
              <span className="muted" style={{ fontSize: '0.72rem' }}>Abono extraordinario a capital:</span>
              <span className="extra-amount">
                -{formatMoney(row.extra.principal, scenario.currency)}
              </span>
            </div>
          )}
          <button
            type="button"
            className="button secondary"
            onClick={() => {
              const el = document.getElementById('events-title');
              el?.focus();
              el?.scrollIntoView({ behavior: 'smooth' });
            }}
          >
            Modificar este mes
          </button>
        </div>
      </div>

      <label className="month-selector">
        Mes para editar
        <input
          aria-label="Mes para editar"
          type="number"
          min="1"
          max={MAX_MONTHS}
          value={month}
          onChange={event => select(Number(event.target.value))}
        />
      </label>

      <p aria-live="polite">
        Mes {month}
        {row
          ? ` (${formatMonth(row.date)}): habitual ${formatMoney(
              row.scheduled.applied,
              scenario.currency
            )}, recuperación ${formatMoney(row.catchup.applied, scenario.currency)}, extra ${formatMoney(
              row.extra.applied,
              scenario.currency
            )}. Capital extra y recuperación: ${formatMoney(
              money(new D(row.extra.principal).plus(row.catchup.principal)),
              scenario.currency
            )}. No aplicado: ${formatMoney(
              money(new D(row.extra.unapplied).plus(row.catchup.unapplied)),
              scenario.currency
            )}.`
          : ': sin pagos en el cronograma modificado.'}
      </p>

      <EventEditor
        key={JSON.stringify(scenario.events)}
        scenario={scenario}
        month={month}
        onChange={onChange}
        onPendingChange={onPendingChange}
      />

      {modified.unappliedEvents.length > 0 && (
        <details>
          <summary>Eventos posteriores no aplicados ({modified.unappliedEvents.length})</summary>
          <ul>
            {modified.unappliedEvents.map(event => (
              <li key={`${event.eventId}-${event.month}`}>
                Mes {event.month}: {formatMoney(event.amount, scenario.currency)}. {event.reason}
              </li>
            ))}
          </ul>
        </details>
      )}
    </section>
  );
}

