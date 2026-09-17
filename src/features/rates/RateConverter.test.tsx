import { expect, test } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import RateConverter, { percentageRate } from './RateConverter';
import { convertRate } from '../../engine/rates';

test('interpreta porcentajes sin redondeo visual y valida entradas', () => {
  expect(percentageRate('monthly', '1,234567890123456789', '12')).toEqual({
    kind: 'monthly', value: '0.01234567890123456789',
  });
  expect(convertRate(percentageRate('monthly', '1', '12'), 'annual')).toBe('0.126825030131969720661201');
  expect(convertRate(percentageRate('nominal', '12', '12'), 'monthly')).toBe('0.01');
  expect(percentageRate('annual', '0', '12').value).toBe('0');
  for (const value of ['', '-1', 'Infinity', '1.000,5', '1e3']) {
    expect(() => percentageRate('annual', value, '12')).toThrow();
  }
  for (const frequency of ['', '0', '-1', '1.5', 'Infinity']) {
    expect(() => percentageRate('nominal', '12', frequency)).toThrow();
  }
});

test('muestra referencias equivalentes y controles nativos etiquetados', () => {
  const html = renderToStaticMarkup(<RateConverter />);
  expect(html).toContain('12,6825030132');
  expect(html).toContain('1,0000000000');
  expect(html).toContain('12,0000000000');
  expect(html.match(/<label/g)).toHaveLength(3);
  expect(html).toContain('Capitalizaciones por año');
  expect(html).toContain('<summary>Ejemplo explicado');
});
