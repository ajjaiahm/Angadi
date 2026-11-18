import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import {
  HttpError, LIMITS, buildTranscript, clientKey, corsOrigin, createRateLimiter, extractReply, parseAssistantRequest,
} from './lib.mjs';

try {
  const envText = await readFile(new URL('.env', import.meta.url), 'utf8');
  for (const line of envText.split(/\r?\n/)) {
    const entry = line.match(/^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (entry && process.env[entry[1]] === undefined) {
      process.env[entry[1]] = entry[2].replace(/^(["'])(.*)\1$/, '$2');
    }
  }
} catch {
  // Environment variables can also be supplied by the hosting platform.
}

const port = Number(process.env.PORT || 3000);
const host = process.env.HOST || '0.0.0.0';
const apiKey = process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'put-your-private-key-here' ? process.env.GEMINI_API_KEY : '';
const model = process.env.GEMINI_MODEL || 'gemini-3.8-flash';
const allowedOrigin = corsOrigin(process.env.ALLOWED_ORIGIN);
const trustProxy = process.env.TRUST_PROXY === 'true';
const rateLimiter = createRateLimiter({ limit: Number(process.env.RATE_LIMIT_PER_MINUTE || 20) });

function send(response, status, body) {
  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff',
  };
  if (allowedOrigin) {
    headers['Access-Control-Allow-Origin'] = allowedOrigin;
    headers['Access-Control-Allow-Methods'] = 'GET, POST, OPTIONS';
    headers['Access-Control-Allow-Headers'] = 'Content-Type';
    headers.Vary = 'Origin';
  }
  response.writeHead(status, headers);
  response.end(status === 204 ? undefined : JSON.stringify(body));
}

async function readJson(request) {
  const type = String(request.headers['content-type'] || '');
  if (!type.toLowerCase().startsWith('application/json')) throw new HttpError(415, 'Content-Type must be application/json.');
  const declared = Number(request.headers['content-length'] || 0);
  if (declared > LIMITS.bodyBytes) throw new HttpError(413, 'Request is too large.');
  let size = 0;
  const chunks = [];
  for await (const chunk of request) {
    size += chunk.length;
    if (size > LIMITS.bodyBytes) {
      request.destroy();
      throw new HttpError(413, 'Request is too large.');
    }
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  } catch {
    throw new HttpError(400, 'Request body is not valid JSON.');
  }
}

async function askGemini(messages, context) {
  let upstream;
  try {
    upstream = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey },
      body: JSON.stringify({
        model,
        input: buildTranscript(messages),
        system_instruction: `You are Angadi AI, a friendly general-purpose chatbot. Answer the user's questions directly, including general knowledge, how-to help, writing, planning, ideas, and shopping. Keep explanations clear and useful, and ask a short follow-up when important details are missing. When answering about the Angadi store, use only the app context below; its prices, stock, and order records are sample data. Never claim you placed an order, charged money, checked live inventory, or arranged delivery. If a request depends on current facts you cannot verify, say so clearly.\n\nApp context:\n${context || 'No store context was provided.'}`,
        store: false,
      }),
      signal: AbortSignal.timeout(30_000),
    });
  } catch (error) {
    const timedOut = error?.name === 'TimeoutError' || error?.name === 'AbortError';
    throw new HttpError(timedOut ? 504 : 502, timedOut ? 'The AI service took too long to answer.' : 'Could not reach the AI service.');
  }
  const data = await upstream.json().catch(() => ({}));
  if (!upstream.ok) {
    // Upstream error text is logged for the operator but never forwarded to clients.
    console.error(`Gemini returned HTTP ${upstream.status}`);
    throw new HttpError(502, upstream.status === 429 ? 'The AI service is busy. Please try again shortly.' : 'The AI service returned an error.');
  }
  const reply = extractReply(data);
  if (!reply) throw new HttpError(502, 'The AI service did not return a text reply. Please try again.');
  return reply;
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url || '/', `http://${request.headers.host || 'localhost'}`);
  let status = 500;
  const reply = (code, body) => { status = code; send(response, code, body); };
  try {
    if (request.method === 'OPTIONS') return reply(204, {});
    if (request.method === 'GET' && url.pathname === '/health') {
      return reply(200, { ok: true, service: 'angadi-gemini-proxy', assistantConfigured: Boolean(apiKey) });
    }
    if (request.method !== 'POST' || url.pathname !== '/api/assistant') return reply(404, { error: 'Route not found.' });
    if (!rateLimiter.allow(clientKey(request, trustProxy))) return reply(429, { error: 'Too many requests. Please try again in a moment.' });
    if (!apiKey) return reply(503, { error: 'Set GEMINI_API_KEY in the backend environment to enable Angadi AI.' });

    const { messages, context } = parseAssistantRequest(await readJson(request));
    return reply(200, { reply: await askGemini(messages, context) });
  } catch (error) {
    if (error instanceof HttpError) return reply(error.status, { error: error.message });
    console.error('Unexpected error:', error?.name || 'Error'); // never log request bodies or keys
    return reply(500, { error: 'The assistant request failed.' });
  } finally {
    if (url.pathname !== '/health') console.log(`${request.method} ${url.pathname} -> ${status}`);
  }
});

server.requestTimeout = 20_000;
server.headersTimeout = 10_000;
server.keepAliveTimeout = 5_000;

server.listen(port, host, () => {
  console.log(`Angadi Gemini proxy listening on ${host}:${port}`);
  if (!apiKey) console.log('GEMINI_API_KEY is missing; /health works, AI requests will return setup instructions.');
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => server.close(() => process.exit(0)));
}
