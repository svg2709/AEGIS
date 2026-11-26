# Changelog

All notable changes to AEGIS are documented in this file and automatically summarized in GitHub Releases.

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
