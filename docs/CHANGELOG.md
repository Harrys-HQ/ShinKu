# ShinKu Changelog

## 2.3.0 "Fluidity" (2026-04-03)
### Added
- **Atmospheric Audio:** New preference to play ambient sounds matching the manga's genre for a more immersive reading experience.
- **Mood Lighting:** Subtly adjusts screen color temperature based on genre (e.g., warmer for Romance, cooler for Horror).
- **Haptic Profiles:** Subtle tactile feedback for page turns, long-presses, and milestone achievements.
- **Deep Reading Stats:** Advanced tracking of reading time and volume per genre and author.
- **Reading Milestones:** New badge system to celebrate reading goals and achievements.
- **AI "For You" Feed:** Personalized recommendations powered by Gemini based on your recent library activity.
- **AI Image Upscaling:** Optional on-device processing to improve the clarity of low-resolution pages.
- **Backdrop Blurs:** Dynamic cover-based blurs added to Library and Manga Info screens for increased UI depth.
- **Bookmarked Chapters:** Added a dedicated option to download only bookmarked chapters.
- **VPN Support:** Automatic library updates now support VPN-aware connection handling.

### Improved
- **Architectural Modernization:** Commenced migration from Injekt to Koin for better stability and Compose integration.
- **Database Performance:** Optimized history and manga tables with new composite indexes for faster loading.
- **Predictive Pre-loading:** Reader now dynamically adjusts prefetching based on your average reading speed.
- **Self-Hosted Sync:** Added native support for WebDAV and Nextcloud synchronization.
- **Migration Logic:** Improved manga migration to preserve page progress and source order correctly.

### Fixed
- **Cloudflare Guard:** Resolved the "blank page" issue when encountering Cloudflare protection.
- **Extension Stability:** Fixed "Pending" state bugs in the extension installer.
- **Thread Starvation:** Ported upstream fixes for smoother performance during heavy background tasks.

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
