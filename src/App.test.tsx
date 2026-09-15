import { expect, test } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import App from './App';

test('la aplicación presenta su nombre', () => {
  expect(renderToStaticMarkup(<App />)).toContain('Finsim');
});
