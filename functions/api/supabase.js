/**
 * Cloudflare Pages Function: /api/supabase
 * Proxies Supabase Auth & REST requests on Cloudflare's Edge Runtime using standard fetch.
 * Reads SUPABASE_URL and SUPABASE_ANON_KEY from context.env with built-in obfuscated vault fallback.
 */

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
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

export async function onRequestOptions() {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}

export async function onRequestPost(context) {
  try {
    const supabaseUrl = (
      (context.env && context.env.SUPABASE_URL) ||
      reconstructVault(U0, U1, U2, 40, 0x37)
    ).replace(/\/+$/, '');

    const supabaseKey = (
      (context.env && context.env.SUPABASE_ANON_KEY) ||
      reconstructVault(K0, K1, K2, 46, 0x6b)
    ).trim();

    const reqBody = await context.request.json();
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
    if (prefer) {
      headers.Prefer = prefer;
    }

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
    return new Response(
      JSON.stringify({ error: err?.message || 'Supabase edge proxy error' }),
      {
        status: 500,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      }
    );
  }
}
