const fs = require('node:fs');
const path = require('node:path');
const { randomUUID } = require('node:crypto');

let screenshotSequence = 0;

async function capture(page, label, { fullPage = false } = {}) {
  const directory = process.env.FINSIM_WEB_DIAGNOSTICS;
  if (!directory) return null;

  const traceFile = path.join(directory, 'trace.log');
  try {
    const filename = `${Date.now()}-${process.pid}-${++screenshotSequence}-${randomUUID()}-${String(label).replace(/[^a-z0-9-]/gi, '-')}.png`;
    const file = path.join(directory, filename);
    fs.mkdirSync(directory, { recursive: true });
    await page.screenshot({ path: file, ...(fullPage ? { fullPage: true } : {}) });
    fs.appendFileSync(traceFile, `${new Date().toISOString()} ${label} screenshot=${filename}\n`);
    return filename;
  } catch (error) {
    // Captures are best-effort so they never replace the test's original failure.
    const detail = error?.message ?? String(error);
    try {
      fs.mkdirSync(directory, { recursive: true });
      fs.appendFileSync(traceFile, `${new Date().toISOString()} capture:error ${label} ${detail}\n`);
    } catch (traceError) {
      try {
        console.error(`FINSIM_WEB_DIAGNOSTICS capture failed (${label}): ${detail}; trace failed: ${traceError?.message ?? traceError}`);
      } catch {}
    }
    return null;
  }
}

module.exports = { capture };
