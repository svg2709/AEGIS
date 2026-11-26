# AEGIS — Offline Survival AI for Humanity

**AEGIS (Adaptive Emergency Guide & Intelligence System)** is an offline-first survival knowledge base, field preparedness toolkit, and hybrid Gemini AI companion available as a **native Android app (Kotlin + Jetpack Compose)** and a **self-hostable Web & AI Proxy server (Node.js + Express)**.

Everything critical — all **81 survival protocols across 14 domains and 37 topics** — is bundled locally so AEGIS works with **zero internet connectivity** on any device, while seamlessly unlocking live **Gemini multi-turn chat**, **Google Search grounding**, **Google Maps grounding**, and an **interactive Preparedness Field Resource Map** whenever connectivity is available.

---

## Quick Start

### 1. Download the Android App (Recommended)
1. Go to [**Releases**](../../releases/latest) and download the latest `aegis-survival-v*.apk`.
2. *(Optional)* Verify integrity against the attached `SHA256SUMS.txt`.
3. Install on Android 8.0+ (API 26+).
4. Future updates check GitHub Releases automatically (paused when Battery Saver is active), show the changelog, and **always ask your permission** before downloading, SHA-256 verifying, and installing.

### 2. Run the Web App & Optional AI/Maps Server Locally
```bash
git clone https://github.com/Aditya-Shaswat/AEGIS.git
cd AEGIS
npm install
cp .env.example .env   # Optional: add GEMINI_API_KEY and/or GOOGLE_MAPS_API_KEY
npm run dev
```
- **Web App**: `http://localhost:3000`
- **Landing Page**: `http://localhost:3000/landing`
- **Health Check**: `http://localhost:3000/api/health`

Without any API keys configured, the web app runs in **100% Offline Knowledge Base mode** immediately.

---

## Key Capabilities

### Native Android App (`android/`)
- **True AMOLED Pitch-Black Default (`#000000`)**: Shuts off OLED subpixels to maximize battery endurance during prolonged outages.
- **Automatic Android Battery Saver Detection**: Monitors `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED` and battery telemetry in real time, automatically switching to **Ultra AMOLED Battery Saver** (`#000000` canvas & cards, conserved routine haptics, paused background update checks) whenever system Battery Saver activates.
- **User-Confirmed Auto-Updates**: Checks GitHub Releases, presents version diff & release notes, verifies SHA-256 checksums from `SHA256SUMS.txt`, and launches Android's package installer only after explicit user confirmation.
- **100% Offline Survival Knowledge Base**: 81 protocols across 14 domains bundled inside `assets/aegis_kb.json` with instant offline search, tier/priority filtering, bookmarks, and adjustable reading font size (`S` / `M` / `L` / `XL`).
- **Field Survival Utilities**:
  - Interactive **Rule of 3** triage shortcuts
  - Live **Morse SOS** camera flashlight + full-screen visual beacon (`... --- ...`) with step progress
  - Altitude-aware **Water Boiling Countdown Timer** (1 min sea level / 3 min > 2,000 m)
  - **Unscented Bleach Water-Purification Calculator** (1L / 4L / 10L / 20L, clear vs. cloudy water)
  - Native Android haptics & share sheet protocol export
- **Optional HTTPS AI Proxy Integration**: Point the Android app at your self-hosted AEGIS server in **System > Online AI Proxy** to unlock Gemini multi-turn chat, Web Search grounding, and Maps AI grounding, with automatic offline KB fallback whenever signal drops.

### Web App & Express Server (`index.html`, `server.js`)
- **Multi-Turn Gemini Chatbot**: Maintains conversation history with a dedicated survival & preparedness system role, supporting:
  - **Gemini (`gemini-4-hydrogen`)** — general multi-turn survival, engineering, and preparedness Q&A
  - **Fast (`gemini-3.1-flash-lite`)** — low-latency quick answers
  - **Web Search (`gemini-4-hydrogen` + `googleSearch`)** — live web-grounded advisories and up-to-date facts with clickable source links
  - **Maps AI (`gemini-4-hydrogen` + `googleMaps`)** — location-grounded preparedness resource guidance with clickable Google Maps place links
- **Interactive Field Resource Map (Google Maps Platform)**: Built with modern Maps JavaScript API (`importLibrary`), `AdvancedMarkerElement`, `PinElement`, and **Places API (New)** (`Place.searchNearby` & `Place.searchByText`) to locate nearby hardware/supply stores, grocery & water stores, pharmacies, parks, campgrounds, and community hubs with opt-in, revocable browser geolocation.
- **Battery Saver Detection on Web**: Uses the Battery Status API (plus a manual top-bar toggle) to engage `#000000` AMOLED power-saving styles and disable CSS animations when battery is low.

---

## Server Configuration & API Reference

### Environment Variables (`.env`)
| Variable | Required | Purpose |
| :--- | :--- | :--- |
| `PORT` | No | Listening port (defaults to `3000`). |
| `GEMINI_API_KEY` | Optional | Enables server-side Gemini multi-turn chat, Google Search grounding, and Google Maps grounding via `@google/genai`. Never exposed to the browser. |
| `GOOGLE_MAPS_API_KEY` | Optional | Enables the interactive Preparedness Field Map (Maps JavaScript API + Places API New). Restrict this key by HTTP referrer and APIs in Google Cloud Console. |
| `ANTHROPIC_API_KEY` | Optional | Optional fallback text provider if `GEMINI_API_KEY` is not set. |

### HTTP Endpoints (`server.js`)
| Method & Path | Description |
| :--- | :--- |
| `GET /` | Serves the AEGIS web application (`index.html`). |
| `GET /landing` | Serves the standalone AMOLED landing page (`aegis-landing.html`). |
| `GET /api/health` | Returns server readiness, configured AI provider, Maps status, and enabled capabilities. |
| `GET /api/kb` | Serves the bundled 81-article offline survival dataset (`aegis_kb.json`). |
| `GET /api/maps/config` | Returns whether the Maps JavaScript API is configured for the interactive Field Map. |
| `POST /api/ai/chat` | Server-side multi-turn chat proxy accepting `{ message, emergency, domain, mode, history, userLocation }`, where `mode` is `"general"`, `"fast"`, `"search"`, or `"maps"`. Returns `{ reply, source, model, mode, groundingSources }`. |

> **Security**: `server.js` blocks direct HTTP access to `server.js`, `package.json`, `android/`, `.git/`, `.github/`, and all dotfiles. `GEMINI_API_KEY` and `ANTHROPIC_API_KEY` are read strictly server-side via `process.env`.

---

## Building the Android App & Automated GitHub Releases

### Local Android Build
1. Open `/android` in **Android Studio** (JDK 17 or 21, Android SDK 35).
2. Run **Build > Make Project** or build from the command line:
   ```bash
   cd android
   ./gradlew assembleRelease assembleDebug
   ```

### Automated GitHub Actions CI/CD (`.github/workflows/android-release.yml`)
- **Smart App-Only Version Bumping**: Automatically increments the semantic version (`versionName` & deterministic `versionCode`) and publishes a new GitHub Release **only when actual application files change** (`android/`, `index.html`, `aegis-landing.html`, `server.js`, `package.json`, `metadata.json`) or when you push an explicit tag (`git tag v1.2.0 && git push origin v1.2.0`).
- **Docs/CI-Only Commits Ignored**: Edits limited to `README.md`, `CHANGELOG.md`, `.gitignore`, `.env.example`, or `.github/` do **not** bump the app version or create a release.
- **Release Artifacts**: Every automated release includes compiled release/debug APKs, an auto-generated commit changelog, and `SHA256SUMS.txt` for in-app update integrity verification.

#### Optional Production Signing Secrets
Under **Settings > Secrets and variables > Actions**, add:
- `ANDROID_KEYSTORE_BASE64` — Base64-encoded `.jks` keystore
- `ANDROID_KEYSTORE_PASSWORD` — Keystore password
- `ANDROID_KEY_ALIAS` — Signing key alias
- `ANDROID_KEY_PASSWORD` — Key password

Without these secrets, GitHub Actions still compiles and publishes an installable APK automatically.

---

## Repository Layout
```text
AEGIS/
├── android/                           # Native Kotlin + Jetpack Compose Android project
│   └── app/src/main/
│       ├── assets/aegis_kb.json       # Bundled 14 domains / 37 topics / 81 protocols
│       └── java/com/aegis/survival/
│           ├── data/                  # Offline KB repository, search, settings, HTTPS proxy client
│           ├── hardware/              # Battery Saver monitor, Morse SOS torch, haptics, share
│           ├── update/                # GitHub Releases updater with SHA-256 verification
│           └── ui/                    # AMOLED theme, Ask, Library, Reader, Emergency, System screens
├── .github/workflows/
│   └── android-release.yml            # Automated APK builds, changelogs, checksums, GitHub Releases
├── server.js                          # Express server + Gemini Chat/Search/Maps grounding proxy
├── index.html                         # AMOLED Web app + Interactive Field Resource Map
├── aegis-landing.html                 # Project landing page
├── CHANGELOG.md                       # Release changelog history
└── .env.example                       # Server environment variable template
```

---

## Important Safety & Terms Notice
- AEGIS is a **preparedness, educational, and informational planning tool** — it is **not** a substitute for professional medical care or official emergency services (such as 911/112). Always contact local emergency responders whenever service is available.
- Interactive map features are powered by **Google Maps Platform** and are subject to the [Google Maps Platform Terms of Service](https://cloud.google.com/maps-platform/terms?utm_campaign=gmp_mcp_codeassist_v1_aistudio) and [Acceptable Use Policy](https://cloud.google.com/maps-platform/terms/aup?utm_campaign=gmp_mcp_codeassist_v1_aistudio).
