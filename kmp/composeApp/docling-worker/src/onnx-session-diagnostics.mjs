const MAX_REPORTED_RUN_STARTS = 256;

export function instrumentInferenceSessions(InferenceSession, getRequestId, report) {
  const originalCreate = InferenceSession?.create;
  if (typeof originalCreate !== 'function') throw new TypeError('ONNX InferenceSession.create no está disponible.');

  let sessionIndex = 0;
  const emit = event => {
    const requestId = getRequestId();
    if (requestId) report(requestId, event);
  };
  const wrappedCreate = async function (...args) {
    const index = ++sessionIndex;
    const providers = (args[1]?.executionProviders ?? []).map(provider =>
      typeof provider === 'string' ? provider : provider?.name ?? 'unknown',
    );
    const startedAt = performance.now();
    emit({ stage: 'onnx-session-create-start', sessionIndex: index, providers });
    try {
      const session = await Reflect.apply(originalCreate, this, args);
      const inputs = session.inputNames ?? [];
      const component = inputs.includes('pixel_values') ? 'vision'
        : inputs.includes('input_ids') ? 'embeddings'
          : inputs.includes('inputs_embeds') ? 'decoder' : 'other';
      emit({ stage: 'onnx-session-created', sessionIndex: index, component, durationMs: Math.round(performance.now() - startedAt) });

      const originalRun = session.run.bind(session);
      let runIndex = 0;
      let runStartRequestId = null;
      let reportedRunStarts = 0;
      session.run = async function (...runArgs) {
        const currentRun = ++runIndex;
        const reportRun = currentRun <= 2 || currentRun % 32 === 0;
        const requestId = getRequestId();
        if (requestId !== runStartRequestId) {
          runStartRequestId = requestId;
          reportedRunStarts = 0;
        }
        // shortcut: cap starts at 256 per session/request; raise only if a diagnostic reaches that count.
        if (reportedRunStarts < MAX_REPORTED_RUN_STARTS) {
          reportedRunStarts += 1;
          if (requestId) report(requestId, { stage: 'onnx-run-start', sessionIndex: index, component, runIndex: currentRun });
        }
        const runStartedAt = performance.now();
        try {
          const output = await originalRun(...runArgs);
          if (reportRun) {
            emit({ stage: 'onnx-run-complete', sessionIndex: index, component, runIndex: currentRun, durationMs: Math.round(performance.now() - runStartedAt) });
          }
          return output;
        } catch (error) {
          emit({ stage: 'onnx-run-error', sessionIndex: index, component, runIndex: currentRun, errorName: error?.name ?? 'Error' });
          throw error;
        }
      };
      return session;
    } catch (error) {
      emit({ stage: 'onnx-session-create-error', sessionIndex: index, providers, errorName: error?.name ?? 'Error' });
      throw error;
    }
  };

  InferenceSession.create = wrappedCreate;
  return () => {
    if (InferenceSession.create === wrappedCreate) InferenceSession.create = originalCreate;
  };
}
