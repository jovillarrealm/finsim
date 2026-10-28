const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFile } = require('node:fs/promises');
const path = require('node:path');
const { chromium, expect } = require('@playwright/test');
const { waitForStableBox } = require('./stable-box.cjs');

const url = process.env.FINSIM_WEB_URL || 'http://127.0.0.1:4174/';
const fixture = path.join(__dirname, 'fixtures/docling-synthetic/synthetic-loan-cop-01.png');

async function clickComposeControl(page, target) {
  await target.waitFor({ state: 'visible' });
  const box = await waitForStableBox(page, target);
  assert.ok(box, 'Compose control should have stable visible bounds.');
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(250);
}

test('Docling worker errors terminate the failed worker so retry creates a fresh one', async () => {
  const { MODEL_TOTAL_BYTES } = await import('../docling-worker/src/model-artifacts.mjs');
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 1000 } });
  try {
    await page.addInitScript(modelBytes => {
      window.__doclingWorkerLifecycle = { created: 0, terminated: 0 };
      window.Worker = class {
        constructor() {
          window.__doclingWorkerLifecycle.created++;
          this.onmessage = null;
          this.onerror = null;
        }

        postMessage(serialized) {
          const request = JSON.parse(serialized);
          if (request.type === 'status') {
            setTimeout(() => this.onmessage?.({
              data: JSON.stringify({ id: request.id, kind: 'result', installed: true, totalBytes: modelBytes }),
            }), 0);
          } else if (request.type === 'analyze-image') {
            setTimeout(() => this.onerror?.({ message: 'synthetic worker failure' }), 0);
          }
        }

        terminate() {
          window.__doclingWorkerLifecycle.terminated++;
        }
      };
    }, MODEL_TOTAL_BYTES);

    await page.goto(url);
    await clickComposeControl(page, page.getByRole('button', { name: 'Importar extracto', exact: true }).first());
    await expect(page.getByText('✓ Instalado y verificado', { exact: true })).toBeVisible();

    const chooserPromise = page.waitForEvent('filechooser');
    await clickComposeControl(page, page.getByRole('button', { name: 'Seleccionar PDF, PNG o JPEG', exact: true }));
    const chooser = await chooserPromise;
    await chooser.setFiles({
      name: 'synthetic-loan-cop-01.png',
      mimeType: 'image/png',
      buffer: await readFile(fixture),
    });

    const analyze = page.getByRole('button', { name: 'Analizar imagen con Granite-Docling', exact: true });
    await clickComposeControl(page, analyze);
    await expect(page.getByText('synthetic worker failure', { exact: true })).toBeVisible();
    await clickComposeControl(page, analyze);
    await expect(page.getByText('synthetic worker failure', { exact: true })).toBeVisible();

    assert.deepEqual(await page.evaluate(() => window.__doclingWorkerLifecycle), { created: 2, terminated: 2 });
  } finally {
    await browser.close();
  }
});
