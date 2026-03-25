# ShinKu Changelog

## 2.0.0 "Reborn" (2026-03-24)
### Added
- **Full Rebrand:** Transitioned entire application to the `com.shinku.reader` namespace for a clean slate.
- **ShinKu Settings Hub:** Consolidated all specialized features (Gemini AI, Smart Categorizer, Performance Profiles) into a single, organized settings menu.
- **Binary Compatibility:** Restored full support for Tachiyomi and Mihon extensions by hardening the internal binary interface.
- **Advanced Discovery:** Implemented a multi-layered extension detection engine for Android 11-14 compatibility.
- **Preview Release Channel:** Established `devRelease` as the new standard for stability verification with R8 minification.

### Improved
- **Extension Reliability:** Optimized the Extension Installer and Receiver to comply with modern Android security policies.
- **UI Organization:** Cleaned up the "More" tab by relocating advanced maintenance tools to the ShinKu Settings hub.
- **ProGuard Hardening:** Updated R8 rules to prevent obfuscation of core extension interfaces.

### Fixed
- **Startup Stability:** Resolved a critical crash in Preview builds related to uninitialized Firebase analytics.
- **Extension Visibility:** Fixed a long-standing issue where installed extensions were not appearing in the Browse tab.

## 2.2.3 (2026-03-13)
### Added
- **Storage & Speed overhaul:** New dedicated settings category under ShinKu Features.
- **Download Migration Tool:** One-click utility to fix missing/broken chapters when migrating from other forks.
- **Aggressive Reader Prefetch:** Optimized background loading for gapless chapter transitions.
- **Image Transcoding:** Experimental Auto-WebP support to reduce library storage footprint by up to 50%.
- **Enhanced Caching:** Dedicated 100MB Coil 3 DiskCache for significantly faster cover loading.

### Improved
- **Library Accuracy:** Guaranteed 100% accurate unread counts that update instantly upon reading.
- **UI Performance:** GPU-accelerated scrolling optimized for 120Hz displays.
- **Reader Buffer:** Expanded 30-page background buffer for a snappier reading experience.
- **E-Ink Optimizations:** Moved ghosting and flash controls to the Storage & Speed menu for easier access.

## 2.2.2 (2026-03-06)
... rest of changelog ...
