import assert from 'node:assert/strict';
import test from 'node:test';
import { evaluateWebCases, parseTapResults, validateReusableBuild, validateWebCatalog } from './verify-web.mjs';

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

test('reusable Wasm build accepts only an unchanged product source and exact artifact', () => {
  const previous = {
    build: { status: 'PASS', exitCode: 0 },
    steps: [
      { id: 'WEB-TOOLCHAIN', status: 'PASS' },
      { id: 'WEB-BUILD', status: 'PASS' },
    ],
    source: {
      commit: 'abc123',
      workingTree: { sha256: 'old-tree', trackedFilesChanged: 4, untrackedFiles: 2 },
    },
    artifact: { sha256: 'artifact-1' },
  };
  const current = {
    commit: 'abc123',
    workingTree: { sha256: 'new-tree', trackedFilesChanged: 4, untrackedFiles: 2 },
  };

  assert.deepEqual(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['scripts/verify-web.mjs', 'scripts/verify-web.test.mjs', 'kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'],
    buildInputChangesSince: [],
    artifactHash: 'artifact-1',
  }), []);
  assert.deepEqual(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['kmp/composeApp/browser-tests/issue-40-financial-ui.test.cjs'],
    buildInputChangesSince: [],
    artifactHash: 'artifact-1',
  }), []);
  assert.match(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['kmp/composeApp/browser-tests/issue-41-transfer.test.cjs'],
    buildInputChangesSince: [],
    artifactHash: 'artifact-1',
  }).join('\n'), /hash completo del árbol difiere/i);
  assert.match(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['package.json'],
    buildInputChangesSince: [],
    artifactHash: 'artifact-1',
  }).join('\n'), /hash completo del árbol difiere/i);
  assert.match(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['scripts/verify-web.mjs', 'kmp/shared/src/commonMain/Loan.kt'],
    buildInputChangesSince: ['kmp/shared/src/commonMain/Loan.kt'],
    artifactHash: 'artifact-1',
  }).join('\n'), /entrada de producto/i);
  assert.match(validateReusableBuild({
    previous,
    current,
    changedSinceBuild: ['scripts/verify-web.mjs'],
    buildInputChangesSince: [],
    artifactHash: 'artifact-2',
  }).join('\n'), /hash del artefacto/i);
});
