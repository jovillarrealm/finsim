async function stableBox(page, locator) {
  if (!await locator.count()) return null;
  const first = await locator.boundingBox();
  if (!first?.width || !first.height) return null;
  await page.waitForTimeout(100);
  const second = await locator.boundingBox();
  if (!second?.width || !second.height) return null;
  return Math.abs(first.x - second.x) <= 2 && Math.abs(first.y - second.y) <= 2 &&
    Math.abs(first.width - second.width) <= 2 && Math.abs(first.height - second.height) <= 2
    ? second
    : null;
}

async function waitForStableBox(page, locator, maxSamples = 5) {
  for (let sample = 0; sample < maxSamples; sample += 1) {
    const box = await stableBox(page, locator);
    if (box) return box;
    if (!await locator.count()) return null;
    const current = await locator.boundingBox();
    if (!current?.width || !current.height) return null;
    if (sample + 1 < maxSamples) await page.waitForTimeout(100);
  }
  return null;
}

module.exports = { stableBox, waitForStableBox };
