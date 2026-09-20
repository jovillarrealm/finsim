import { Fragment } from 'react';
import type { Currency, LoanResult } from '../../domain/scenario';
import { formatMoney, formatMonth } from '../../format';

export default function MonthlyTable({ result, currency, onSelectMonth }: { result: LoanResult; currency: Currency; onSelectMonth?: (month: number) => void }) {
  return <details className="panel monthly-table"><summary>Detalle mes a mes <span className="muted">· {result.rows.length} meses</span></summary>
    <p className="muted">Importes en {currency}. Abre un mes para consultar cargos, distribución de pagos y atrasos.</p>
    <div className="table-scroll" role="region" aria-label="Cronograma mensual, desplazamiento horizontal" tabIndex={0}>
      <table><caption>Capital, cargos y pagos de cada mes</caption><thead><tr>
        <th scope="col">Mes</th><th scope="col">Saldo inicial</th><th scope="col">Interés generado</th><th scope="col">Seguro generado</th>
        <th scope="col">Capital habitual</th><th scope="col">Capital extra</th><th scope="col">Dinero pagado</th><th scope="col">Saldo final</th><th scope="col">Detalle</th>
      </tr></thead><tbody>{result.rows.map(row => <tr key={row.month}>
        <th scope="row"><span className="month-number">{row.month}</span> {formatMonth(row.date)}</th>
        {[row.openingBalance, row.interestCharged, row.insuranceCharged, row.scheduled.principal, row.extra.principal, row.cashPaid, row.closingBalance].map((value, index) => <td key={index}>{formatMoney(value, currency)}</td>)}
        <td><details><summary>Ver mes {row.month}</summary><div className="month-detail">
          {onSelectMonth && <button className="button secondary" type="button" onClick={() => onSelectMonth(row.month)}>Editar mes {row.month}</button>}
          <h3>Distribución de pagos</h3><p className="muted">Primero seguros, luego intereses y capital. Los extras se aplican después del pago habitual.</p>
          <dl>{([['Habitual', row.scheduled], ['Recuperación', row.catchup], ['Extraordinario', row.extra]] as const).map(([label, allocation]) =>
            <Fragment key={label}><dt className="allocation-heading">{label}</dt><dd />
              {([['Ofrecido', allocation.offered], ['Aplicado', allocation.applied], ['A seguros', allocation.insurance],
                ['A intereses', allocation.interest], ['A capital', allocation.principal], ['No aplicado', allocation.unapplied]] as const).map(([title, value]) =>
                <Fragment key={title}><dt>{title}</dt><dd>{formatMoney(value, currency)}</dd></Fragment>)}
            </Fragment>)}
          </dl><h3>Al cierre del mes</h3><dl>
            {([['Capital vencido', row.overduePrincipal], ['Intereses pendientes', row.pendingInterest], ['Seguros pendientes', row.pendingInsurance],
              ['Atrasos', row.arrears], ['Deuda total', row.debt]] as const).map(([label, value]) => <Fragment key={label}><dt>{label}</dt><dd>{formatMoney(value, currency)}</dd></Fragment>)}
          </dl><p className="muted">El capital vencido ya está incluido en el saldo de capital.</p>
        </div></details></td>
      </tr>)}</tbody></table>
    </div>
  </details>;
}
