/**
 * Cloudflare Worker Entry Point (wrangler deploy)
 * Serves static assets from ./public via env.ASSETS and handles /api/supabase & /api/gemini edge routes.
 */

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization'
};

const U0 = [142, 189, 7, 111, 21, 58, 232, 139, 161, 70, 101, 29, 63, 238];
const U1 = [43, 201, 206, 190, 74, 112, 5, 42, 241, 204, 185, 85, 61];
const U2 = [96, 73, 56, 236, 149, 160, 65, 121, 4, 40, 231, 158, 171];

const K0 = [33, 201, 136, 171, 66, 89, 93, 40, 211, 176, 163, 127, 113, 73, 97, 192];
const K1 = [105, 3, 52, 217, 130, 142, 98, 95, 28, 103, 247, 138, 144, 105, 66];
const K2 = [159, 77, 121, 19, 3, 255, 136, 136, 105, 67, 65, 20, 182, 173, 170];

function reconstructVault(s0, s1, s2, len, seedByte) {
  const bytes = new Uint8Array(len);
  let i0 = 0, i1 = 0, i2 = 0;
  for (let i = 0; i < len; i++) {
    const masked = i % 3 === 0 ? s0[i0++] : i % 3 === 1 ? s1[i1++] : s2[i2++];
    const salt = ((i * 73 + seedByte * 31 + 19) ^ 0x5a) & 0xff;
    bytes[i] = (masked ^ salt) & 0xff;
  }
  return new TextDecoder().decode(bytes);
}

async function handleSupabaseProxy(request, env) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }
  try {
    const supabaseUrl = ((env && env.SUPABASE_URL) || reconstructVault(U0, U1, U2, 40, 0x37)).replace(/\/+$/, '');
    const supabaseKey = ((env && env.SUPABASE_ANON_KEY) || reconstructVault(K0, K1, K2, 46, 0x6b)).trim();

    const reqBody = await request.json();
    const path = String(reqBody.path || '');
    const method = String(reqBody.method || 'GET').toUpperCase();
    const accessToken = reqBody.accessToken || '';
    const prefer = reqBody.prefer || '';
    const payload = reqBody.body;

    if (!path.startsWith('/auth/v1/') && !path.startsWith('/rest/v1/') && !path.startsWith('/functions/v1/')) {
      return new Response(JSON.stringify({ error: 'Disallowed Supabase endpoint path' }), {
        status: 400,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      });
    }

    const headers = {
      apikey: supabaseKey,
      Authorization: `Bearer ${accessToken || supabaseKey}`,
      'Content-Type': 'application/json'
    };
    if (prefer) headers.Prefer = prefer;

    const fetchInit = { method, headers };
    if (method !== 'GET' && method !== 'HEAD' && payload !== undefined) {
      fetchInit.body = typeof payload === 'string' ? payload : JSON.stringify(payload);
    }

    const upstream = await fetch(`${supabaseUrl}${path}`, fetchInit);
    const text = await upstream.text();
    return new Response(text || '{}', {
      status: upstream.status,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err?.message || 'Supabase edge proxy error' }), {
      status: 500,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
    });
  }
}

async function handleGeminiProxy(request, env) {
  if (request.method === 'OPTIONS') {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }
  try {
    const apiKey = env && env.GEMINI_API_KEY;
    if (!apiKey) {
      return new Response(JSON.stringify({ error: 'GEMINI_API_KEY is not configured.' }), {
        status: 500,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      });
    }
    const body = await request.json();
    const model = body.model || 'gemini-2.5-flash';
    const contents = body.contents || (body.prompt ? [{ role: 'user', parts: [{ text: String(body.prompt) }] }] : []);
    const payload = { contents };
    if (body.systemInstruction) {
      payload.systemInstruction = typeof body.systemInstruction === 'string'
        ? { parts: [{ text: body.systemInstruction }] }
        : body.systemInstruction;
    }
    if (body.generationConfig) payload.generationConfig = body.generationConfig;

    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;
    const upstream = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await upstream.json();
    if (!upstream.ok) {
      return new Response(JSON.stringify(data), {
        status: upstream.status,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      });
    }
    const text = data?.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('') || '';
    return new Response(JSON.stringify({ text, candidates: data.candidates || [], raw: data }), {
      status: 200,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err?.message || 'Unexpected edge function error' }), {
      status: 500,
      headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
    });
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === '/api/supabase') {
      return handleSupabaseProxy(request, env);
    }
    if (url.pathname === '/api/gemini') {
      return handleGeminiProxy(request, env);
    }
    if (env && env.ASSETS) {
      const assetResp = await env.ASSETS.fetch(request);
      if (url.pathname.startsWith('/downloads/')) {
        const ct = (assetResp.headers.get('content-type') || '').toLowerCase();
        if (ct.includes('text/html')) {
          return new Response('Application package not found', { status: 404 });
        }
        const fileName = url.pathname.split('/').pop() || 'AnimeUniverse-Package';
        const headers = new Headers(assetResp.headers);
        if (fileName.endsWith('.apk')) {
          headers.set('Content-Type', 'application/vnd.android.package-archive');
        } else if (fileName.endsWith('.exe')) {
          headers.set('Content-Type', 'application/vnd.microsoft.portable-executable');
        } else if (fileName.endsWith('.dmg')) {
          headers.set('Content-Type', 'application/x-apple-diskimage');
        }
        headers.set('Content-Disposition', `attachment; filename="${fileName}"`);
        return new Response(assetResp.body, {
          status: assetResp.status,
          headers
        });
      }
      if (url.pathname === '/' || url.pathname === '/index.html' || url.pathname === '/sw.js') {
        const noCacheHeaders = new Headers(assetResp.headers);
        noCacheHeaders.set('Cache-Control', 'no-cache, no-store, must-revalidate');
        return new Response(assetResp.body, {
          status: assetResp.status,
          headers: noCacheHeaders
        });
      }
      return assetResp;
    }
    return new Response('Not Found', { status: 404 });
  }
};
