import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';

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
const apiKey = process.env.GEMINI_API_KEY;
const maxBodyBytes = 16 * 1024;
const windows = new Map();

function send(response, status, body) {
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': process.env.ALLOWED_ORIGIN || '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Cache-Control': 'no-store',
  });
  response.end(JSON.stringify(body));
}

function allowRequest(ip) {
  const now = Date.now();
  const window = windows.get(ip);
  if (!window || now - window.startedAt > 60_000) {
    windows.set(ip, { startedAt: now, count: 1 });
    return true;
  }
  window.count += 1;
  return window.count <= 20;
}

async function readJson(request) {
  let size = 0;
  const chunks = [];
  for await (const chunk of request) {
    size += chunk.length;
    if (size > maxBodyBytes) throw new Error('Request is too large.');
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function getReply(payload) {
  const steps = Array.isArray(payload.steps) ? payload.steps : [];
  const outputs = steps.filter((step) => step.type === 'model_output').flatMap((step) => step.content || []);
  const reply = outputs.filter((part) => part.type === 'text').map((part) => part.text || '').join('\n').trim();
  if (reply) return reply;
  return (payload.outputs || []).filter((part) => part.type === 'text').map((part) => part.text || '').join('\n').trim();
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url || '/', `http://${request.headers.host || 'localhost'}`);
  if (request.method === 'OPTIONS') return send(response, 204, {});
  if (request.method === 'GET' && url.pathname === '/health') return send(response, 200, { ok: true, service: 'angadi-gemini-proxy' });
  if (request.method !== 'POST' || url.pathname !== '/api/assistant') return send(response, 404, { error: 'Route not found.' });

  const ip = request.socket.remoteAddress || 'unknown';
  if (!allowRequest(ip)) return send(response, 429, { error: 'Too many requests. Please try again in a moment.' });
  if (!apiKey) return send(response, 503, { error: 'Set GEMINI_API_KEY in the backend environment to enable Angadi AI.' });

  try {
    const body = await readJson(request);
    const messages = Array.isArray(body.messages) ? body.messages.slice(-12) : [];
    const context = typeof body.context === 'string' ? body.context.slice(0, 6_000) : '';
    const transcript = messages
      .filter((message) => typeof message?.text === 'string')
      .map((message) => `${message.role === 'assistant' ? 'Assistant' : 'Shopper'}: ${message.text.slice(0, 2_000)}`)
      .join('\n');
    if (!transcript.trim()) return send(response, 400, { error: 'Send a message to start a conversation.' });

    const gemini = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey },
      body: JSON.stringify({
        model: process.env.GEMINI_MODEL || 'gemini-3.8-flash',
        input: transcript,
        system_instruction: `You are Angadi AI, a friendly general-purpose chatbot. Answer the user's questions directly, including general knowledge, how-to help, writing, planning, ideas, and shopping. Keep explanations clear and useful, and ask a short follow-up when important details are missing. When answering about the Angadi store, use only the app context below; its prices, stock, and order records are sample data. Never claim you placed an order, charged money, checked live inventory, or arranged delivery. If a request depends on current facts you cannot verify, say so clearly.\n\nApp context:\n${context || 'No store context was provided.'}`,
        store: false,
      }),
      signal: AbortSignal.timeout(30_000),
    });
    const data = await gemini.json().catch(() => ({}));
    if (!gemini.ok) {
      const message = data.error?.message || `Gemini returned ${gemini.status}.`;
      return send(response, 502, { error: message });
    }
    const reply = getReply(data);
    if (!reply) return send(response, 502, { error: 'Gemini did not return a text reply. Please try again.' });
    return send(response, 200, { reply });
  } catch (error) {
    const status = error.message === 'Request is too large.' ? 413 : 400;
    return send(response, status, { error: error.message || 'The assistant request failed.' });
  }
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Angadi Gemini proxy listening on port ${port}`);
  if (!apiKey) console.log('GEMINI_API_KEY is missing; /health works, AI requests will return setup instructions.');
});
