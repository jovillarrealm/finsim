import { expect, test } from 'vitest';
import { type Allocation, type MonthRow } from '../../domain/scenario';
import { reconcileRow, statementSchema } from './reconcile';

const zero: Allocation = { offered: '0.00', applied: '0.00', principal: '0.00', insurance: '0.00', interest: '0.00', unapplied: '0.00' };
const row: MonthRow = {
  month: 1, date: '2026-10', openingBalance: '1000.00', interestCharged: '10.00', insuranceCharged: '0.00',
  scheduled: { ...zero, offered: '507.51', applied: '507.51', interest: '10.00', principal: '497.51' },
  catchup: zero, extra: zero, closingBalance: '502.49', overduePrincipal: '0.00', pendingInterest: '0.00', pendingInsurance: '0.00', arrears: '0.00', debt: '502.49', cashPaid: '507.51',
};

test('muestra igualdad y diferencia de extracto contra referencia independiente', () => {
  expect(reconcileRow({ month: 1, interest: '10.00', principal: '497.51', balance: '502.49' }, [row]).map(item => item.difference)).toEqual(['0.00', '0.00', '0.00']);
  expect(reconcileRow({ month: 1, interest: '11.25' }, [row])[0].difference).toBe('1.25');
  expect(() => reconcileRow({ month: 2, balance: '0.00' }, [row])).toThrow('Ese mes no existe');
  expect(statementSchema.safeParse({ month: 1 }).success).toBe(false);
  expect(statementSchema.safeParse({ month: 1, interest: '1.001' }).success).toBe(false);
});
