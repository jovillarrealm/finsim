import assert from 'node:assert/strict';
import { test } from 'node:test';
import { loadModelWithFallback } from '../src/inference-backend.mjs';

test('WebGPU initialization failure retries the pinned model on WASM', async () => {
  const devices = [];
  let fallbackNotified = false;
  const result = await loadModelWithFallback('webgpu', async device => {
    devices.push(device);
    if (device === 'webgpu') throw new TypeError('no available backend found. [webgpu] webgpuInit is not a function');
    return 'wasm-model';
  }, () => { fallbackNotified = true; });

  assert.deepEqual(devices, ['webgpu', 'wasm']);
  assert.equal(fallbackNotified, true);
  assert.deepEqual(result, { model: 'wasm-model', device: 'wasm' });
});

test('available WebGPU loads once without falling back', async () => {
  const devices = [];
  const result = await loadModelWithFallback('webgpu', async device => {
    devices.push(device);
    return 'gpu-model';
  });

  assert.deepEqual(devices, ['webgpu']);
  assert.deepEqual(result, { model: 'gpu-model', device: 'webgpu' });
});

test('unrelated WebGPU model errors and WASM errors are not hidden', async () => {
  const webgpuError = new Error('invalid model configuration');
  await assert.rejects(() => loadModelWithFallback('webgpu', async () => { throw webgpuError; }), error => error === webgpuError);
  const wasmError = new Error('WASM failed');
  await assert.rejects(() => loadModelWithFallback('wasm', async () => { throw wasmError; }), error => error === wasmError);
});
