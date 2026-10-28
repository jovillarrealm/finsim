import assert from 'node:assert/strict';
import test from 'node:test';
import { inspectBrowserStorage, isAllowedNetworkRequest, readAccessiblePageText, report } from './verify-docling.mjs';
import { summarizeVerification } from './verification-status.mjs';
import { MODEL_ARTIFACTS, modelArtifactUrl } from '../kmp/composeApp/docling-worker/src/model-artifacts.mjs';

test('external GET with a document query is blocked even without a body', () => {
  const request = { url: 'https://collector.example/collect?document=SYNTHETIC-CONTENT', method: 'GET', hasBody: false };
  const previousCheck = [request].filter(item => !['GET', 'HEAD'].includes(item.method) || item.hasBody);

  assert.equal(previousCheck.length, 0, 'the old check incorrectly treated this request as safe');
  assert.equal(isAllowedNetworkRequest(request, 'http://127.0.0.1:4173'), false);
});

test('only pinned model artifacts and their known redirect chain are allowed externally', () => {
  const artifact = modelArtifactUrl(MODEL_ARTIFACTS[0].path);
  const largeArtifact = modelArtifactUrl('onnx/decoder_model_merged_q4f16.onnx_data');

  assert.equal(isAllowedNetworkRequest({ url: artifact, method: 'GET' }, 'http://127.0.0.1:4173'), true);
  assert.equal(isAllowedNetworkRequest({ url: `${artifact}?document=SYNTHETIC-CONTENT`, method: 'GET' }, 'http://127.0.0.1:4173'), false);
  assert.equal(isAllowedNetworkRequest({ url: artifact, method: 'POST' }, 'http://127.0.0.1:4173'), false);
  assert.equal(isAllowedNetworkRequest({
    url: 'https://us.aws.cdn.hf.co/xet-bridge-us/abc123/abcdef?signature=download-only',
    method: 'GET', redirectedFrom: [largeArtifact],
  }, 'http://127.0.0.1:4173'), true);
  assert.equal(isAllowedNetworkRequest({
    url: 'https://us.aws.cdn.hf.co/xet-bridge-us/abc123/abcdef?document=SYNTHETIC-CONTENT',
    method: 'GET',
  }, 'http://127.0.0.1:4173'), false);
});

test('profile text is readable when body.innerText omits Compose shadow-DOM content', async () => {
  const selectors = [];
  const page = {
    locator(selector) {
      selectors.push(selector);
      if (selector === 'body') return { innerText: async () => '' };
      assert.equal(selector, 'body *');
      return { allInnerTexts: async () => ['Instalar modelo local', 'Modelo local Granite-Docling 258M'] };
    },
  };

  assert.equal(await page.locator('body').innerText(), '');
  assert.equal(await readAccessiblePageText(page), 'Instalar modelo local\nModelo local Granite-Docling 258M');
  assert.deepEqual(selectors, ['body', 'body *']);
});

test('Cache Storage response bodies are scanned for synthetic document markers', async () => {
  const previous = new Map(['localStorage', 'sessionStorage', 'caches', 'indexedDB']
    .map(key => [key, Object.getOwnPropertyDescriptor(globalThis, key)]));
  const fakeStorage = { length: 0, key: () => null, getItem: () => null };
  const marker = 'SYNTHETIC-STATEMENT-CONTENT';
  const cacheRequest = new Request('https://app.example/cache/entry');
  Object.defineProperties(globalThis, {
    localStorage: { configurable: true, value: fakeStorage },
    sessionStorage: { configurable: true, value: fakeStorage },
    caches: { configurable: true, value: {
      keys: async () => ['example-cache'],
      open: async () => ({
        keys: async () => [cacheRequest],
        match: async () => new Response(new ReadableStream({
          start(controller) {
            const middle = Math.floor(marker.length / 2);
            controller.enqueue(new TextEncoder().encode(`body:${marker.slice(0, middle)}`));
            controller.enqueue(new TextEncoder().encode(`${marker.slice(middle)}:tail`));
            controller.close();
          },
        })),
      }),
    } },
    indexedDB: { configurable: true, value: { databases: async () => [] } },
  });

  try {
    const result = await inspectBrowserStorage({ markers: [{ id: 'fixture-text', value: marker }], pdfBytes: [1, 2, 3], cookieValues: [] });
    assert.deepEqual(result.markerIds, ['fixture-text']);
    assert.deepEqual(result.markerLocations, [{ id: 'fixture-text', store: 'Cache Storage response body' }]);
    assert.equal(result.storage.cacheStorage.status, 'INSPECTED');
  } finally {
    for (const [key, descriptor] of previous) {
      if (descriptor) Object.defineProperty(globalThis, key, descriptor);
      else delete globalThis[key];
    }
  }
});

test('PDF-only PASS evidence remains incomplete for PNG, JPEG, ambiguity, and global privacy', () => {
  const pdfOnlyPasses = report.steps.map(step => step.id.endsWith('-PDF') ? { ...step, status: 'PASS' } : step);
  const summary = summarizeVerification(pdfOnlyPasses);

  assert.equal(report.coverage.criteria['DOC-01'].PNG, 'NOT_RUN');
  assert.equal(report.coverage.criteria['DOC-01'].JPEG, 'NOT_RUN');
  assert.equal(report.coverage.ambiguity, 'NOT_RUN');
  assert.equal(report.steps.find(step => step.id === 'SEC-01').status, 'NOT_RUN');
  assert.equal(summary.status, 'INCOMPLETE');
  assert.equal(pdfOnlyPasses.some(step => ['DOC-01', 'DOC-02/03/04'].includes(step.id)), false);
});
