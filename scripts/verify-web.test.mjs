import assert from 'node:assert/strict';
import test from 'node:test';
import { evaluateWebCases, loadWebCatalogs, parseTapResults, validateWebCatalog } from './verify-web.mjs';

test('TAP results preserve exact case titles, failures, and skips', () => {
  const results = parseTapResults(`
    TAP version 13
    # Subtest: browser-tests/example.test.cjs
        # Subtest: [WEB-40] works
        ok 1 - [WEB-40] works
        # Subtest: [WEB-41] fails
        not ok 2 - [WEB-41] fails
        # Subtest: [WEB-42] skipped
        ok 3 - [WEB-42] skipped # SKIP browser missing
    1..3
  `);

  assert.deepEqual(results, [
    { title: '[WEB-40] works', status: 'PASS' },
    { title: '[WEB-41] fails', status: 'FAIL' },
    { title: '[WEB-42] skipped', status: 'NOT_RUN' },
  ]);
});

test('a browser expectation only passes on one exact, freshly reported title', () => {
  const catalog = [{
    id: 'WEB-40-X',
    title: '[WEB-40-X] exact title',
    requirement: 'UI-02',
    expectation: 'Exact behavior.',
    expectationSource: 'docs/financial-conventions.md §5',
    platform: 'Chrome WebAssembly',
    source: 'kmp/composeApp/browser-tests/issue-40-example.test.cjs',
  }];
  assert.equal(evaluateWebCases(catalog, [{ title: catalog[0].title, status: 'PASS' }])[0].status, 'PASS');
  assert.equal(evaluateWebCases(catalog, [{ title: `${catalog[0].title} extra`, status: 'PASS' }])[0].status, 'NOT_RUN');
  assert.equal(evaluateWebCases(catalog, [
    { title: catalog[0].title, status: 'PASS' },
    { title: catalog[0].title, status: 'PASS' },
  ])[0].status, 'FAIL');
});

test('web catalogs require unique stable IDs and complete evidence fields', () => {
  const entry = {
    id: 'WEB-40-X', title: '[WEB-40-X] exact title', requirement: 'UI-02',
    expectation: 'Exact behavior.', expectationSource: 'a checked reference',
    platform: 'Chrome WebAssembly', source: 'test.cjs',
  };
  assert.deepEqual(validateWebCatalog([entry]), []);
  assert.match(validateWebCatalog([entry, { ...entry, title: 'other title' }]).join('\n'), /ID duplicado/i);
  assert.match(validateWebCatalog([{ ...entry, expectationSource: '' }]).join('\n'), /expectationSource/i);
});

test('issue 40 mixed-navigation and result-geometry cases have exact catalog entries', async () => {
  const { cases } = await loadWebCatalogs();
  assert.deepEqual(validateWebCatalog(cases), []);
  const origin = cases.find(item => item.id === 'WEB-40-ORIGIN-A11Y');
  const resultGeometry = cases.find(item => item.id === 'WEB-40-TNUM-UI');

  assert.equal(origin?.title, '[WEB-40-ORIGIN-A11Y] Keep the form tree while selecting payment origin with keyboard after wheel scrolling');
  assert.match(origin?.expectation ?? '', /wheel|rueda/i);
  assert.equal(resultGeometry?.title, '[WEB-40-TNUM-UI] Visible result width follows text length without changing for equal-length amounts');
  assert.match(resultGeometry?.expectation ?? '', /bounding boxes|cajas/i);
  assert.match(resultGeometry?.expectation ?? '', /rasterización/i);
});
