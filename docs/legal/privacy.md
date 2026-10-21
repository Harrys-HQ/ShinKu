# Privacy Policy

**Effective Date:** October 2026

Your privacy is a fundamental human right. ShinKu is engineered from the ground up as a **local-first, zero-telemetry** application.

---

## 🔒 What Information We Collect

### 1. No Telemetry or Usage Tracking
ShinKu does **not** collect, store, transmit, or sell:
* Your reading history, habits, or bookmarks.
* Your device identifiers (IMEI, MAC address, advertising ID).
* Analytics on what features you click or how often you use the app.
* Crash logs sent to third-party proprietary trackers (no Firebase Crashlytics, no Google Analytics, no Facebook SDK).

### 2. Local Storage
All your data—including your library, reading progress, custom categories, tracker tokens, and downloads—is stored strictly on your local device inside your private SQLite database (`tachiyomi.db`) and user-designated storage folder.

---

## 🌐 Network Connections & Third Parties

ShinKu only connects to external servers when explicitly requested by you:

1. **Third-Party Trackers (Optional):**
   If you choose to log into tracking services (e.g., MyAnimeList, AniList, Kitsu, MangaUpdates), authentication tokens are stored locally on your device and used exclusively to synchronize your personal reading progress with those services over secure HTTPS.
2. **AI Translation & Vibe Search (Optional):**
   If you configure Google Gemini API for live page translation or Vibe Search, textual queries or image panels are sent directly from your device to the Google Gemini endpoint using your own personal API key. ShinKu maintainers have no access to these requests.
3. **App Updates:**
   When checking for updates, ShinKu queries GitHub's public API (`https://api.github.com/repos/Harrys-HQ/ShinKu/releases`) to compare the latest version tag. No personal identifiers are transmitted.

---

## 📬 Contact

For questions regarding this privacy policy or our open-source codebase, open a ticket on our [GitHub Issues](https://github.com/Harrys-HQ/ShinKu/issues) board.
