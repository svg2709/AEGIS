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

const KB_STOP_WORDS = new Set([
  'the', 'and', 'for', 'with', 'that', 'this', 'from', 'your', 'have',
  'are', 'how', 'what', 'when', 'where', 'into', 'about', 'can', 'you',
  'will', 'not', 'use', 'using', 'make', 'get', 'need', 'should', 'must',
  'they', 'them', 'then', 'than', 'also', 'just', 'like', 'some', 'more',
]);

let cachedKbIndex = null;

function tokenizeKb(text) {
  const parts = String(text || '')
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((t) => t.length >= 2 && !KB_STOP_WORDS.has(t));
  return new Set(parts);
}

function getKbIndex() {
  if (cachedKbIndex) return cachedKbIndex;
  try {
    if (!fs.existsSync(KB_JSON_PATH)) return [];
    const raw = JSON.parse(fs.readFileSync(KB_JSON_PATH, 'utf8'));
    const items = [];
    for (const domain of Object.values(raw.domains || {})) {
      const domainId = domain.id || '';
      const domainLabel = domain.label || domainId;
      for (const topic of Object.values(domain.topics || {})) {
        const topicLabel = topic.label || '';
        for (const art of topic.articles || []) {
          const title = art.title || '';
          const body = art.body || '';
          items.push({
            id: art.id || '',
            title,
            titleLower: title.toLowerCase(),
            body,
            bodyLower: body.toLowerCase(),
            priority: art.priority || 'medium',
            time: art.time || 'ongoing',
            domainId,
            domainLabel,
            topicLabel,
            tokens: tokenizeKb(`${title} ${topicLabel} ${domainLabel} ${body}`),
          });
        }
      }
    }
    cachedKbIndex = items;
    return items;
  } catch (err) {
    console.error('[AEGIS KB Index] Load error:', err.message);
    return [];
  }
}

function retrieveKbContext(message, safeHistory, activeArticleId, cleanDomain) {
  const index = getKbIndex();
  if (!index.length) return [];

  const pinned = activeArticleId
    ? index.find((a) => a.id === activeArticleId)
    : null;

  const recentUserText = safeHistory
    .filter((h) => h.role === 'user')
    .slice(-3)
    .map((h) => h.content)
    .join(' ');
  const recentAssistantText = safeHistory
    .filter((h) => h.role === 'assistant')
    .slice(-2)
    .map((h) => h.content.slice(0, 350))
    .join(' ');

  const currentTokens = tokenizeKb(message);
  const contextTokens = tokenizeKb(`${recentUserText} ${recentAssistantText}`);
  const msgLower = message.toLowerCase();

  const scored = index
    .map((item) => {
      let score = 0;
      if (cleanDomain !== 'all' && item.domainId === cleanDomain) score += 1.5;
      if (msgLower.length >= 4 && item.titleLower.includes(msgLower)) score += 10;

      for (const tok of currentTokens) {
        if (item.titleLower.includes(tok)) score += 4.0;
        else if (item.domainLabel.toLowerCase().includes(tok) || item.topicLabel.toLowerCase().includes(tok)) score += 2.2;
        else if (item.tokens.has(tok)) score += 1.5;
      }

      // Context carry-over from recent turns (lower weight so topic switches still win)
      let ctxHits = 0;
      for (const tok of contextTokens) {
        if (item.titleLower.includes(tok)) {
          score += 1.4;
          ctxHits++;
        } else if (item.tokens.has(tok)) {
          score += 0.45;
          ctxHits++;
        }
        if (ctxHits >= 8) break;
      }

      if (pinned && item.id === pinned.id) score += 8.0;
      return { item, score };
    })
    .filter((x) => x.score >= 1.5)
    .sort((a, b) => b.score - a.score);

  const selected = [];
  if (pinned) selected.push(pinned);
  for (const { item } of scored) {
    if (selected.length >= 3) break;
    if (!selected.some((s) => s.id === item.id)) selected.push(item);
  }
  return selected;
}

function buildKbReferenceBlock(articles) {
  if (!articles.length) return '';
  const blocks = articles.map((a, idx) => {
    const excerpt = a.body.length > 1400 ? `${a.body.slice(0, 1400)}…` : a.body;
    return `[Protocol ${idx + 1}: ${a.title} | Domain: ${a.domainLabel} | Priority: ${a.priority.toUpperCase()} | ID: ${a.id}]\n${excerpt}`;
  });
  return `\n\nVERIFIED AEGIS OFFLINE PROTOCOLS (use these as primary ground truth when relevant; adapt directly to the user's specific situation and follow-up constraints):\n${blocks.join('\n\n')}`;
}

function parseReplyAndFollowUps(rawText) {
  const text = String(rawText || '').trim();
  const lines = text.split(/\r?\n/);
  let followUps = [];
  let topic = null;
  const kept = [];

  for (const line of lines) {
    const trimmed = line.trim();
    if (/^FOLLOWUPS\s*:/i.test(trimmed)) {
      const payload = trimmed.replace(/^FOLLOWUPS\s*:/i, '').trim();
      followUps = payload
        .split('|')
        .map((s) => s.replace(/^[-•*\d.)\s]+/, '').trim())
        .filter((s) => s.length >= 4 && s.length <= 70)
        .slice(0, 3);
      continue;
    }
    if (/^TOPIC\s*:/i.test(trimmed)) {
      const val = trimmed.replace(/^TOPIC\s*:/i, '').trim().slice(0, 48);
      if (val) topic = val;
      continue;
    }
    kept.push(line);
  }

  return {
    reply: kept.join('\n').trim(),
    followUps,
    topic,
  };
}

function buildAlternatingGeminiHistory(safeHistory, currentMessage) {
  // Remove trailing duplicate user turn if client included the current prompt inside history
  const trimmed = [...safeHistory];
  if (
    trimmed.length > 0 &&
    trimmed[trimmed.length - 1].role === 'user' &&
    trimmed[trimmed.length - 1].content.trim().toLowerCase() === currentMessage.trim().toLowerCase()
  ) {
    trimmed.pop();
  }

  // Merge consecutive same-role messages so Gemini chats always see clean alternating turns
  const normalized = [];
  for (const item of trimmed) {
    const role = item.role === 'user' ? 'user' : 'model';
    if (normalized.length === 0 && role === 'model') continue; // history should begin with user
    if (normalized.length > 0 && normalized[normalized.length - 1].role === role) {
      normalized[normalized.length - 1].parts[0].text += `\n\n${item.content}`;
    } else {
      normalized.push({ role, parts: [{ text: item.content }] });
    }
  }

  // Ensure history ends on a model turn before sendMessage(userPrompt)
  if (normalized.length > 0 && normalized[normalized.length - 1].role === 'user') {
    normalized.pop();
  }
  return normalized;
}

app.post('/api/ai/chat', async (req, res) => {
  const {
    message,
    emergency = false,
    domain = 'all',
    history = [],
    mode = 'general',
    userLocation = null,
    activeArticleId = null,
  } = req.body || {};

  if (typeof message !== 'string' || !message.trim()) {
    return res.status(400).json({ error: 'A non-empty message string is required.' });
  }
  const cleanMessage = message.trim().slice(0, 2000);
  const cleanDomain = typeof domain === 'string' ? domain.trim().slice(0, 64) : 'all';
  const cleanArticleId = typeof activeArticleId === 'string' ? activeArticleId.trim().slice(0, 80) : null;
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
        .slice(-16)
        .filter((item) => {
          const text = typeof item?.content === 'string' ? item.content : typeof item?.text === 'string' ? item.text : '';
          const role = item?.role;
          return (
            item &&
            (role === 'user' || role === 'assistant' || role === 'model') &&
            text.trim().length > 0
          );
        })
        .map((item) => {
          const text = typeof item.content === 'string' ? item.content : item.text;
          return {
            role: item.role === 'user' ? 'user' : 'assistant',
            content: text.trim().slice(0, 1500),
          };
        })
    : [];

  const matchedProtocols = retrieveKbContext(cleanMessage, safeHistory, cleanArticleId, cleanDomain);
  const kbContextBlock = buildKbReferenceBlock(matchedProtocols);

  const followUpFormattingRule =
    '\n\nCONTEXT & CONVERSATION RULES:\n' +
    '- Carefully track all details mentioned earlier in the conversation (environment, available tools, injuries, quantities, missing gear, number of people, altitude/climate). Never repeat generic advice that ignores constraints the user already stated.\n' +
    '- If the user asks a short follow-up (e.g. "what if I have no fire?", "how much for 4 liters?", "what next?"), answer directly within the ongoing scenario.\n' +
    '- If one critical detail would materially improve safety (e.g., whether water is muddy, bleeding severity, ambient temperature), ask 1 brief clarifying question at the end.\n' +
    '- On the very last two lines of your response, ALWAYS output:\n' +
    'TOPIC: <2-4 word active survival topic>\n' +
    'FOLLOWUPS: <short follow-up 1> | <short follow-up 2> | <short follow-up 3>';

  let systemInstruction;
  if (isEmergency) {
    systemInstruction =
      'You are AEGIS, a Survival & Emergency Reference Chatbot. Give TERSE, numbered, immediately actionable steps tailored to the user\'s exact situation and prior messages. Lead with the most critical life-saving action. No preamble. Prefix critical hazards with WARNING:. Remind users to contact official local emergency services (911/112) whenever phone service is available.' +
      kbContextBlock +
      followUpFormattingRule;
  } else if (cleanMode === 'maps') {
    systemInstruction =
      'You are AEGIS, a Preparedness & Field Resource Advisor grounded with live Google Maps data. Help the user identify real-world local preparedness resources (hardware/outdoor stores, grocery/water supplies, pharmacies, public parks, campgrounds, and community facilities) for planning and situational awareness. Use **Heading** for sections and - for bullet points. Note that this is for preparedness planning and informational reference, not emergency dispatch.' +
      kbContextBlock +
      followUpFormattingRule;
  } else if (cleanMode === 'search') {
    systemInstruction =
      'You are AEGIS, a Civilization & Preparedness Intelligence Chatbot grounded with live Google Search data. Provide accurate, up-to-date, context-aware guidance on weather advisories, preparedness, survival science, water, agriculture, and engineering. Use **Heading** for sections, numbered lists for steps, and - for bullet points. Prefix genuinely hazardous steps with WARNING:.' +
      kbContextBlock +
      followUpFormattingRule;
  } else {
    systemInstruction =
      'You are AEGIS, an intelligent Survival & Civilization Preparedness Chatbot — expert in wilderness survival, first aid reference, water purification, agriculture, off-grid engineering, and emergency problem-solving. Maintain deep context across multi-turn conversations and tailor every answer to the user\'s exact constraints, gear, and follow-up questions. Use **Heading** for sections, numbered lists for steps, and - for bullet points. Prefix genuinely hazardous steps with WARNING:.' +
      kbContextBlock +
      followUpFormattingRule;
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
      // - gemini-4-hydrogen for context-aware multi-turn chat, Search grounding, and Maps grounding
      // - gemini-3.1-flash-lite for ultra-low-latency fast mode
      const model = cleanMode === 'fast' ? 'gemini-3.1-flash-lite' : 'gemini-4-hydrogen';
      const geminiHistory = buildAlternatingGeminiHistory(safeHistory, cleanMessage);

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

      const rawText = (response.text || '').trim();
      if (!rawText) {
        return res.status(502).json({
          error: 'Empty response from Gemini API.',
          offlineRecommended: true,
        });
      }

      const parsed = parseReplyAndFollowUps(rawText);
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
        reply: parsed.reply || rawText,
        source: sourceTag,
        model,
        mode: cleanMode,
        groundingSources,
        citedArticleIds: matchedProtocols.map((p) => p.id).filter(Boolean),
        followUps: parsed.followUps,
        contextTopic: parsed.topic || matchedProtocols[0]?.title || null,
      });
    }

    // Anthropic server-side fallback when ANTHROPIC_API_KEY is configured instead of GEMINI_API_KEY
    const cleanAnthropicHistory = buildAlternatingGeminiHistory(safeHistory, cleanMessage).map((h) => ({
      role: h.role === 'model' ? 'assistant' : 'user',
      content: h.parts[0].text,
    }));

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
          ...cleanAnthropicHistory,
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
    const rawText = data?.content?.[0]?.text?.trim() || '';
    if (!rawText) {
      return res.status(502).json({
        error: 'Empty response from AI provider.',
        offlineRecommended: true,
      });
    }

    const parsed = parseReplyAndFollowUps(rawText);
    return res.json({
      reply: parsed.reply || rawText,
      source: 'anthropic-proxy',
      groundingSources: [],
      citedArticleIds: matchedProtocols.map((p) => p.id).filter(Boolean),
      followUps: parsed.followUps,
      contextTopic: parsed.topic || matchedProtocols[0]?.title || null,
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
