const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const HOST = '0.0.0.0';

const KB_JSON_PATH = path.join(__dirname, 'android', 'app', 'src', 'main', 'assets', 'aegis_kb.json');

const BLOCKED_EXACT = new Set([
  '/server.js',
  '/package.json',
  '/package-lock.json',
  '/bun.lock',
  '/metadata.json',
  '/readme.md',
  '/changelog.md',
]);

const BLOCKED_PREFIXES = [
  '/node_modules',
  '/android',
  '/.github',
  '/.git',
];

app.use(express.json({ limit: '32kb' }));

app.use((req, res, next) => {
  const normalized = req.path.toLowerCase();
  if (
    BLOCKED_EXACT.has(normalized) ||
    BLOCKED_PREFIXES.some((prefix) => normalized === prefix || normalized.startsWith(prefix + '/')) ||
    normalized.includes('/.')
  ) {
    return res.status(404).json({ error: 'Not found' });
  }
  next();
});

function getConfiguredProvider() {
  if (process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY.trim()) {
    return 'gemini';
  }
  if (process.env.ANTHROPIC_API_KEY && process.env.ANTHROPIC_API_KEY.trim()) {
    return 'anthropic';
  }
  return null;
}

app.get('/api/health', (req, res) => {
  const provider = getConfiguredProvider();
  res.json({
    ok: true,
    aiProxyConfigured: Boolean(provider),
    provider: provider || 'offline-only',
  });
});

app.get('/api/kb', (req, res) => {
  if (!fs.existsSync(KB_JSON_PATH)) {
    return res.status(404).json({ error: 'Knowledge base dataset not found' });
  }
  res.type('application/json').sendFile(KB_JSON_PATH);
});

app.post('/api/ai/chat', async (req, res) => {
  const { message, emergency = false, domain = 'all', history = [] } = req.body || {};

  if (typeof message !== 'string' || !message.trim()) {
    return res.status(400).json({ error: 'A non-empty message string is required.' });
  }
  const cleanMessage = message.trim().slice(0, 2000);
  const cleanDomain = typeof domain === 'string' ? domain.trim().slice(0, 64) : 'all';
  const isEmergency = Boolean(emergency);

  const safeHistory = Array.isArray(history)
    ? history
        .slice(-8)
        .filter(
          (item) =>
            item &&
            (item.role === 'user' || item.role === 'assistant') &&
            typeof item.content === 'string' &&
            item.content.trim().length > 0
        )
        .map((item) => ({
          role: item.role,
          content: item.content.trim().slice(0, 1200),
        }))
    : [];

  const systemInstruction = isEmergency
    ? 'You are AEGIS, an emergency survival intelligence system. Give TERSE, numbered, immediately actionable steps. Lead with the most critical life-saving action. No preamble. Prefix critical life-safety hazards with WARNING:.'
    : 'You are AEGIS, a Civilization Intelligence System — expert in survival, emergency medicine, agriculture, water infrastructure, engineering, and civilization recovery. Answer accurately and practically. Use **Heading** for sections, numbered lists for steps, and - for bullet points. Prefix genuinely hazardous steps with WARNING:.';

  const provider = getConfiguredProvider();
  if (!provider) {
    return res.status(503).json({
      error: 'Online AI proxy is not configured on the server. Using offline knowledge base.',
      offlineRecommended: true,
    });
  }

  try {
    if (provider === 'gemini') {
      const { GoogleGenAI } = await import('@google/genai');
      const ai = new GoogleGenAI({
        apiKey: process.env.GEMINI_API_KEY,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          },
        },
      });

      const historyText = safeHistory
        .map((m) => `${m.role === 'user' ? 'Operator' : 'AEGIS'}: ${m.content}`)
        .join('\n\n');
      const prompt = [
        historyText ? `Recent context:\n${historyText}\n` : '',
        `Current query${cleanDomain !== 'all' ? ` [Domain focus: ${cleanDomain}]` : ''}: ${cleanMessage}`,
      ]
        .filter(Boolean)
        .join('\n');

      const response = await ai.models.generateContent({
        model: 'gemini-4-hydrogen',
        contents: prompt,
        config: {
          systemInstruction,
        },
      });

      const text = (response.text || '').trim();
      if (!text) {
        return res.status(502).json({
          error: 'Empty response from AI provider. Falling back to offline KB.',
          offlineRecommended: true,
        });
      }

      return res.json({
        reply: text,
        source: 'gemini-proxy',
        model: 'gemini-4-hydrogen',
      });
    }

    // Anthropic server-side fallback when ANTHROPIC_API_KEY is configured
    const anthropicRes = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': process.env.ANTHROPIC_API_KEY,
        'anthropic-version': '2023-06-01',
      },
      body: JSON.stringify({
        model: 'claude-sonnet-4-20250514',
        max_tokens: 1400,
        system: systemInstruction,
        messages: [
          ...safeHistory,
          {
            role: 'user',
            content:
              cleanMessage +
              (cleanDomain !== 'all' ? ` [Focus on domain: ${cleanDomain}]` : ''),
          },
        ],
      }),
    });

    if (!anthropicRes.ok) {
      const errData = await anthropicRes.json().catch(() => ({}));
      const msg = errData?.error?.message || `Upstream HTTP ${anthropicRes.status}`;
      return res.status(502).json({
        error: `AI proxy error: ${msg}`,
        offlineRecommended: true,
      });
    }

    const data = await anthropicRes.json();
    const text = data?.content?.[0]?.text?.trim() || '';
    if (!text) {
      return res.status(502).json({
        error: 'Empty response from AI provider.',
        offlineRecommended: true,
      });
    }

    return res.json({
      reply: text,
      source: 'anthropic-proxy',
    });
  } catch (err) {
    console.error('[AEGIS Proxy] Error:', err?.message || err);
    return res.status(502).json({
      error: 'Online AI proxy temporarily unavailable. Using offline knowledge base.',
      offlineRecommended: true,
    });
  }
});

app.get('/landing', (req, res) => {
  res.sendFile(path.join(__dirname, 'aegis-landing.html'));
});

app.use(
  express.static(__dirname, {
    dotfiles: 'deny',
    index: 'index.html',
  })
);

app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, HOST, () => {
  console.log(`AEGIS server running at http://${HOST}:${PORT}`);
});
