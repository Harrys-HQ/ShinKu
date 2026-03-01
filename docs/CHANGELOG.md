# ShinKu Changelog

## 2.2.1 (2026-03-01)
### Added
- **Performance Enhancements:** Completed tracking and optimization tasks to improve application responsiveness.

## 2.2.0 (2026-02-25)
### Added
- **AI Metadata Enrichment:** New "Fix with AI" button in Edit Info to automatically generate high-quality descriptions and tags using Gemini.
- **"Zen" Immersive Reader:** New mode to hide all UI distractions (page numbers, clock, bars) for pure immersion.
- **AI-Powered Feed:** The Feed now proactively suggests new manga based on your top reading genres and history.
- **Sync Conflict Resolver:** Smart detection of reading progress discrepancies across devices with a one-tap "Jump to Progress" option.

### Fixed
- **UI Scaling:** Fixed text wrapping in the Reading Journey card and text cutoff in the Statistics genre breakdown.
- **Dropbox Reliability:** Improved auto-sync triggers when opening and finishing chapters.

## 2.1.2 (2026-02-25)
### Added
- **AI-Powered Reader:** Integrated Google ML Kit OCR and Gemini AI for **Live Translation** and **Interactive Footnotes** (cultural context) directly in the reader.
- **Enhanced Reading Journey:** Expanded stats card with **Reading Streaks** and **Top Genre** breakdown.
- **Visual Polish:** Added an animated **Shimmer Effect** for loading manga covers across the app.
- **Dropbox Auto-Sync:** Enabled automatic background synchronization triggered by reading progress (chapter open/read).
- **Source Health Dashboard:** Monitor real-time reliability and speed of all extensions.

### Improved
- **Precise Fast Scroller:** Completely rewrote the Library Grid scroller to remove average-height assumptions, ensuring pixel-perfect tracking for any library size.
- **Smart Throttling:** Automatically slows down library updates for "Sensitive" or slow sources to prevent IP bans.
- **UI Layouts:** Optimized Reading Journey and Statistics screens to prevent text wrapping and cutoff on smaller screens.

### Fixed
- **Migration Crash:** Added compatibility bridges for `JsoupExtensions` and `RxExtension` to resolve `NoClassDefFoundError` in legacy extensions.
- **Migration Stability:** Added error handling to the Smart Search engine to prevent source-specific crashes from hanging batch migrations.
- **Vibe Search Isolation:** Restricted Vibe Search to global discovery; migration now strictly uses Direct Title Search to prevent AI-related timeouts.

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
