import { afterEach, expect, test, vi } from 'vitest';
import { exampleScenario, type Scenario } from '../domain/scenario';
import { loadScenario, parseScenario, saveScenario, SCENARIO_STORAGE_KEY, serializeScenario } from './scenario';

const scenario: Scenario = {
  ...exampleScenario, source: 'current', payment: '150.00',
  insurance: [
    ...exampleScenario.insurance,
    { id: 'fixed', name: 'Seguro fijo', kind: 'fixed', value: '10.00', endsAtPayoff: false, endMonth: 50 },
  ],
  events: [
    { id: 'extra', kind: 'extra', month: 1, amount: '100.01' },
    { id: 'repeat', kind: 'recurring', month: 2, endMonth: 10, every: 2, amount: '50.00' },
    { id: 'missed', kind: 'missed', month: 3 },
    { id: 'catchup', kind: 'catchup', month: 4, amount: '150.00' },
  ],
};

function memoryStorage() {
  const values = new Map<string, string>();
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => { values.set(key, value); },
  };
}

afterEach(() => vi.unstubAllGlobals());

test('JSON versionado preserva entradas, seguros y todos los eventos sin resultados derivados', () => {
  const text = serializeScenario(scenario);
  expect(JSON.parse(text).version).toBe(1);
  expect(parseScenario(text)).toEqual(scenario);
  expect(text).not.toContain('rows');
  const storage = memoryStorage();
  expect(loadScenario(storage)).toBeNull();
  saveScenario(scenario, storage);
  expect(loadScenario(storage)).toEqual(scenario);
  expect(storage.getItem(SCENARIO_STORAGE_KEY)).toBe(text);
  vi.stubGlobal('localStorage', storage);
  saveScenario(exampleScenario);
  expect(loadScenario()).toEqual(exampleScenario);
});

test('rechaza corrupción, versión desconocida, precisión indebida y datos incompletos', () => {
  expect(() => parseScenario('{')).toThrow('JSON válido');
  expect(() => parseScenario(JSON.stringify({ ...scenario, version: 2 }))).toThrow('versión');
  for (const value of [null, [], {}, { ...scenario, principal: '1.001' },
    { ...scenario, principal: 'abc' }, { ...scenario, principal: 100 },
    { ...scenario, rows: [] }, { ...scenario, events: [{ id: 'bad', kind: 'extra', month: 1201, amount: '1' }] }]) {
    expect(() => parseScenario(JSON.stringify(value))).toThrow('datos inválidos');
  }
});

test('importación inválida conserva escenario activo y guardado; carga corrupta no borra datos', () => {
  const storage = memoryStorage();
  saveScenario(scenario, storage);
  let active = scenario;
  expect(() => { active = parseScenario('{'); }).toThrow();
  expect(active).toBe(scenario);
  expect(loadScenario(storage)).toEqual(scenario);
  expect(() => saveScenario({ ...scenario, principal: '0' }, storage)).toThrow();
  expect(loadScenario(storage)).toEqual(scenario);
  storage.setItem(SCENARIO_STORAGE_KEY, '{');
  expect(() => loadScenario(storage)).toThrow('JSON válido');
  expect(storage.getItem(SCENARIO_STORAGE_KEY)).toBe('{');
});

test('fallos de acceso y cuota reportan errores españoles sin borrar la copia previa', () => {
  const storage = memoryStorage();
  saveScenario(scenario, storage);
  const blocked = { getItem: storage.getItem, setItem: () => { throw new Error('QuotaExceededError'); } };
  expect(() => saveScenario(exampleScenario, blocked)).toThrow('No se pudo guardar');
  expect(loadScenario(storage)).toEqual(scenario);
  expect(() => loadScenario({ ...storage, getItem: () => { throw new Error('SecurityError'); } }))
    .toThrow('No se pudo acceder');
  vi.stubGlobal('localStorage', undefined);
  expect(() => saveScenario(scenario)).toThrow('No se pudo guardar');
  expect(() => loadScenario()).toThrow('No se pudo acceder');
});
