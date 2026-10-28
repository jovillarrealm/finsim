import { cp, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'esbuild';

const packageRoot = path.dirname(fileURLToPath(import.meta.url));
const outputDirectory = path.resolve(packageRoot, '../build/generated/doclingResources/docling');
const dependencyRoot = path.join(packageRoot, 'node_modules');

await mkdir(outputDirectory, { recursive: true });
await build({
  entryPoints: [path.join(packageRoot, 'src/worker.mjs')],
  bundle: true,
  format: 'esm',
  platform: 'browser',
  target: 'es2022',
  outfile: path.join(outputDirectory, 'worker.mjs'),
  sourcemap: false,
  minify: false,
  logLevel: 'info',
});
await Promise.all([
  cp(
    path.join(dependencyRoot, 'onnxruntime-web/dist/ort-wasm-simd-threaded.asyncify.mjs'),
    path.join(outputDirectory, 'ort-wasm-simd-threaded.asyncify.mjs'),
  ),
  cp(
    path.join(dependencyRoot, 'onnxruntime-web/dist/ort-wasm-simd-threaded.asyncify.wasm'),
    path.join(outputDirectory, 'ort-wasm-simd-threaded.asyncify.wasm'),
  ),
  cp(
    path.join(dependencyRoot, 'pdfjs-dist/legacy/build/pdf.worker.mjs'),
    path.join(outputDirectory, 'pdf.worker.mjs'),
  ),
]);
