# Changelog

All notable changes to AEGIS are documented in this file and automatically summarized in GitHub Releases.

## [1.1.0] - 2026-11-26

### Added
- **User-Confirmed In-App Auto-Updates**: Android and web now check GitHub Releases for newer versions, show the release changelog, verify APK SHA-256 checksums (`SHA256SUMS.txt`), and explicitly ask for user confirmation before downloading and installing.
- **Automatic Battery Saver AMOLED Detection**: Monitors Android `PowerManager.ACTION_POWER_SAVE_MODE_CHANGED` (and web Battery Status API) to automatically engage pitch-black `#000000` Ultra AMOLED power-saving settings, conserve routine haptics, and pause background update checks while Battery Saver is active.
- **Multi-Turn Gemini Chat & Live Grounding**: Upgraded `/api/ai/chat` and both clients with multi-turn `gemini-4-hydrogen` chat, fast `gemini-3.1-flash-lite` mode, live **Google Search grounding**, and **Google Maps grounding** with clickable verified source links and seamless offline KB fallback.
- **Interactive Preparedness Field Resource Map**: Added a Google Maps Platform map tab (Maps JavaScript API + Places API New `searchNearby`/`searchByText` + `AdvancedMarkerElement`) for locating nearby supply stores, food/water, pharmacies, parks, campgrounds, and community hubs with opt-in revocable location sharing.
- **Expanded Field Survival Utilities & Legibility**: Added interactive Rule of 3 triage tiles, altitude-aware Water Boiling countdown timer, unscented bleach water-purification calculator, visual Morse SOS step readout, saved bookmarks, S/M/L/XL reading text scaling, and upgraded `Plus Jakarta Sans` typography.

### Changed
- **Smart App-Only Auto-Versioning**: Updated GitHub Actions CI/CD to bump versions and cut releases only when actual application files change (skipping docs/git/CI-only commits) and to respect explicit minor/major version bumps in `package.json`.
- **Expanded GitHub Documentation**: Rewrote `README.md` with quick-start install instructions, server API/environment reference, Android build steps, and CI/CD release documentation.

## [1.0.0] - 2026-11-26

### Added
- **Native Kotlin & Jetpack Compose Android App (`android/`)**: Built a standalone Android application targeting SDK 35 (Min SDK 24) with Material 3.
- **True AMOLED Pitch-Black Theme (`#000000`)**: Default `#000000` canvas across system bars, navigation, chat, library, and protocol reader for maximum OLED battery conservation in emergencies.
- **Complete Offline Survival Knowledge Base**: Extracted all 14 domains, 37 topics, and 81 verified survival protocols into bundled offline Android assets (`aegis_kb.json`) with instant local keyword/phrase ranking.
- **Native Emergency Hardware Tools**: International Morse-code (`... --- ...`) rear camera flash SOS beacon with automatic full-screen visual fallback, tactile haptics (`VibratorManager`), connectivity checks, and native Android share sheet export.
- **Optional Server-Side AI Proxy (`/api/ai/chat`)**: Server-managed Gemini (`gemini-4-hydrogen`) or Anthropic proxy with strict payload validation and automatic offline KB fallback.
- **Automated GitHub Actions Compilation & Auto-Releases**: Compiles Android APKs directly in GitHub on every push/tag, generates release changelogs and SHA-256 checksums, and automatically publishes GitHub Releases.

### Changed & Cleaned
- Removed duplicate and legacy repository artifacts (`index2.html`, `app.html`, `aegis-ultimate.html`, `AEGIS Prototype.zip`, `aegis-survival-v1.0.3.apk`, and `RelieFlow.ipynb`).
- Updated web preview (`index.html`) and standalone landing page (`aegis-landing.html`) to true AMOLED `#000000` default backgrounds and removed browser-side API key inputs.
