import test from 'node:test';
import assert from 'node:assert/strict';
import {
  HttpError, LIMITS, buildTranscript, clientKey, corsOrigin, createRateLimiter, extractReply, parseAssistantRequest,
} from '../lib.mjs';

test('rate limiter allows up to the limit then blocks, per key', () => {
  let t = 0;
  const limiter = createRateLimiter({ limit: 3, windowMs: 1000, now: () => t });
  assert.deepEqual([1, 2, 3, 4].map(() => limiter.allow('a')), [true, true, true, false]);
  assert.equal(limiter.allow('b'), true);
  t = 1500; // window elapsed
  assert.equal(limiter.allow('a'), true);
});

test('rate limiter evicts expired windows and fails closed when full of live keys', () => {
  let t = 0;
  const limiter = createRateLimiter({ limit: 1, windowMs: 1000, maxKeys: 2, now: () => t });
  limiter.allow('a'); limiter.allow('b');
  assert.equal(limiter.allow('c'), false); // both windows still live
  t = 2000;
  assert.equal(limiter.allow('c'), true); // expired entries evicted
  assert.ok(limiter.size <= 2);
});

test('parseAssistantRequest normalises and truncates messages', () => {
  const long = 'x'.repeat(LIMITS.messageChars + 50);
  const { messages, context } = parseAssistantRequest({ messages: [{ role: 'assistant', text: 'hi' }, { role: 'weird', text: long }], context: 'c'.repeat(9000) });
  assert.equal(messages.length, 2);
  assert.equal(messages[1].role, 'user');
  assert.equal(messages[1].text.length, LIMITS.messageChars);
  assert.equal(context.length, LIMITS.contextChars);
});

test('parseAssistantRequest keeps only the most recent messages', () => {
  const many = Array.from({ length: 30 }, (_, i) => ({ role: 'user', text: `m${i}` }));
  const { messages } = parseAssistantRequest({ messages: many });
  assert.equal(messages.length, LIMITS.messages);
  assert.equal(messages.at(-1).text, 'm29');
});

test('parseAssistantRequest rejects bad shapes with HttpError 400', () => {
  for (const bad of [null, [], 'x', {}, { messages: 'no' }, { messages: [] }, { messages: [{ role: 'user', text: '   ' }] }, { messages: [{ role: 'assistant', text: 'last is assistant' }] }]) {
    assert.throws(() => parseAssistantRequest(bad), (e) => e instanceof HttpError && e.status === 400);
  }
});

test('buildTranscript labels roles', () => {
  assert.equal(buildTranscript([{ role: 'user', text: 'a' }, { role: 'assistant', text: 'b' }]), 'Shopper: a\nAssistant: b');
});

test('extractReply handles steps, legacy outputs and empty payloads', () => {
  assert.equal(extractReply({ steps: [{ type: 'model_output', content: [{ type: 'text', text: 'Hello' }, { type: 'text', text: 'there' }] }] }), 'Hello\nthere');
  assert.equal(extractReply({ outputs: [{ type: 'text', text: 'Legacy' }] }), 'Legacy');
  assert.equal(extractReply({}), '');
  assert.equal(extractReply(null), '');
});

test('clientKey only trusts X-Forwarded-For when configured', () => {
  const req = { headers: { 'x-forwarded-for': '9.9.9.9, 1.1.1.1' }, socket: { remoteAddress: '10.0.0.1' } };
  assert.equal(clientKey(req, false), '10.0.0.1');
  assert.equal(clientKey(req, true), '9.9.9.9');
  assert.equal(clientKey({ headers: {}, socket: {} }, true), 'unknown');
});

test('corsOrigin is disabled unless explicitly set', () => {
  assert.equal(corsOrigin(undefined), null);
  assert.equal(corsOrigin('  '), null);
  assert.equal(corsOrigin('https://shop.example.com'), 'https://shop.example.com');
});
