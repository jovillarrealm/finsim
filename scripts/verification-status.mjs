const statuses = new Set(['PASS', 'FAIL', 'BLOCKED', 'NOT_RUN', 'NOT_APPLICABLE']);

export function summarizeVerification(steps) {
  if (!Array.isArray(steps)) throw new TypeError('Los pasos de verificación deben ser una lista.');
  for (const step of steps) {
    if (!statuses.has(step.status)) throw new Error(`Estado de verificación desconocido: ${step.status}`);
    if (step.status === 'NOT_APPLICABLE' && !step.reason?.trim()) {
      throw new Error('NOT_APPLICABLE requiere un motivo.');
    }
  }

  const status = steps.some(step => step.status === 'FAIL')
    ? 'FAIL'
    : steps.length === 0 || steps.some(step => ['BLOCKED', 'NOT_RUN'].includes(step.status))
      ? 'INCOMPLETE'
      : 'PASS';

  return { complete: status === 'PASS', exitCode: status === 'PASS' ? 0 : 1, status };
}
