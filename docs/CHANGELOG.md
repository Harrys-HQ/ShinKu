# ShinKu Changelog

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
