const { test } = require('node:test');
const assert = require('node:assert/strict');
const { waitForStableBox } = require('../kmp/composeApp/browser-tests/stable-box.cjs');

function fixture(boxes, { count = 1 } = {}) {
  let index = 0;
  let boxCalls = 0;
  let waits = 0;
  const locator = {
    async count() { return count; },
    async boundingBox() {
      boxCalls += 1;
      const box = boxes[Math.min(index, boxes.length - 1)];
      index += 1;
      return box;
    }
  };
  return {
    locator,
    page: { async waitForTimeout() { waits += 1; } },
    calls: () => ({ boxCalls, waits })
  };
}

const rect = x => ({ x, y: 20, width: 80, height: 32 });

test('waitForStableBox returns two matching positive rectangles', async () => {
  const sample = fixture([rect(10), rect(11)]);
  assert.deepEqual(await waitForStableBox(sample.page, sample.locator), rect(11));
  assert.deepEqual(sample.calls(), { boxCalls: 2, waits: 1 });
});

test('waitForStableBox stops after the requested samples when geometry keeps moving', async () => {
  const sample = fixture([rect(0), rect(10), rect(20), rect(30), rect(40), rect(50), rect(60), rect(70), rect(80)]);
  assert.equal(await waitForStableBox(sample.page, sample.locator, 3), null);
  assert.deepEqual(sample.calls(), { boxCalls: 9, waits: 5 });
});

test('waitForStableBox returns quickly for a missing or clipped rectangle', async () => {
  const missing = fixture([], { count: 0 });
  assert.equal(await waitForStableBox(missing.page, missing.locator), null);
  assert.deepEqual(missing.calls(), { boxCalls: 0, waits: 0 });

  const clipped = fixture([{ x: 0, y: 0, width: 0, height: 0 }]);
  assert.equal(await waitForStableBox(clipped.page, clipped.locator), null);
  assert.deepEqual(clipped.calls(), { boxCalls: 2, waits: 0 });
});
