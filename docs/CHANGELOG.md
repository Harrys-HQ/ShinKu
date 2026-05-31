# ShinKu Changelog

## 2.5.1 "Safe Sync & Core Optimization" (2026-05-31)
### Fixed
- **Extension Update Latency**: Resolved package manager latency issues where updated extensions remained stuck in the updates pending list.

### Added
- **Database Background Vacuum**: Scheduled a WorkManager-based weekly Database Maintenance periodic task that safely vacuums and optimizes SQLite index fragmentation when the device is idle and charging, completely avoiding startup deadlocks.
- **Compose Skipping Performance**: Optimized the Library category layouts by mapping `displayedCategories` state to `ImmutableList`, preventing redundant recompositions during scrolling.
- **Enforced Core Safeguards**: Integrated a dependency-free directory package consistency scanner unit test to programmatically verify frozen zone paths.

## 2.5.0 "Stability & Refinement" (2026-05-27)
### Added
- **Unified Settings Hub:** Consolidated all ShinKu-specific features and SY-legacy preferences into a single "ShinKu Settings" screen for easier navigation.
- **Enhanced Immersion:** Integrated Mood Lighting, Backdrop Blur, and Haptic Feedback controls directly into the settings.
- **Tracker Sync Broadcast:** Progress updates, status, and scores are now broadcasted in parallel to all linked tracking services (AniList, MyAnimeList, etc.).
- **Smart Tracker Inheritance:** Progress is automatically inherited when binding new trackers to existing manga.

### Improved
- **Search UX:** Refactored Global Search with a responsive toolbar, integrated progress indicators, and scroll-aware chips.
- **Source Health Intelligence:** Implemented intelligent throttling during library updates based on real-time source health scores (latency/failure rates) without blocking browsing.
- **Extension Safeguards:** Established a 'Freeze Zone' for legacy packages to protect APK extension compatibility during architectural shifts.
- **UI Performance:** Leveraged Compose `@Immutable` annotations and `ImmutableList` to further reduce unnecessary recompositions in Feed and Browse screens.

## 2.4.0 "Fluidity & Power" (2026-04-20)
### Added
- **Dynamic Theming:** Immersive reader and details UI that adapts its color palette to the current manga cover.
- **On-Device AI Engine:** Integrated MediaPipe Universal Sentence Encoder for privacy-first, local text embeddings.
- **Similar Vibes:** New carousel in Manga Info to discover similar titles in your library based on AI "vibe" similarity.
- **AI Categorizer:** Experimental feature to automatically group your library into theme-based categories using K-Means clustering.
- **Source Health Monitoring:** Real-time reliability indicators (Green/Yellow/Red) in the source list.
- **Multi-threaded Downloader:** High-performance engine that splits single images into parallel chunks for faster downloads.
- **Resumable Downloads:** Granular byte-offset tracking in SQLite for resuming interrupted downloads.
- **Gesture Preview:** Visual playground in Reader Settings to map and test your tap zones.
- **Auto Webtoon Detection:** Smart switching to long-strip mode based on page aspect ratio analysis.

### Improved
- **Stability:** Patched critical `IllegalStateException` on hardware-backed bitmaps during color extraction.
- **Security:** Resolved `SecurityException` during notification channel management.
- **Infrastructure:** Upgraded project to **Min SDK 24** to support modern ML and networking tasks.

## 2.3.2 "Performance Refinement" (2026-04-06)
### Fixed
- **Startup Stability:** Resolved a critical race condition where logging was attempted before `XLog` initialization.
- **Database Optimization:** Added missing indexes to `mangas_categories` and `merged` tables to resolve "automatic index" performance warnings during joins.

### Improved
- **Network Performance:** Increased default network cache from 5MB to 100MB to significantly reduce redundant image re-downloads.
- **Image Loading:** Optimized Coil's memory cache (increased to 40% RAM) and fine-tuned parallelism for smoother page transitions on multi-core devices.
- **Startup Speed:** Offloaded `WidgetManager` initialization to a background thread to prevent blocking the main UI thread during application launch.

### Maintenance
- **Build Cleanup:** Completely removed the discontinued `standard` build flavor to improve R8 stability and reduce project complexity.

## 2.3.1 "Spring Stability" (2026-04-04)
### Fixed
- **Database Stability:** Implemented `busy_timeout` to resolve "database is locked" crashes during high-concurrency tasks.
- **Security & Performance:** Updated SQLCipher to v4.14.1 for improved encrypted database efficiency.
- **Source Reliability:** Migrated NHentai to the V2 JSON API for more stable and faster metadata retrieval.
- **Sync Integrity:** Fixed "Ghost Chapters" issue where deleted chapters could reappear after backup restoration or sync.
- **Reader Robustness:** Improved "Retry" button logic to force a fresh fetch of image URLs on failure.

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
