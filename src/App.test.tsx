import { expect, test } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import App from './App';

test('la aplicación muestra el préstamo de ejemplo y sus herramientas', () => {
  const html = renderToStaticMarkup(<App />);
  expect(html.includes('Ejemplo · Crédito de libre inversión')).toBe(true);
  expect(html.includes('Convertir tasas')).toBe(true);
  expect(html.includes('Comparar extracto')).toBe(true);
});
