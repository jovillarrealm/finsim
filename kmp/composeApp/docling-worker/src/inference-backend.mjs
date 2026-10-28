const WEBGPU_BACKEND_FAILURE = /webgpu|no available backend found/i;

export async function loadModelWithFallback(preferredDevice, loadModel, onFallback = () => {}) {
  try {
    return { model: await loadModel(preferredDevice), device: preferredDevice };
  } catch (error) {
    const message = String(error?.message ?? error);
    if (preferredDevice !== 'webgpu' || !WEBGPU_BACKEND_FAILURE.test(message)) throw error;
    await onFallback(error);
    return { model: await loadModel('wasm'), device: 'wasm' };
  }
}
