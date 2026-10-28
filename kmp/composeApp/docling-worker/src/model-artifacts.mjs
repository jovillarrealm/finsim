export const MODEL_ID = 'onnx-community/granite-docling-258M-ONNX';
export const MODEL_REVISION = 'e8602580df77443fc3421cf3bae0601da601e5c6';
export const MODEL_ORIGIN = 'https://huggingface.co';
export const MODEL_REMOTE_PATH_TEMPLATE = `{model}/resolve/${MODEL_REVISION}/`;
export const MODEL_CACHE_NAME = `finsim-docling-${MODEL_REVISION}`;

export const MODEL_ARTIFACTS = Object.freeze([
  { path: 'added_tokens.json', bytes: 35, sha256: '882e49757a3bea90e00df1d2dc8c18b2b526ff77380db20439feeb1f053a63cd' },
  { path: 'chat_template.jinja', bytes: 588, sha256: 'a4b0bf4348b014acca131da664d05b61e39d5dc8af980f1913846a57ccc6d289' },
  { path: 'preprocessor_config.json', bytes: 486, sha256: '6cb6e36d6fcb88ca1502c4a26750715dc3e7dedddc9a8f17b27d8d167d1457e7' },
  { path: 'processor_config.json', bytes: 68, sha256: 'e7bff42da73ae9eec9042ef20e066e11f1ee20f025358ff79131e3c0fb549b46' },
  { path: 'tokenizer_config.json', bytes: 18616, sha256: 'e584ead05a3639a2e026b77b39f94175f5fb5be63ad8a30f90fea8fa1bc852a5' },
  { path: 'tokenizer.json', bytes: 4131145, sha256: '1cbe567c74f76ff6713446ed1cfb9bd50bbd164ae99e59e79d81b5efabe01113' },
  { path: 'config.json', bytes: 1819, sha256: '74ea7ab2bb6f22521171c747abbc1562b4c7d28a98a3b6708f36db2ac31caef2' },
  { path: 'generation_config.json', bytes: 174, sha256: 'bf47a1f276dec7f6fa0a926e7aeeb5dc22087359d46777410b0bb24282bb4325' },
  { path: 'merges.txt', bytes: 916646, sha256: 'b6fe424e334903f7fb84d3a106d9730455f4744b9fe3c21ee136d97a00e72502' },
  { path: 'special_tokens_map.json', bytes: 1078, sha256: '3e1c6950fd5624d1e6d60eea7110882c9743e37fa643364e17094466331e1cb5' },
  { path: 'vocab.json', bytes: 1612698, sha256: 'e2bad0ce74e6ddfece426ef99e18067f4a6c3d4e65bb6dd9cfd638450cca0142' },
  { path: 'onnx/embed_tokens_quantized.onnx', bytes: 767, sha256: 'b9832aee6f125d71ff52fd4a31d305ecd44d721a746a18856663514600ac5d74' },
  { path: 'onnx/embed_tokens_quantized.onnx_data', bytes: 57802752, sha256: 'f384533f15a3caf44f8fc541984776e6d06c24c2251752e13c8aa7b34907b810' },
  { path: 'onnx/vision_encoder_q4f16.onnx', bytes: 315763, sha256: '422362254ffb41f59d4fe13785a26056b637fa024770bc99e7547f9d31a72c21' },
  { path: 'onnx/vision_encoder_q4f16.onnx_data', bytes: 54753792, sha256: 'd9d41942328cd341e7487bf91b9334247de88e4a7d2ff1bc402fb31cc2ca97c9' },
  { path: 'onnx/decoder_model_merged_q4f16.onnx', bytes: 283790, sha256: '1293c530ba2015f4297a74f895302c9a9f4989ac615f7ae3e6a87155cb46256e' },
  { path: 'onnx/decoder_model_merged_q4f16.onnx_data', bytes: 93352576, sha256: '554a113dee5d0874ab27264da4bef6f0c1b30cb54eeb6de49069ebdf621d7d34' },
]);

export const MODEL_TOTAL_BYTES = MODEL_ARTIFACTS.reduce((sum, artifact) => sum + artifact.bytes, 0);

export function modelArtifactUrl(path) {
  if (!MODEL_ARTIFACTS.some(artifact => artifact.path === path)) {
    throw new Error('Archivo del modelo fuera de la lista fijada.');
  }
  return `${MODEL_ORIGIN}/${MODEL_ID}/resolve/${MODEL_REVISION}/${path}`;
}

export function artifactForUrl(input) {
  let url;
  try {
    url = new URL(typeof input === 'string' ? input : input instanceof URL ? input.href : input.url);
  } catch {
    return null;
  }
  if (url.origin !== MODEL_ORIGIN) return null;
  return MODEL_ARTIFACTS.find(artifact => modelArtifactUrl(artifact.path) === url.href) ?? null;
}

export async function verifyArtifactBytes(artifact, bytes, subtle = globalThis.crypto?.subtle) {
  if (!(bytes instanceof Uint8Array) || bytes.byteLength !== artifact.bytes || !subtle) return false;
  const digest = new Uint8Array(await subtle.digest('SHA-256', bytes));
  const hex = Array.from(digest, byte => byte.toString(16).padStart(2, '0')).join('');
  return hex === artifact.sha256;
}

export async function readVerifiedResponse(response, artifact) {
  if (!response || response.status !== 200) {
    throw new Error(`No se pudo descargar ${artifact.path} (HTTP ${response?.status ?? 'sin respuesta'}).`);
  }
  const bytes = new Uint8Array(await response.arrayBuffer());
  if (!(await verifyArtifactBytes(artifact, bytes))) {
    throw new Error(`El archivo ${artifact.path} no coincide con tamaño o SHA-256 de la revisión fijada.`);
  }
  return bytes;
}
