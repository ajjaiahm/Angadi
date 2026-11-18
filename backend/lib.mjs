// Pure helpers for the Angadi Gemini proxy. Kept separate from server.mjs so they can be unit tested.

export const LIMITS = Object.freeze({
  bodyBytes: 16 * 1024,
  messages: 12,
  messageChars: 2_000,
  contextChars: 6_000,
});

export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/** Fixed-window per-key limiter that evicts expired windows so memory stays bounded. */
export function createRateLimiter({ limit = 20, windowMs = 60_000, maxKeys = 10_000, now = Date.now } = {}) {
  const windows = new Map();
  return {
    allow(key) {
      const t = now();
      if (windows.size >= maxKeys) {
        for (const [k, w] of windows) if (t - w.startedAt > windowMs) windows.delete(k);
        if (windows.size >= maxKeys) return false; // under attack from many addresses: fail closed
      }
      const current = windows.get(key);
      if (!current || t - current.startedAt > windowMs) {
        windows.set(key, { startedAt: t, count: 1 });
        return true;
      }
      current.count += 1;
      return current.count <= limit;
    },
    get size() {
      return windows.size;
    },
  };
}

/** Validates and normalises an /api/assistant request body. Throws HttpError(400) on bad input. */
export function parseAssistantRequest(body) {
  if (body === null || typeof body !== 'object' || Array.isArray(body)) throw new HttpError(400, 'Send a JSON object.');
  if (!Array.isArray(body.messages)) throw new HttpError(400, 'messages must be an array.');
  const messages = body.messages
    .slice(-LIMITS.messages)
    .filter((m) => m && typeof m === 'object' && typeof m.text === 'string' && m.text.trim())
    .map((m) => ({ role: m.role === 'assistant' ? 'assistant' : 'user', text: m.text.slice(0, LIMITS.messageChars) }));
  if (messages.length === 0) throw new HttpError(400, 'Send a message to start a conversation.');
  if (messages[messages.length - 1].role !== 'user') throw new HttpError(400, 'The last message must come from the shopper.');
  const context = typeof body.context === 'string' ? body.context.slice(0, LIMITS.contextChars) : '';
  return { messages, context };
}

export function buildTranscript(messages) {
  return messages.map((m) => `${m.role === 'assistant' ? 'Assistant' : 'Shopper'}: ${m.text}`).join('\n');
}

/** Extracts text from either the `steps` or legacy `outputs` Interactions response shapes. */
export function extractReply(payload) {
  const steps = Array.isArray(payload?.steps) ? payload.steps : [];
  const fromSteps = steps
    .filter((s) => s?.type === 'model_output')
    .flatMap((s) => s.content || [])
    .filter((p) => p?.type === 'text')
    .map((p) => p.text || '')
    .join('\n')
    .trim();
  if (fromSteps) return fromSteps;
  return (payload?.outputs || [])
    .filter((p) => p?.type === 'text')
    .map((p) => p.text || '')
    .join('\n')
    .trim();
}

/** Client address, honouring X-Forwarded-For only when the operator says a trusted proxy sets it. */
export function clientKey(request, trustProxy) {
  if (trustProxy) {
    const forwarded = String(request.headers['x-forwarded-for'] || '').split(',')[0].trim();
    if (forwarded) return forwarded;
  }
  return request.socket?.remoteAddress || 'unknown';
}

/** Parses ALLOWED_ORIGIN: unset/empty means no CORS headers at all (the Android app doesn't need CORS). */
export function corsOrigin(value) {
  const origin = (value || '').trim();
  return origin || null;
}
