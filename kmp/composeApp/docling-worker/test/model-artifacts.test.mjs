import assert from 'node:assert/strict';
import { createHash, webcrypto } from 'node:crypto';
import { test } from 'node:test';
import {
  MODEL_ARTIFACTS,
  MODEL_ID,
  MODEL_REMOTE_PATH_TEMPLATE,
  MODEL_REVISION,
  MODEL_TOTAL_BYTES,
  artifactForUrl,
  modelArtifactUrl,
  readVerifiedResponse,
  verifyArtifactBytes,
} from '../src/model-artifacts.mjs';
import { isAllowedResourceRequest, requestHasBody, resolveResourceUrl } from '../src/worker-network-policy.mjs';

test('pinned manifest has unique artifacts and a reproducible total', () => {
  assert.equal(new Set(MODEL_ARTIFACTS.map(item => item.path)).size, MODEL_ARTIFACTS.length);
  assert.equal(MODEL_TOTAL_BYTES, MODEL_ARTIFACTS.reduce((sum, item) => sum + item.bytes, 0));
  assert.ok(MODEL_TOTAL_BYTES > 200_000_000);
  assert.match(MODEL_REVISION, /^[a-f0-9]{40}$/);
  assert.equal(MODEL_REMOTE_PATH_TEMPLATE, `{model}/resolve/${MODEL_REVISION}/`);
});

test('only exact revision and manifest paths are classified as downloadable model assets', () => {
  const first = MODEL_ARTIFACTS[0];
  assert.equal(artifactForUrl(modelArtifactUrl(first.path)), first);
  assert.equal(artifactForUrl(new URL(modelArtifactUrl(first.path))), first);
  assert.equal(artifactForUrl(`https://huggingface.co/${MODEL_ID}/resolve/main/${first.path}`), null);
  assert.equal(artifactForUrl(`https://example.invalid/${MODEL_ID}/resolve/${MODEL_REVISION}/${first.path}`), null);
  assert.throws(() => modelArtifactUrl('../document.pdf'));
});

test('only bodyless GET/HEAD reaches same-origin and pinned model URLs', () => {
  const baseUrl = new URL('https://finsim.example/app/');
  const localUrl = resolveResourceUrl('docling/worker.mjs', baseUrl);
  const modelUrl = new URL(modelArtifactUrl(MODEL_ARTIFACTS[0].path));
  const externalUrl = resolveResourceUrl(new URL('https://attacker.example/upload'), baseUrl);
  const pinnedArtifact = artifactForUrl(modelUrl) !== null;

  assert.equal(isAllowedResourceRequest(localUrl, 'GET', null, baseUrl, false), true);
  assert.equal(isAllowedResourceRequest(localUrl, 'HEAD', undefined, baseUrl, false), true);
  assert.equal(isAllowedResourceRequest(localUrl, 'POST', null, baseUrl, false), false);
  assert.equal(isAllowedResourceRequest(localUrl, 'GET', new Uint8Array([1]), baseUrl, false), false);
  assert.equal(isAllowedResourceRequest(modelUrl, 'GET', null, baseUrl, pinnedArtifact), true);
  assert.equal(isAllowedResourceRequest(modelUrl, 'HEAD', null, baseUrl, pinnedArtifact), true);
  assert.equal(isAllowedResourceRequest(modelUrl, 'GET', new Uint8Array([1]), baseUrl, pinnedArtifact), false);
  assert.equal(isAllowedResourceRequest(modelUrl, 'POST', null, baseUrl, pinnedArtifact), false);
  assert.equal(isAllowedResourceRequest(externalUrl, 'GET', null, baseUrl, false), false);
});

test('detects bodies inherited from Request even when init.body is undefined', () => {
  const request = new Request('https://finsim.example/app/docling/worker.mjs', {
    method: 'POST',
    body: 'sensitive payload',
  });

  assert.equal(requestHasBody(request), true);
  assert.equal(requestHasBody(request, { method: 'GET', body: undefined }), true);
  assert.equal(requestHasBody('https://finsim.example/app/docling/worker.mjs', { body: 'payload' }), true);
  assert.equal(requestHasBody('https://finsim.example/app/docling/worker.mjs', { method: 'GET', body: null }), false);
  assert.equal(requestHasBody('https://finsim.example/app/docling/worker.mjs', { method: 'HEAD' }), false);
});

test('rejects inherited Request bodies on both local and pinned URLs', () => {
  const baseUrl = new URL('https://finsim.example/app/');
  const request = new Request('https://finsim.example/app/docling/worker.mjs', {
    method: 'POST',
    body: 'sensitive payload',
  });
  const localUrl = resolveResourceUrl(request, baseUrl);
  const pinnedUrl = new URL(modelArtifactUrl(MODEL_ARTIFACTS[0].path));
  const body = requestHasBody(request, { method: 'GET', body: undefined }) ? {} : null;

  assert.equal(isAllowedResourceRequest(localUrl, 'GET', body, baseUrl, false), false);
  assert.equal(isAllowedResourceRequest(pinnedUrl, 'GET', body, baseUrl, true), false);
});

test('verified download accepts expected bytes and rejects altered size or content', async () => {
  const bytes = new TextEncoder().encode('synthetic model artifact');
  const artifact = {
    path: 'fixture.bin',
    bytes: bytes.byteLength,
    sha256: createHash('sha256').update(bytes).digest('hex'),
  };
  assert.equal(await verifyArtifactBytes(artifact, bytes, webcrypto.subtle), true);
  assert.equal(await verifyArtifactBytes(artifact, new Uint8Array([...bytes, 0]), webcrypto.subtle), false);
  assert.equal(await verifyArtifactBytes(artifact, new TextEncoder().encode('altered'), webcrypto.subtle), false);
  const response = new Response(bytes, { status: 200 });
  assert.deepEqual(await readVerifiedResponse(response, artifact), bytes);
  await assert.rejects(() => readVerifiedResponse(new Response('bad', { status: 200 }), artifact), /SHA-256/);
  await assert.rejects(() => readVerifiedResponse(new Response(bytes, { status: 206 }), artifact), /HTTP 206/);
});
