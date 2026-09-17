import { D } from './domain/scenario';

export function formatMoney(value: string): string {
  const [whole, cents] = new D(value).abs().toFixed(2).split('.');
  return `${new D(value).isNegative() ? '−' : ''}$ ${BigInt(whole).toLocaleString('es-CO')},${cents}`;
}

export function formatMonth(value: string): string {
  const [year, month] = value.split('-').map(Number);
  const date = new Date(0);
  date.setUTCFullYear(year, month - 1, 1);
  return new Intl.DateTimeFormat('es-CO', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(date);
}
