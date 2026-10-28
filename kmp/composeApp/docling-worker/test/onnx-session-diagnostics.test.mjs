import assert from 'node:assert/strict';
import test from 'node:test';
import { instrumentInferenceSessions } from '../src/onnx-session-diagnostics.mjs';

test('records ONNX session and run timing without inputs or outputs', async () => {
  const events = [];
  class FakeInferenceSession {
    static async create() { return new FakeInferenceSession(); }
    inputNames = ['inputs_embeds'];
    async run() { return { logits: 'synthetic document text' }; }
  }

  const restore = instrumentInferenceSessions(FakeInferenceSession, () => 'request-1', (_id, event) => events.push(event));
  try {
    const session = await FakeInferenceSession.create(null, { executionProviders: ['wasm'] });
    await session.run({ input_ids: 'synthetic document text' });
  } finally {
    restore();
  }

  assert.deepEqual(events.map(event => event.stage), [
    'onnx-session-create-start', 'onnx-session-created', 'onnx-run-start', 'onnx-run-complete',
  ]);
  assert.equal(events[1].component, 'decoder');
  assert.equal(events[0].providers[0], 'wasm');
  assert.equal(JSON.stringify(events).includes('synthetic document text'), false);
});
