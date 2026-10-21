# Development Philosophy & AI Transparency

ShinKu is built on a simple foundation: **to be the most refined, fast, and feature-rich reader for Android.**

As open-source software evolves in the era of artificial intelligence, we believe in radical transparency regarding how ShinKu is designed, developed, and maintained.

---

## 🤖 AI Assistance, Human Craftsmanship

ShinKu is actively developed with the assistance of cutting-edge AI engineering agents (such as Google DeepMind's Antigravity and Gemini systems). AI helps us:

* Accelerate repetitive refactoring and boilerplate reduction.
* Analyze complex upstream code diffs across Tachiyomi, Mihon, and SY bases.
* Draft initial test matrices, architecture specs, and documentation.
* Rapidly explore edge-case bugs and UI responsiveness challenges.

### 🛡️ The Non-Negotiable Human-in-the-Loop Standard

However, **AI never writes unchecked code for ShinKu.** 

* **Hand-Curated Architecture:** Every single feature, component, and database migration is conceived, directed, and architecturally verified by the developer.
* **Rigorous Line-by-Line Code Review:** No AI-generated code is merged without thorough human review for security, resource leaks, memory bounds, and Kotlin Coroutine correctness.
* **Tested by a Daily Reader:** The developer is a daily, heavy manga reader. Every release build (`devRelease`) is installed on actual Android physical hardware and actively used in real-world reading sessions before public release.
* **Zero Blind Trust:** We treat AI as an ultra-fast junior engineer pair-programmer; the taste, tactile feel, aesthetic decisions, and bug-hunting remain 100% human-driven.

---

## ⚡ The Vibe & Speed Principles

1. **Zero Distraction (Content First):** UI chrome should get out of the way. When you open a chapter, the navigation dock floats away, black backgrounds melt into OLED displays, and only the story remains.
2. **Resilience Over Excuses:** Network flakes and Cloudflare challenges happen. ShinKu's resumable byte-range downloads and anti-bot spoofing mean your library updates reliably.
3. **No Monolithic Bloat:** We refactor and modularize code strictly to ensure fast startup times, smooth 120Hz scrolling, and minimal battery consumption.
4. **Privacy & Offline First:** Your reading habits, library entries, and progress are stored exclusively on your device. No telemetry, no third-party tracking, and no external tracking servers.
