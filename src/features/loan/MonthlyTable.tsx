import { Fragment, useState } from 'react';
import { D, type Currency, type LoanResult } from '../../domain/scenario';
import { formatMoney, formatMonth } from '../../format';

function exportScheduleCsv(result: LoanResult, currency: Currency) {
  const headers = [
    'Mes',
    'Fecha',
    'Saldo Inicial',
    'Interés Generado',
    'Seguro Generado',
    'Capital Habitual',
    'Capital Extra',
    'Dinero Pagado',
    'Saldo Final',
  ];
  const rows = result.rows.map(r => [
    r.month,
    formatMonth(r.date),
    r.openingBalance,
    r.interestCharged,
    r.insuranceCharged,
    r.scheduled.principal,
    r.extra.principal,
    r.cashPaid,
    r.closingBalance,
  ]);
  const csvContent = [headers.join(','), ...rows.map(row => row.map(v => `"${v}"`).join(','))].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `cronograma-amortizacion-${currency}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export default function MonthlyTable({
  result,
  currency,
  onSelectMonth,
}: {
  result: LoanResult;
  currency: Currency;
  onSelectMonth?: (month: number) => void;
}) {
  const [viewMode, setViewMode] = useState<'milestones' | 'table'>('milestones');

  // Filtrado Curado de Hitos Decisivos (T10)
  const rows = result.rows;
  const prepaymentMonths = new Set(rows.filter(r => new D(r.extra.principal).gt(0)).map(r => r.month));
  const milestoneMonthSet = new Set<number>();

  if (rows.length > 0) {
    milestoneMonthSet.add(1); // Mes 01: Inicio
    for (const m of prepaymentMonths) {
      if (m > 1) milestoneMonthSet.add(m - 1); // Mes previo al impacto
      milestoneMonthSet.add(m); // Mes de abono extraordinario
    }
    if (result.payoffMonth) {
      milestoneMonthSet.add(result.payoffMonth); // Liquidación anticipada
    } else {
      milestoneMonthSet.add(rows[rows.length - 1].month);
    }
    // Si no hay abonos, incluir mes intermedio para dar perspectiva
    if (prepaymentMonths.size === 0 && rows.length > 2) {
      milestoneMonthSet.add(Math.floor(rows.length / 2));
    }
  }

  const milestoneRows = rows.filter(r => milestoneMonthSet.has(r.month));

  return (
    <section className="panel monthly-table" aria-label="Cronograma de amortización">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Desglose de amortización</p>
          <h2>Cronograma Clave</h2>
        </div>
        <div className="schedule-view-toggle" role="tablist" aria-label="Formato del cronograma">
          <button
            type="button"
            role="tab"
            aria-selected={viewMode === 'milestones'}
            className={viewMode === 'milestones' ? 'active' : ''}
            onClick={() => setViewMode('milestones')}
          >
            Hitos Clave ({milestoneRows.length})
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={viewMode === 'table'}
            className={viewMode === 'table' ? 'active' : ''}
            onClick={() => setViewMode('table')}
          >
            Tabla Completa ({result.rows.length})
          </button>
        </div>
      </div>

      <p className="muted">
        Importes en {currency}. Consulta los hitos decisivos del crédito o examina la tabla completa mes a mes.
      </p>

      {/* VISTA 1: Hitos Clave Curados (T10) */}
      {viewMode === 'milestones' && (
        <div className="milestones-schedule-list" role="region" aria-label="Lista de hitos decisivos de amortización">
          {milestoneRows.map(row => {
            const isPayoff = result.payoffMonth === row.month || new D(row.closingBalance).eq(0);
            const isExtra = new D(row.extra.principal).gt(0);

            return (
              <div
                key={row.month}
                className={`milestone-item ${
                  isPayoff ? 'highlight-payoff' : isExtra ? 'highlight-abono' : ''
                }`}
              >
                <div
                  className={`milestone-icon ${
                    isPayoff ? 'icon-verified' : isExtra ? 'icon-extra' : 'icon-regular'
                  }`}
                  aria-hidden="true"
                >
                  {isPayoff ? '✓' : isExtra ? '⚡' : `M${String(row.month).padStart(2, '0')}`}
                </div>
                <div className="milestone-info">
                  <div className="milestone-title-row">
                    <span className="milestone-title">
                      Mes {String(row.month).padStart(2, '0')} · {formatMonth(row.date)}
                    </span>
                    {isExtra && (
                      <span className="milestone-badge badge-extra">
                        +{formatMoney(row.extra.principal, currency)} Extra
                      </span>
                    )}
                    {isPayoff && <span className="milestone-badge badge-payoff">Fin de Crédito</span>}
                  </div>
                  <span className="milestone-subtitle">
                    {isPayoff
                      ? 'Deuda extinguida al 100%'
                      : isExtra
                      ? 'Salto directo de amortización a capital'
                      : `Cuota: ${formatMoney(row.cashPaid, currency)} · Cap ${formatMoney(
                          row.scheduled.principal,
                          currency
                        )} | Int ${formatMoney(row.interestCharged, currency)}`}
                  </span>
                </div>
                <div className="milestone-metrics">
                  <span className={`milestone-amount ${isExtra ? 'extra-amount' : ''}`}>
                    {isPayoff ? 'SALDO $0,00' : formatMoney(row.cashPaid, currency)}
                  </span>
                  <span className="milestone-balance">
                    {isPayoff ? '100% Amortizado' : `Saldo: ${formatMoney(row.closingBalance, currency)}`}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* VISTA 2: Tabla Completa Exhaustiva en 2D */}
      {viewMode === 'table' && (
        <div
          className="table-scroll"
          role="region"
          aria-label="Cronograma mensual completo, desplazamiento horizontal"
          tabIndex={0}
        >
          <table>
            <caption>Capital, cargos y pagos de cada mes</caption>
            <thead>
              <tr>
                <th scope="col">Mes</th>
                <th scope="col">Saldo inicial</th>
                <th scope="col">Interés generado</th>
                <th scope="col">Seguro generado</th>
                <th scope="col">Capital habitual</th>
                <th scope="col">Capital extra</th>
                <th scope="col">Dinero pagado</th>
                <th scope="col">Saldo final</th>
                <th scope="col">Detalle</th>
              </tr>
            </thead>
            <tbody>
              {result.rows.map(row => (
                <tr key={row.month}>
                  <th scope="row">
                    <span className="month-number">{row.month}</span> {formatMonth(row.date)}
                  </th>
                  {[
                    row.openingBalance,
                    row.interestCharged,
                    row.insuranceCharged,
                    row.scheduled.principal,
                    row.extra.principal,
                    row.cashPaid,
                    row.closingBalance,
                  ].map((value, index) => (
                    <td key={index}>{formatMoney(value, currency)}</td>
                  ))}
                  <td>
                    <details>
                      <summary>Ver mes {row.month}</summary>
                      <div className="month-detail">
                        {onSelectMonth && (
                          <button
                            className="button secondary"
                            type="button"
                            onClick={() => onSelectMonth(row.month)}
                          >
                            Editar mes {row.month}
                          </button>
                        )}
                        <h3>Distribución de pagos</h3>
                        <p className="muted">
                          Primero seguros, luego intereses y capital. Los extras se aplican después del pago habitual.
                        </p>
                        <dl>
                          {(
                            [
                              ['Habitual', row.scheduled],
                              ['Recuperación', row.catchup],
                              ['Extraordinario', row.extra],
                            ] as const
                          ).map(([label, allocation]) => (
                            <Fragment key={label}>
                              <dt className="allocation-heading">{label}</dt>
                              <dd />
                              {(
                                [
                                  ['Ofrecido', allocation.offered],
                                  ['Aplicado', allocation.applied],
                                  ['A seguros', allocation.insurance],
                                  ['A intereses', allocation.interest],
                                  ['A capital', allocation.principal],
                                  ['No aplicado', allocation.unapplied],
                                ] as const
                              ).map(([title, value]) => (
                                <Fragment key={title}>
                                  <dt>{title}</dt>
                                  <dd>{formatMoney(value, currency)}</dd>
                                </Fragment>
                              ))}
                            </Fragment>
                          ))}
                        </dl>
                        <h3>Al cierre del mes</h3>
                        <dl>
                          {(
                            [
                              ['Capital vencido', row.overduePrincipal],
                              ['Intereses pendientes', row.pendingInterest],
                              ['Seguros pendientes', row.pendingInsurance],
                              ['Atrasos', row.arrears],
                              ['Deuda total', row.debt],
                            ] as const
                          ).map(([label, value]) => (
                            <Fragment key={label}>
                              <dt>{label}</dt>
                              <dd>{formatMoney(value, currency)}</dd>
                            </Fragment>
                          ))}
                        </dl>
                        <p className="muted">El capital vencido ya está incluido en el saldo de capital.</p>
                      </div>
                    </details>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Botón de exportación CSV accesible */}
      <div className="schedule-actions">
        <button
          className="button ghost"
          type="button"
          onClick={() => exportScheduleCsv(result, currency)}
        >
          <span aria-hidden="true">📥</span> Descargar cronograma completo (.CSV)
        </button>
      </div>
    </section>
  );
}

