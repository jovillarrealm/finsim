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

test('records run 3 start even when it has no completion event yet', async () => {
  const events = [];
  let completeThirdRun;
  class FakeInferenceSession {
    static async create() { return new FakeInferenceSession(); }
    inputNames = ['pixel_values'];
    runCount = 0;
    async run() {
      this.runCount += 1;
      if (this.runCount === 3) return new Promise(resolve => { completeThirdRun = resolve; });
      return {};
    }
  }

  const restore = instrumentInferenceSessions(FakeInferenceSession, () => 'request-3', (_id, event) => events.push(event));
  try {
    const session = await FakeInferenceSession.create(null, { executionProviders: ['webgpu'] });
    await session.run({ pixel_values: 'private image tensor' });
    await session.run({ pixel_values: 'private image tensor' });
    const thirdRun = session.run({ pixel_values: 'private image tensor' });
    await Promise.resolve();

    assert.deepEqual(events.filter(event => event.stage === 'onnx-run-start').map(event => event.runIndex), [1, 2, 3]);
    assert.equal(events.some(event => event.stage === 'onnx-run-complete' && event.runIndex === 3), false);
    assert.equal(JSON.stringify(events).includes('private image tensor'), false);

    completeThirdRun({});
    await thirdRun;
  } finally {
    restore();
  }
});

test('run start reports are bounded per session and request', async () => {
  const events = [];
  class FakeInferenceSession {
    static async create() { return new FakeInferenceSession(); }
    inputNames = ['pixel_values'];
    async run() { return {}; }
  }

  const restore = instrumentInferenceSessions(FakeInferenceSession, () => 'request-bounded', (_id, event) => events.push(event));
  try {
    const session = await FakeInferenceSession.create(null, { executionProviders: ['wasm'] });
    for (let index = 0; index < 300; index += 1) await session.run({ pixel_values: 'private image tensor' });
  } finally {
    restore();
  }

  const starts = events.filter(event => event.stage === 'onnx-run-start');
  assert.equal(starts.length, 256);
  assert.equal(starts.at(-1).runIndex, 256);
  assert.equal(JSON.stringify(events).includes('private image tensor'), false);
});
