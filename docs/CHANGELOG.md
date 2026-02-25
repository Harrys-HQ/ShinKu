# ShinKu Changelog

## 2.1.2 (2026-02-25)
### Fixed
- **Migration Crash:** Added compatibility bridges for `JsoupExtensions` and `RxExtension` to resolve `NoClassDefFoundError` in extensions (e.g., Mangabuddy).
- **Migration Stability:** Added error handling to the Smart Search engine to prevent source-specific crashes from hanging batch migrations.
- **Vibe Search Isolation:** Restricted Vibe Search to global discovery; migration now strictly uses Direct Title Search to prevent AI-related timeouts.

### Added
- **Source Health Dashboard:** Monitor real-time reliability and speed of all extensions.
- **Smart Throttling:** Automatically slows down library updates for "Sensitive" or slow sources to prevent IP bans.
- **Global Repo Scanner:** Background task to periodically check the health of all available English sources in the repository.
- **Weighted Grading:** New 5-star rating system based on both Reliability (70%) and Speed (30%).
- **Batch Migration Parallelization:** Optimized Mass Migration to process 3 manga simultaneously for significantly faster lookups.
- **Targeted Scanning:** Ability to refresh health for only installed sources or the entire repository.

## 2.1.1 (2026-02-24)
- **New: Full Rebrand:** Completed the transition from Mihon/Tachiyomi to the **ShinKu** identity across the entire codebase.
- **New: 120Hz Support:** Added High Refresh Rate support in **Settings > ShinKu Features** for significantly smoother scrolling on compatible devices.
- **New: Performance Profiles:** Added specialized profiles in **Settings > ShinKu Features**:
    - **E-Ink Optimized:** Disables animations, forces high contrast, and auto-flashes pages to prevent ghosting on e-readers.
    - **Low RAM / Power:** Limits background preloading and caps image cache to 25MB for budget hardware.
- **Improved: Extension Compatibility:** Restored full binary compatibility with existing Tachiyomi and Mihon extensions.
- **Improved: Legacy Backups:** Restored the ability to import all standard `.tachibk` files from Mihon and TachiyomiSY.
- **Improved: External Deep Links:** Added support for `tachiyomi://` and `mihon://` links (e.g. for adding repos or tracker login) so external sites correctly recognize ShinKu.
- **UI: Icon Refinement:** Switched to a new foreground-only logo for notifications and themed icons to prevent cropping artifacts.
- **Technical: Massive Package Rename:** Migrated the entire internal structure to `com.shinku.reader`.
- **Technical: Build System:** Unified all build logic and convention plugins under the `shinku.buildlogic` namespace.
- **Branding:** Updated all documentation, help URLs, and social links to point to the new ShinKu ecosystem.

## 2.0.0 (2026-02-19)
- **New:** Rebranding to ShinKu 2.0.0 (Crimson).
- **New:** AI-powered **Vibe Search** with Google Gemini integration.
- **New:** **Reading Journey** statistics card in the More tab.
- **New:** **Dynamic Theme** overhaul with immersive cover-based coloring.
- ... [rest of existing changelog]
