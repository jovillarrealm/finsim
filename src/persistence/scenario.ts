import { scenarioSchema, type Scenario } from '../domain/scenario';

export const SCENARIO_STORAGE_KEY = 'finsim.scenario';
type ScenarioStorage = Pick<Storage, 'getItem' | 'setItem'>;

function validateScenario(value: unknown): Scenario {
  if (typeof value === 'object' && value !== null && 'version' in value && value.version !== 1)
    throw new Error('La versión del escenario no es compatible. Se admite la versión 1.');
  try {
    return scenarioSchema.parse(value);
  } catch {
    throw new Error('El escenario contiene datos inválidos. Revisa los campos, importes, tasas y meses.');
  }
}

export function serializeScenario(scenario: Scenario): string {
  return JSON.stringify(validateScenario(scenario), null, 2);
}

export function parseScenario(text: string): Scenario {
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    throw new Error('El archivo no contiene JSON válido.');
  }
  return validateScenario(value);
}

export function saveScenario(scenario: Scenario, storage?: ScenarioStorage): void {
  // Validate before touching storage; setItem atomically replaces the previous value.
  const text = serializeScenario(scenario);
  try {
    (storage ?? globalThis.localStorage).setItem(SCENARIO_STORAGE_KEY, text);
  } catch {
    throw new Error('No se pudo guardar el escenario en este navegador. Exporta una copia JSON.');
  }
}

export function loadScenario(storage?: ScenarioStorage): Scenario | null {
  let text: string | null;
  try {
    text = (storage ?? globalThis.localStorage).getItem(SCENARIO_STORAGE_KEY);
  } catch {
    throw new Error('No se pudo acceder al escenario guardado en este navegador.');
  }
  // Never remove corrupt data automatically or replace the caller’s active scenario.
  return text === null ? null : parseScenario(text);
}
