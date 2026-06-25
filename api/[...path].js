export const config = {
  api: {
    bodyParser: false,
    responseLimit: '10mb',
  },
};

const ORACLE_URL = process.env.ORACLE_BONACA_URL;
const SECRET = process.env.BONACA_BACKEND_SECRET ?? '';

export default async function handler(req, res) {
  if (!ORACLE_URL || !SECRET) {
    res.status(503).json({ message: 'Bonaca backend is not configured' });
    return;
  }

  const target = `${ORACLE_URL.replace(/\/+$/, '')}${req.url}`;

  const headers = { 'X-Backend-Secret': SECRET };
  for (const [key, value] of Object.entries(req.headers)) {
    const lower = key.toLowerCase();
    if (['host', 'connection', 'transfer-encoding', 'keep-alive', 'x-backend-secret'].includes(lower)) {
      continue;
    }
    headers[key] = Array.isArray(value) ? value.join(', ') : value;
  }

  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }
  const body = chunks.length > 0 ? Buffer.concat(chunks) : undefined;

  let upstream;
  try {
    upstream = await fetch(target, {
      method: req.method ?? 'GET',
      headers,
      body: body?.length ? body : undefined,
      signal: AbortSignal.timeout(15000),
    });
  } catch {
    res.status(502).json({ message: 'Bonaca backend is unavailable' });
    return;
  }

  res.status(upstream.status);
  for (const [key, value] of upstream.headers.entries()) {
    if (!['transfer-encoding', 'connection', 'keep-alive'].includes(key.toLowerCase())) {
      res.setHeader(key, value);
    }
  }

  res.end(Buffer.from(await upstream.arrayBuffer()));
}
