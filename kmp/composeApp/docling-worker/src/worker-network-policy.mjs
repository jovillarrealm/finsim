export function resolveResourceUrl(input, baseUrl) {
  const value = typeof input === 'string' ? input : input instanceof URL ? input.href : input?.url;
  if (typeof value !== 'string') throw new TypeError('La solicitud no tiene una URL válida.');
  return new URL(value, baseUrl);
}

export function requestHasBody(input, init) {
  const requestBody = typeof Request !== 'undefined' && input instanceof Request ? input.body : null;
  const initBody = init && Object.hasOwn(init, 'body') ? init.body : null;
  return requestBody != null || initBody != null;
}

export function isAllowedResourceRequest(url, method, body, baseUrl, isPinnedArtifact) {
  const normalizedMethod = String(method ?? 'GET').toUpperCase();
  if ((normalizedMethod !== 'GET' && normalizedMethod !== 'HEAD') || body != null) return false;
  return (url.origin === baseUrl.origin && url.pathname.startsWith(baseUrl.pathname)) || isPinnedArtifact;
}
