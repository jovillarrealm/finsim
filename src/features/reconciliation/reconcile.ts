import { z } from 'zod';
import { D, money, moneySchema, monthSchema, type MonthRow } from '../../domain/scenario';

export const statementSchema = z.object({
  month: monthSchema,
  interest: moneySchema.optional(),
  insurance: moneySchema.optional(),
  principal: moneySchema.optional(),
  balance: moneySchema.optional(),
}).strict().refine(row => [row.interest, row.insurance, row.principal, row.balance].some(value => value !== undefined),
  'Introduce al menos un importe del extracto.');

export type StatementRow = z.infer<typeof statementSchema>;
export type StatementField = Exclude<keyof StatementRow, 'month'>;
export const fieldLabels: Record<StatementField, string> = {
  interest: 'Interés cobrado', insurance: 'Seguro cobrado', principal: 'Capital pagado', balance: 'Saldo final',
};

export function reconcileRow(input: StatementRow, rows: MonthRow[]) {
  const statement = statementSchema.parse(input);
  const row = rows.find(item => item.month === statement.month);
  if (!row) throw new Error('Ese mes no existe en el cronograma seleccionado.');
  const simulated: Record<StatementField, string> = {
    interest: row.interestCharged,
    insurance: row.insuranceCharged,
    principal: money(new D(row.scheduled.principal).plus(row.catchup.principal).plus(row.extra.principal)),
    balance: row.closingBalance,
  };
  return (Object.keys(fieldLabels) as StatementField[]).flatMap(field => {
    const actual = statement[field];
    return actual === undefined ? [] : [{ field, actual, simulated: simulated[field],
      difference: money(new D(actual).minus(simulated[field])) }];
  });
}
