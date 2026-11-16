/**
 * Cloudflare Pages Function: /api/gemini
 * Protects GEMINI_API_KEY on Cloudflare's Edge Runtime using standard Web fetch.
 */

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization'
};

export async function onRequestOptions() {
  return new Response(null, { status: 204, headers: CORS_HEADERS });
}

export async function onRequestPost(context) {
  try {
    const apiKey = context.env && context.env.GEMINI_API_KEY;
    if (!apiKey) {
      return new Response(
        JSON.stringify({
          error: 'GEMINI_API_KEY is not configured in Cloudflare Pages environment variables.'
        }),
        {
          status: 500,
          headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
        }
      );
    }

    const body = await context.request.json();
    const model = body.model || 'gemini-2.5-flash';

    const contents =
      body.contents ||
      (body.prompt
        ? [{ role: 'user', parts: [{ text: String(body.prompt) }] }]
        : []);

    const payload = {
      contents
    };

    if (body.systemInstruction) {
      payload.systemInstruction =
        typeof body.systemInstruction === 'string'
          ? { parts: [{ text: body.systemInstruction }] }
          : body.systemInstruction;
    }

    if (body.generationConfig) {
      payload.generationConfig = body.generationConfig;
    }

    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
      model
    )}:generateContent?key=${encodeURIComponent(apiKey)}`;

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

    const text =
      data?.candidates?.[0]?.content?.parts
        ?.map((p) => p.text || '')
        .join('') || '';

    return new Response(
      JSON.stringify({
        text,
        candidates: data.candidates || [],
        raw: data
      }),
      {
        status: 200,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      }
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err?.message || 'Unexpected edge function error' }),
      {
        status: 500,
        headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' }
      }
    );
  }
}
