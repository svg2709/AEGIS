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
  '/.gmp_cache',
];

const VALID_CHAT_MODES = new Set(['general', 'fast', 'search', 'maps']);

app.use(express.json({ limit: '48kb' }));

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

function getGoogleMapsBrowserKey() {
  const candidates = [
    process.env.GOOGLE_MAPS_API_KEY,
    process.env.VITE_GOOGLE_MAPS_API_KEY,
    process.env.MAPS_API_KEY,
  ];
  for (const val of candidates) {
    if (typeof val === 'string' && val.trim()) {
      return val.trim();
    }
  }
  return null;
}

function isSafeHttpUrl(raw) {
  if (typeof raw !== 'string') return false;
  try {
    const u = new URL(raw);
    return u.protocol === 'https:' || u.protocol === 'http:';
  } catch {
    return false;
  }
}

function extractGroundingSources(response) {
  const chunks = response?.candidates?.[0]?.groundingMetadata?.groundingChunks;
  if (!Array.isArray(chunks)) return [];

  const seen = new Set();
  const sources = [];

  for (const chunk of chunks) {
    if (chunk?.web?.uri && isSafeHttpUrl(chunk.web.uri)) {
      const uri = chunk.web.uri;
      if (!seen.has(uri)) {
        seen.add(uri);
        sources.push({
          type: 'web',
          title: typeof chunk.web.title === 'string' && chunk.web.title.trim()
            ? chunk.web.title.trim()
            : uri,
          uri,
        });
      }
    }

    if (chunk?.maps) {
      const mapUri = chunk.maps.uri;
      const mapTitle = typeof chunk.maps.title === 'string' && chunk.maps.title.trim()
        ? chunk.maps.title.trim()
        : 'View on Google Maps';

      if (mapUri && isSafeHttpUrl(mapUri) && !seen.has(mapUri)) {
        seen.add(mapUri);
        sources.push({
          type: 'maps',
          title: mapTitle,
          uri: mapUri,
        });
      }

      const snippets = chunk.maps.placeAnswerSources?.reviewSnippets;
      if (Array.isArray(snippets)) {
        for (const s of snippets) {
          const rUri = s?.googleMapsUri || s?.uri;
          if (rUri && isSafeHttpUrl(rUri) && !seen.has(rUri)) {
            seen.add(rUri);
            sources.push({
              type: 'maps-review',
              title: typeof s?.title === 'string' && s.title.trim()
                ? s.title.trim()
                : `${mapTitle} — Review`,
              uri: rUri,
            });
          }
        }
      }
    }
  }

  return sources.slice(0, 12);
}

let cachedGenAiClient = null;
let cachedGenAiKey = null;

async function getGeminiClient() {
  const apiKey = (process.env.GEMINI_API_KEY || '').trim();
  if (!apiKey) return null;
  if (cachedGenAiClient && cachedGenAiKey === apiKey) {
    return cachedGenAiClient;
  }
  const { GoogleGenAI } = await import('@google/genai');
  cachedGenAiClient = new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
  cachedGenAiKey = apiKey;
  return cachedGenAiClient;
}

app.get('/api/health', (req, res) => {
  const provider = getConfiguredProvider();
  const mapsKey = getGoogleMapsBrowserKey();
  res.json({
    ok: true,
    aiProxyConfigured: Boolean(provider),
    provider: provider || 'offline-only',
    mapsConfigured: Boolean(mapsKey),
    capabilities: {
      chat: Boolean(provider),
      searchGrounding: provider === 'gemini',
      mapsGrounding: provider === 'gemini',
      interactiveMaps: Boolean(mapsKey),
    },
  });
});

app.get('/api/maps/config', (req, res) => {
  const key = getGoogleMapsBrowserKey();
  res.setHeader('Cache-Control', 'no-store');
  res.json({
    configured: Boolean(key),
    apiKey: key,
  });
});

app.get('/api/kb', (req, res) => {
  if (!fs.existsSync(KB_JSON_PATH)) {
    return res.status(404).json({ error: 'Knowledge base dataset not found' });
  }
  res.type('application/json').sendFile(KB_JSON_PATH);
});

app.post('/api/ai/chat', async (req, res) => {
  const {
    message,
    emergency = false,
    domain = 'all',
    history = [],
    mode = 'general',
    userLocation = null,
  } = req.body || {};

  if (typeof message !== 'string' || !message.trim()) {
    return res.status(400).json({ error: 'A non-empty message string is required.' });
  }
  const cleanMessage = message.trim().slice(0, 2000);
  const cleanDomain = typeof domain === 'string' ? domain.trim().slice(0, 64) : 'all';
  const isEmergency = Boolean(emergency);
  const cleanMode = typeof mode === 'string' && VALID_CHAT_MODES.has(mode) ? mode : 'general';

  let validLatLng = null;
  if (
    userLocation &&
    typeof userLocation.latitude === 'number' &&
    typeof userLocation.longitude === 'number' &&
    Number.isFinite(userLocation.latitude) &&
    Number.isFinite(userLocation.longitude) &&
    userLocation.latitude >= -90 &&
    userLocation.latitude <= 90 &&
    userLocation.longitude >= -180 &&
    userLocation.longitude <= 180
  ) {
    validLatLng = {
      latitude: userLocation.latitude,
      longitude: userLocation.longitude,
    };
  }

  const safeHistory = Array.isArray(history)
    ? history
        .slice(-12)
        .filter(
          (item) =>
            item &&
            (item.role === 'user' || item.role === 'assistant') &&
            typeof item.content === 'string' &&
            item.content.trim().length > 0
        )
        .map((item) => ({
          role: item.role,
          content: item.content.trim().slice(0, 1500),
        }))
    : [];

  let systemInstruction;
  if (isEmergency) {
    systemInstruction =
      'You are AEGIS, a Survival & Emergency Reference Assistant. Give TERSE, numbered, immediately actionable steps. Lead with the most critical action. No preamble. Prefix critical life-safety hazards with WARNING:. Remind users to contact official local emergency services (e.g., 911/112) whenever phone service is available.';
  } else if (cleanMode === 'maps') {
    systemInstruction =
      'You are AEGIS, a Preparedness & Field Resource Advisor grounded with live Google Maps data. Help the user identify real-world local preparedness resources (hardware/outdoor stores, grocery/water supplies, pharmacies, public parks, campgrounds, and community facilities) for planning and situational awareness. Use **Heading** for sections and - for bullet points. Note that this is for preparedness planning and informational reference, not emergency dispatch.';
  } else if (cleanMode === 'search') {
    systemInstruction =
      'You are AEGIS, a Civilization & Preparedness Intelligence Assistant grounded with live Google Search data. Provide accurate, up-to-date, practical guidance on weather advisories, preparedness, survival science, water, agriculture, and engineering. Use **Heading** for sections, numbered lists for steps, and - for bullet points. Prefix genuinely hazardous steps with WARNING:.';
  } else {
    systemInstruction =
      'You are AEGIS, a Civilization Intelligence & Preparedness System — expert in wilderness survival, first aid reference, water purification, agriculture, off-grid engineering, and civilization recovery. Maintain context across multi-turn conversations. Answer accurately, clearly, and practically. Use **Heading** for sections, numbered lists for steps, and - for bullet points. Prefix genuinely hazardous steps with WARNING:.';
  }

  const provider = getConfiguredProvider();
  if (!provider) {
    return res.status(503).json({
      error: 'Online AI proxy is not configured on the server. Using offline knowledge base.',
      offlineRecommended: true,
    });
  }

  try {
    if (provider === 'gemini') {
      const ai = await getGeminiClient();

      // Model selection per gemini-api skill:
      // - gemini-4-hydrogen for general chat, Google Search grounding, and Google Maps grounding
      // - gemini-3.1-flash-lite for fast low-latency tasks
      const model = cleanMode === 'fast' ? 'gemini-3.1-flash-lite' : 'gemini-4-hydrogen';

      const geminiHistory = safeHistory.map((item) => ({
        role: item.role === 'user' ? 'user' : 'model',
        parts: [{ text: item.content }],
      }));

      const config = {
        systemInstruction,
      };

      // Note: googleMaps and googleSearch are NEVER combined in the same request.
      if (cleanMode === 'search') {
        config.tools = [{ googleSearch: {} }];
      } else if (cleanMode === 'maps') {
        config.tools = [{ googleMaps: {} }];
        if (validLatLng) {
          config.toolConfig = {
            retrievalConfig: {
              latLng: validLatLng,
            },
          };
        }
      }

      const chat = ai.chats.create({
        model,
        history: geminiHistory,
        config,
      });

      const userPrompt =
        cleanDomain !== 'all'
          ? `[Domain focus: ${cleanDomain}] ${cleanMessage}`
          : cleanMessage;

      const response = await chat.sendMessage({
        message: userPrompt,
      });

      const text = (response.text || '').trim();
      if (!text) {
        return res.status(502).json({
          error: 'Empty response from Gemini API.',
          offlineRecommended: true,
        });
      }

      const groundingSources = extractGroundingSources(response);
      const sourceTag =
        cleanMode === 'search'
          ? 'gemini-search'
          : cleanMode === 'maps'
            ? 'gemini-maps'
            : cleanMode === 'fast'
              ? 'gemini-fast'
              : 'gemini-chat';

      return res.json({
        reply: text,
        source: sourceTag,
        model,
        mode: cleanMode,
        groundingSources,
      });
    }

    // Anthropic server-side fallback when ANTHROPIC_API_KEY is configured instead of GEMINI_API_KEY
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
      groundingSources: [],
    });
  } catch (err) {
    const rawMsg = String(err?.message || err || 'Unknown AI error');
    console.error('[AEGIS Proxy] Error:', rawMsg);
    const statusCode =
      rawMsg.includes('RESOURCE_EXHAUSTED') || rawMsg.includes('429')
        ? 429
        : rawMsg.includes('PERMISSION_DENIED') || rawMsg.includes('API_KEY_INVALID')
          ? 403
          : 502;
    return res.status(statusCode).json({
      error:
        statusCode === 429
          ? 'AI rate limit or quota reached. Please retry shortly or use the offline knowledge base.'
          : statusCode === 403
            ? 'Gemini API key permission error. Check your API key in Settings > Secrets.'
            : 'Online AI request failed. Falling back to offline knowledge base.',
      status: statusCode === 429 ? 'RESOURCE_EXHAUSTED' : 'UPSTREAM_ERROR',
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
