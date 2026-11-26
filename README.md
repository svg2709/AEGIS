# AEGIS — Offline Survival AI for Humanity

AEGIS (Autonomous Emergency Guidance & Intelligence System) is an offline-first survival and civilization recovery platform designed to work with zero internet connectivity.

## What's Inside

- **Native Android App (`android/`)**: Built with **Kotlin + Jetpack Compose** (Material 3) on a **true AMOLED pitch-black (`#000000`)** default canvas to maximize OLED battery life.
  - **100% Offline Knowledge Base**: All **14 domains, 37 topics, and 81 survival protocols** bundled directly inside the APK (`aegis_kb.json`) with fast local search and ranking.
  - **Emergency Hardware Tools**: International Morse SOS camera torch beacon (`... --- ...`) with automatic full-screen beacon fallback, tactile haptics, and native Android share sheet export.
  - **Optional HTTPS AI Proxy**: Connect to your self-hosted AEGIS server for online synthesis while automatically falling back to the offline knowledge base when disconnected.
- **Web Preview & Optional Server AI Proxy (`server.js`, `index.html`, `aegis-landing.html`)**:
  - Serves the interactive web app at `/` and landing page at `/landing` on port `3000`.
  - Exposes `GET /api/health`, `GET /api/kb`, and `POST /api/ai/chat` (reads `GEMINI_API_KEY` or `ANTHROPIC_API_KEY` strictly from server environment variables).
- **Automated GitHub APK Builds & Releases (`.github/workflows/android-release.yml`)**:
  - Compiles release and debug APKs directly on GitHub Actions using JDK 21 and Gradle 8.11.1.
  - Automatically generates commit changelogs + SHA-256 checksums and publishes a new GitHub Release on every push to `main` or tag `v*.*.*`.

## Building & Running Locally

### Web Preview & Optional AI Proxy
```bash
npm install
npm run dev
```
Open `http://localhost:3000` for the web app or `http://localhost:3000/landing` for the landing page. Copy `.env.example` to `.env` if you want to enable the optional server-side AI proxy.

### Native Android App
1. Open the `android/` directory in Android Studio (JDK 17/21, Android SDK 35).
2. Run **Build > Make Project** or `./gradlew assembleRelease`.

### Automated GitHub Releases & Optional Release Signing
Every push to `main` or version tag (`git tag v1.1.0 && git push origin v1.1.0`) automatically builds the APK, generates a release changelog, and publishes a GitHub Release with attached APKs and `SHA256SUMS.txt`.

To sign release APKs with your own Android release keystore in GitHub Actions, add these repository secrets under **Settings > Secrets and variables > Actions**:
- `ANDROID_KEYSTORE_BASE64` — Base64-encoded `.jks` keystore file
- `ANDROID_KEYSTORE_PASSWORD` — Keystore password
- `ANDROID_KEY_ALIAS` — Signing key alias
- `ANDROID_KEY_PASSWORD` — Signing key password

Without these secrets, GitHub Actions still builds and publishes an immediately installable APK automatically.
