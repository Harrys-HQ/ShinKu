# ShinKu Changelog
 
## 2.7.15 "Extension ABI Modernization, Batch Translation & Upstream Sync" (2026-10-26)
### Fixed
- **Extension ABI & Coroutines Modernization (Fixes #21)**: Resolved fatal `NoSuchMethodError: runBlockingK$default` and `toJavaInstant` exceptions caused by ABI desynchronization with modern Keiyoushi extensions by upgrading `kotlinx-coroutines` to `1.11.0`, raising `minSdk` to `26` (Android 8.0 Oreo) for native JVM `java.time.Instant` linking, and removing `,allowoptimization` in R8 proguard rules to preserve extension interface APIs.
- **Safe Extension Replacement Staging (Upstream Mihon)**: Staged extension file replacement into `.part` files prior to atomic renaming, preventing deletion or corruption of installed extensions if copying or installing fails.
- **Tab Badge Overflow Protection (Upstream Mihon)**: Replaced `Row` layout in `TabText` with custom constraint layout measurement offsetting, guaranteeing category pill badges stay visible and long tab labels truncate gracefully without clipping.

### Features & Reader Experience
- **Whole Chapter Batch Translation**: Read foreign-language manga seamlessly with on-demand background translation of entire chapters. Includes non-maximum suppression (NMS) bubble overlap prevention, bounding box collision merging, persistent per-chapter JSON disk caching, and an unobtrusive floating progress pill.
- **Unified Translation Overlay Control**: Synchronized the reader overlay visibility master switch across Webtoon and Paged viewers, allowing instant on-the-fly toggling from both the reader action menu and translation progress sheets.
- **Universal OpenAI-Compatible AI Engine**: Added support for OpenRouter, DeepSeek, Groq, and custom endpoints alongside Google Gemini and offline ML Kit, featuring client-side Zero-Leak data privacy guarantees.
- **Google Drive SAF Cloud Sync**: Integrated native Google Drive cloud folder synchronization via Android's Storage Access Framework, allowing seamless, privacy-first cross-device library backup and restoration without 3rd-party servers.
- **Manga Cover Source Badges**: Added an optional, configurable Source badge overlay for manga covers in the Library (available in Library > Filter > Display > Source).

### Fixed
- **Haptic Feedback Crash on Samsung & Android 14+ (Fixes #20)**: Added missing `android.permission.VIBRATE` to the manifest and wrapped vibrator service invocations in safety guards, resolving fatal `SecurityException` crashes when turning on haptic feedback in settings.
- **Reader Dialog Text Cropping & Overflow**: Resolved text clipping and broken word wraps on action buttons in the reader long-press page action sheet.
- **Empty Category Crash Guard (Upstream SY)**: Guarded against `IllegalArgumentException` when library category lists are empty.
- **Database Migration 3 Column Fix**: Fixed legacy column name (`manga_source`) so upgrades from older databases passing through migration 13 succeed without missing-column exceptions.
- **Merged Manga Deterministic Chapter Sorting**: Added `ORDER BY C.manga_id, C.source_order` to prevent random/shuffled merged chapter ordering.
- **Merged Manga Reader Downloads & Cache**: Resolved component manga sources in reader download-ahead loops instead of attempting to query un-downloadable virtual merged IDs.
- **Extension Installer Crash Guard (Upstream Mihon)**: Added `ActivityNotFoundException` handling when system package confirmation screens are disabled on custom ROMs.
- **Reader Dialog Selection State Reset**: Reset `useExtraPage` state properly when dismissing cover selection dialogs.

### Performance & Typesetting
- **Komakun-Adapted Smart Typesetting**: Integrated luminance and ink-edge bubble boundary probing (`detectBubbleBounds`) to strictly contain translation boxes inside speech balloons without covering comic art.
- **Proportional Reader Typesetting**: Formatted horizontal translated dialogue with balanced line breaking (`BREAK_STRATEGY_BALANCED`), hyphenation, and dynamic font auto-fitting matching scanlation standards.
- **On-Device & Cloud Live Translation**: Bundled offline Google ML Kit translation engine alongside Google Gemini Cloud AI for instant, free offline translation.
- **Merged Manga Library Query Optimization (Upstream SY)**: Replaced full-database chapter scans in `LibraryQuery` with direct merged table joins, significantly speeding up library loading times.
- **Security & Privacy (Upstream Mihon)**: Redacted `Authorization`, `Auth`, and `Cookie` headers in logcat, linking network header logging strictly to verbose logging preferences.
- **Tracker & Metadata Enhancements**: Added tracking status support for Kavita, Suwayomi, and Comick with automatic metadata autofill.

## 2.7.13 "Official Portal, Documentation & Full Brand Independence" (2026-10-21)
### Features & Documentation
- **Official Documentation Website**: Launched the official ShinKu portal and documentation website at [https://harrys-hq.github.io/ShinKu/](https://harrys-hq.github.io/ShinKu/) with VitePress, dark mode, responsive layout, and full-text local search.
- **In-App Help & Guide Migration**: Directed all in-app help, onboarding walkthroughs, Cloudflare troubleshooting banners, and error logs directly to ShinKu's official website.
- **Local Source Comprehensive Guide**: Added dedicated guide covering folder structure, CBZ/CBR/ZIP archive handling, custom covers, and ComicInfo.xml metadata.
- **Support & Sponsorship Integration**: Added direct Ko-fi support preference in the About screen and enabled native GitHub repository sidebar sponsorship.
- **Safe-Harbor Compliance**: Formalized DMCA and local-first privacy policy ensuring safe-harbor standing.

## 2.7.12 "Discover Feed Overhaul, Responsive UI & Upstream Performance Sync" (2026-10-21)
### Performance & Optimization
- **Browse > Feed Concurrency Overhaul**: Parallelized feed searches and source queries across `Dispatchers.IO.limitedParallelism(4)` to prevent single-source head-of-line blocking.
- **Concurrent Genre Highlights Fetching**: Refactored dynamic genre highlight queries from sequential execution to parallel async workers, reducing section loading times from ~15s down to ~2s.
- **Layout Stability & Zero-Jank Rendering**: Retained offline library fallback sections while network queries resolve, preventing layout collapse and abrupt content jumping.
- **Eliminated Redundant Startup Reloads**: Guarded initial state lifecycle so returning from manga details or switching tabs preserves scroll position and loaded cards instantly with 0ms lag.
- **Library Update Prioritization (Upstream Mihon)**: Prioritized sources with the largest entry count (`.sortedByDescending { it.size }`) to minimize background update time.

### UI & UX Polish
- **Hero Carousel Auto-Advance**: Added a subtle 6-second auto-advance loop to the featured hero banner that pauses automatically during user touch/drag interactions.
- **Responsive Multi-Device Layouts**: Scaled hero banners (`200dp` mobile / `240dp` tablet) and manga cards (`116dp` mobile / `128dp` tablet) dynamically based on screen width (`>= 600dp`), and adapted skeleton shimmer cards to fully populate wide tablet viewports.
- **"In Library" Visual Badges**: Integrated live database observation and in-library badges on Discover cards and hero banners with tactile haptic feedback.
- **Genre Filter UX & Expanded Aliases**: Added an inline dismiss/clear button on genre highlights, friendly empty states when no results are found, and expanded genre aliases (Isekai/Reincarnation, Sci-Fi/Science Fiction, Martial Arts/Wuxia, Comedy, Romance, etc.).

### Fixed
- **Cover Cache Reloading on Unfavorite (Upstream Mihon)**: Removed `coverLastModified` timestamp bump when unfavoriting entries, stopping Coil disk cache eviction and eliminating cover flickering/redownloading.
- **Extension Cold-Start Race Condition (Upstream SY)**: Prevented `installedExtensionsFlow` from returning empty lists on cold launch, ensuring Global Search and crash loggers never miss installed extensions.
- **Cloudflare Interactive Early Abort (Upstream SY)**: Added JavaScript interface listener for Cloudflare `interactiveBegin` events, unblocking background workers immediately rather than waiting for 30s timeouts.
- **Tracker ID Whitespace Trimming (Upstream SY)**: Added whitespace trimming before integer conversion on `id:` prefix searches in MyAnimeList.

## 2.7.11 "AI Upscaling Isolation, Sensor Optimization & Upstream Stability" (2026-10-19)
### Performance & Optimization
- **AI Upscaling Jitter & Thumbnail Isolation (Fixes #19)**: Resolved UI stutter and scrolling jitter across the Library and Browse screens when AI Image Upscaling is active. `UpscalingInterceptor` now strictly isolates execution to reader pages (< 1500px), prevents cover thumbnail interception, enforces dimension bounds, recycles intermediate bitmaps, and offloads heavy convolution to background worker threads.
- **Adaptive Night Read Sensor Throttling**: Throttled raw ambient light sensor events in `ReaderActivity` to only update when crossing discrete brightness thresholds, preventing continuous window attribute modification and ViewModel state churn.
- **Background Extension Initialization**: Offloaded extension discovery, manifest scanning, and classloader initialization off the main UI thread to `Dispatchers.IO`, coordinating safely with `AndroidSourceManager` to guarantee completely stutter-free cold starts.

### Fixed
- **Live Translation Engine Decoupling**: Fixed on-device translation engines (e.g., MLKit, AICore) requiring a Gemini cloud API key.
- **WebView Renderer Termination Guard**: Stripped dangerous and initiator-sensitive headers (specifically `Origin`) from WebView requests, preventing Chromium from killing the renderer process.
- **WebView Source Header Isolation**: Confined source headers exclusively to the origin host in WebView navigation, preventing leakage to external domains or redirects.
- **Download Cache Tracking for Stored Stubs**: Grouped multiple source IDs sharing directory names on disk and deduplicated chapter count queries, fixing missing download tracking when installed sources share folder names with stored stubs.

## 2.7.10 "Next.js RSC Stream Healing, Slug Self-Correction & Upstream Stability" (2026-10-16)
### Fixed
- **Next.js RSC Stream Page Healing (Drake Scans & Modern Multisrc)**: Resolved "No pages found" errors when opening chapters on Drake Scans and other Next.js-based extensions. Added an automatic `NextJsRscInterceptor` and fallback reader stream decoder to extract page URLs even if extension themes encounter early Next.js RSC reader header structures.
- **Title Slug Self-Correction & Poisoned Cache Healing (Fixes #18)**: Fixed "No pages found" on titles containing numeric formatting or punctuation (e.g. *Logging 10,000 Years into the Future*). Slugs now automatically strip commas, apostrophes, and punctuation before hyphenating, and poisoned cache entries automatically self-heal.
- **Memo Route Persistence Across Domain Boundaries**: Retained manga and chapter memo parameters through domain mapper conversions and DB updates, ensuring consistent routing across all app lifecycle events.
- **Extension Filter Crash Prevention**: Protected the browse source filter UI against crashes caused by malformed extension filter indices or unexpected tri-state values.
- **Comick Tracker Decimal Formatting**: Formatted read chapter progress with standard decimal notation to prevent sync failures on decimal chapter numbers.
- **Tracker First-Read Date Fix**: Eliminated erroneous timezone conversion when recording the first-read date on tracked series.
- **Paged Reader Split-Page Crash Prevention**: Fixed reader crashes when viewing split double-page spreads across chapter transitions by scoping split-page caches to specific chapter IDs.
- **Webtoon Reader Exit Crash Guard**: Added window attachment verification on delayed single-tap navigation events to prevent crashes when rapidly exiting the reader.
- **Cloudflare Challenge Navigation Fix**: Redirected non-GET Cloudflare challenges to root paths to prevent 404/405 errors during verification.

## 2.7.9 "Webtoon Strip Rendering, Comick Tracker & High-Performance Upstream Sync" (2026-10-13)
### Added
- **Webtoon Strip Rendering Engine**: Draws tall webtoon pages as stacked horizontal strips via `WebtoonStripImageView` and Coil `webtoonStrips`. Uses about 30% less memory on tall pages, completely prevents frame drop and stutter between pages, avoids giant OpenGL texture allocations, and automatically frees memory for offscreen pages exceeding 64 MB.
- **Comick Tracker Integration**: Added native support for the Comick tracking service (`TrackerManager.COMICK`) with PKCE SHA-256 OAuth authentication, reading status synchronization, score updates, and direct series lookup via `id:` search prefixes.
- **Load Ahead Only on Wi-Fi**: Added a new reader toggle in Reader Settings allowing users to conserve mobile data by restricting page preloads to adjacent pages when on cellular networks.
- **Next Chapter Preloading**: The reader now automatically preloads the upcoming chapter in the background once the active chapter finishes loading.
- **Unfavorite Manga Confirmation Dialog**: Prevents accidental unfavoriting on the Manga Screen by showing a confirmation prompt before removal from the library.

### Performance & Optimization
- **10× Statistics Speedup**: Accelerated opening the Statistics screen by up to 10× (e.g. from 4.1s to 0.4s) on large libraries by batching tracking queries into a single database operation (`GetTracksPerManga.await()`).
- **3× Chapter List Loading**: Reduced processing power and accelerated chapter list displays by up to 3× for high-volume series (~900+ chapters) on Manga and Updates screens using optimized bulk lookups for queued and downloaded chapters.

### Fixed
- **Download Cache Overwrite & Mutex Protection**: Eliminated startup download cache overwrite race conditions and protected cache serialization with atomic file writes and root directory mutex locks.
- **Deleted Chapter Download Cleanup**: Automatically cancels queued downloads if the source deletes or removes the corresponding chapters during library sync.
- **Disk Download Resume Indexing**: When resuming downloads, finished chapters discovered on disk are immediately indexed rather than re-downloaded.
- **Duplicate Download Queue Cleanup**: Finished chapter downloads now enqueue and delete duplicate chapter downloads in the reader.
- **Excluded Category Download Protection**: Preserved excluded category downloads from accidental deletion when marking chapters as read.
- **Corrupted Local Archive Isolation**: Local source now wraps individual chapter archive inspection in try-catches, gracefully skipping corrupted zip/cbz files instead of failing the entire chapter list.
- **Duplicate Library Search Query**: Added non-empty title constraints to prevent blank titles matching 100% of the library.
- **Local Source Global Search**: Local source is now properly included when performing global searches.

## 2.7.8 "Authentic Atmospheric Audio & Upstream Stability Fixes" (2026-10-09)
### Added
- **Authentic Full-Length Atmospheric Audio Engine**: Completely replaced synthetic equations with high-quality, authentic CC0 public domain recordings hosted remotely on [Harrys-HQ/ShinKu-Assets](https://github.com/Harrys-HQ/ShinKu-Assets). Features 8 tailored genre soundscapes (Action, Horror, Mystery, Cyberpunk, Historical/Wuxia, Rain, Forest, and Cafe) with 3–5 minute durations and 5-second seamless equal-power crossfading.
- **Atmospheric Audio Volume & Soundscape Controls**: Added volume % slider (0–100%) in both ShinKu Settings and Reader Settings, a manual "Soundscape Vibe" selector to override genre matching, "Download All Soundscapes" for complete offline readiness, "Clear Soundscape Cache", and a direct in-app link to the CC0 source attribution and license page.
- **Smart Genre Priority Matching**: Rebalanced audio genre detection to prioritize high-octane themes (Action, Martial Arts, Shounen) over background ambient tags, ensuring action series play fitting battle tension rather than calm forest ambience.

### Fixed
- **Library Range Selection Crash**: Fixed `IndexOutOfBoundsException` when multi-selecting manga across category boundaries by tracking the category and ID of the last selected item.
- **Reader Tap Navigation Touch Recovery**: Fixed reader becoming unresponsive to page taps after a button press turned into a scroll/drag gesture by re-enabling gesture detection on `MotionEvent.ACTION_CANCEL`.
- **Webtoon Window Resizing & Foldable Support**: Fixed blank gaps and viewport offsets after window resize or device unfolding by dynamically measuring the container when `layoutParams.height == MATCH_PARENT`.
- **WebView Background Thread Freeze**: Auto-dismisses JavaScript alerts, confirms, and prompts if the user navigates away from the WebView, preventing hangs during Cloudflare and Captcha verification.
- **Corrupted Cache Bypass on Reader Retry**: Reader page retries now force a clean network re-download (`PriorityPage.RETRY`) to immediately bypass damaged or incomplete cached files.
- **Shizuku Dead Service Unbind Crash**: Guarded Shizuku unbind calls with `Shizuku.pingBinder()` to prevent app crashes when Shizuku is force-closed or terminated by the OS.

## 2.7.7 "Keiyoushi Compatibility & Library Speed Optimization" (2026-10-05)
### Fixed
- **Keiyoushi KeiSource & VineTheme Chapter Loading**: Resolved `Attempt to invoke virtual method 'java.lang.Class java.lang.Object.getClass()' on a null object reference` error toast when opening or downloading chapters from Drake Scans and other modern Keiyoushi multisrc extensions (`VineTheme`) by ensuring chapter and manga routing metadata are consistently maintained and hydrated.
- **Persistent Memo Routing Cache**: Introduced persistent `MangaMemoCache` and persistent `ChapterMemoCache` with auto-synthesized fallback routing to guarantee that required routing parameters (`slug`, `number`, `isLocked`, and `id`) survive app restarts, deep links, and process termination without requiring breaking SQLite schema migrations.
- **Reader & Downloader Metadata Pre-Hydration**: Pre-hydrated chapter routing metadata across Reader WebView and Share actions, background library update workers, deep link ingestion, and ComicInfo XML generation during chapter downloads.
- **Memo Cache Memory & I/O Optimization (OOM & Socket Leak Fix)**: Converted `ChapterMemoCache` to a bounded, thread-safe in-memory LRU cache (`MAX_ENTRIES = 1000`) and automatically purged legacy 50MB SharedPreferences XML files, reducing Dalvik heap allocation from 513 MB to under 32 MB. Fixed OkHttp socket leaks by ensuring all responses are safely closed.
- **Restored Blazing Fast Library Update Speed**: Fixed library update slowdowns (>8 minutes) caused by artificial delays. Fully restored user-configured concurrency settings (`Boost` and `Extreme`) with standard 50ms burst spacing. Smart Throttling now engages dynamically only when a source is actively failing or degraded, and database statistics updates now run asynchronously without stalling the chapter sync pipeline.
- **Extension Compatibility (`JsonObject.get` NullPointerException Fix)**: Resolved `Attempt to invoke virtual method 'JsonObject.get(Object)' on a null object reference` error on Asura Scans, Mangabat, and other Keiyoushi extensions by ensuring `SManga.memo` and `SChapter.memo` getters never return null and pre-synthesizing required slugs and IDs.
- **Lateinit URL Initialization Fix**: Converted `url` and `name` properties in `SMangaImpl` and `SChapterImpl` from lateinit to safe default strings, completely preventing `UninitializedPropertyAccessException` crashes across Mangafreak and modern extensions.
- **Update Error Log Sharing**: Added a dedicated **"Share"** action directly to the Library Update error notification, enabling one-tap export of `shinku_update_errors.txt` to the public Downloads folder, Google Drive, or messaging apps.
- **Startup WorkManager & Database Optimization**: Eliminated background worker storm on app open by changing periodic WorkManager policies to `KEEP` and removing unnecessary worker resets in `App.onCreate()`. Added `LIMIT` parameter to history queries to prevent full database table scans on app startup.

## 2.7.6 "MangaDex Image Resolution Hotfix" (2026-10-04)
### Fixed
- **MangaDex Chapter Loading & Downloads**: Resolved `Unable to resolve host: cmdxd98sb0x3yprd.mangadex.networkhttps` error when opening or downloading MangaDex chapters by removing improper `baseUrl` prepending on relative page paths. Standard sources continue to resolve relative URLs through `HttpSource.imageRequest` while sources with dynamic CDN nodes retain clean relative paths.
- **Corrupted Disk Cache Auto-Recovery**: Added automatic detection and invalidation of corrupted legacy disk cache entries for MangaDex pages, allowing previously failed chapters to load immediately without requiring a manual cache clear.

## 2.7.5 "Search Concurrency & Thread Optimization" (2026-10-04)
### Fixed
- **Browse & Search OS Thread Leaks**: Replaced raw `Executors.newFixedThreadPool` with coroutines `Dispatchers.IO.limitedParallelism` across `SearchScreenModel`, `FeedScreenModel`, and `SourceFeedScreenModel`. Eliminates silent thread leaks where multiple unclosed OS threads accumulated on every search and feed screen navigation.
- **Search Item Loading State Race Condition**: Fixed a state-update race condition in `SearchScreenModel` where concurrent source completions evaluated items outside of atomic updates, causing parallel search results to overwrite each other and leave sources stuck in the "Loading" state indefinitely.
- **TreeMap Key Collision on Duplicate Sources**: Added unique source ID tie-breakers to `sortComparator` in `SearchScreenModel` and `MigrateSearchScreenModel` to prevent `TreeMap` from colliding and overwriting sources that share the same name and language.
- **Reader Error Layout Polish**: Disabled futile retry triggers on permanent HTTP errors (404 Not Found and 410 Gone) in both Pager and Webtoon reader viewers, and reduced error container height in Webtoon viewer to prevent layout disruption.
- **Graceful Unresolvable Image URL Handling**: Hardened `HttpPageLoader` to safely catch `UnsupportedOperationException` when extensions fail to resolve individual page URLs and automatically fall back to refreshing the complete page list.
- **Redundant Nullability & Type Conversions**: Cleaned up unnecessary non-null assertions and redundant type conversions in `MangaCoverFetcher`, `Downloader`, and `ExtensionInstallReceiver`.

## 2.7.4 "Reader Resilience & AI Modernization" (2026-09-30)
### Added
- **Gemini 3.x AI Modernization & Testing**: Upgraded Gemini AI model offerings with direct support for the latest model families (`gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`). Added a real-time **"Test Model"** diagnostic action directly inside AI Settings to verify API key validity, connectivity, and response latency.
- **Dynamic Preload Buffer Adaptation**: The reader preload queue now automatically tunes its buffer depth according to user reading velocity—fast readers get larger buffers (up to 30 pages) to eliminate loading pauses, while slower readers conserve memory and network bandwidth.
- **AI Translation Page Caching**: Added dedicated in-memory and disk caching for translated pages, allowing instantaneous re-renders without re-querying AI translation engines.
- **Source Health Re-Test Action**: Added an on-demand "Re-test" button to the Source Health screen to easily probe and verify individual source connectivity in real time.

### Fixed
- **Reader Page Retry Crash (`IllegalArgumentException: Expected URL scheme`)**: Fixed an issue where tapping "Retry" on failed pages in modern extensions (like MangaFreak) wiped `page.imageUrl` to null. Because these sources omit `page.url`, subsequent retries failed with empty OkHttp URL scheme exceptions. `retryPage()` now preserves image URLs for standard sources, and missing URLs trigger an automatic self-healing page list refresh from the source.
- **Universal URL Scheme Normalization**: Hardened `HttpSource`, `Requests.kt`, `DataSaver`, `Downloader`, and Coil image fetchers to automatically normalize protocol-relative (`//...`) and domain-relative (`/...`) image and cover URLs to absolute `https:` URLs using the source's base URL.
- **Chapter Cache Poisoning Prevention**: Prevented storing corrupted page lists (`imageUrl = null`, `url = ""`) into `ChapterCache`, and added automatic detection to discard invalid cached lists and refresh from the network upon chapter opening.
- **Source Health False "Stable" Ratings**: Fixed Source Health diagnostics falsely flagging sources as "Stable" by ensuring health probes execute with the extension's authentic User-Agent and headers.
- **Milestones Filter Scrolling & Layout Polish**: Resolved horizontal scroll clipping on Milestones filter chips and fixed text boundary constraints on badge cards.

### Changed
- **Adaptive 429/503 Backoff & Jitter**: Implemented exponential backoff with randomized jitter on HTTP 429 (Rate Limit) and 503 (Cloudflare) responses to prevent IP bans during aggressive loading.
- **Viewport Page Priority Elevation**: Active reading pages in the viewport are now assigned top priority in the loading queue over background preload tasks.
- **Dependency Alignments**: Updated AndroidX WorkManager to 2.12.0 and Koin to 4.2.2 for enhanced background execution stability.

## 2.7.3 "Gamified Milestones & Completionist Hub" (2026-09-27)
### Added
- **Expanded Gamified Milestones (53 Badges across 7 Categories)**: Completely expanded the Reading Journey milestone system from 12 to 53 comprehensive achievements spanning Bronze 🥉, Silver 🥈, Gold 🥇, Platinum 💎, and Mythic 👑 tiers across Reading Time, Chapter Conquest, Daily Streaks, Library Collection & Completion, Genre Mastery, Reading Habits, and Tracker Records.
- **Completionist Milestones Hub**: Added dynamic Reader Level progression (`🎖️ Level X Reader`) based on unlocked achievements, with live percentage progress tracking.
- **Status & Category Filter Chips**: Added quick filters for `All (53)`, `Unlocked (X)`, and `In Progress (Y)`—automatically sorted by closest-to-unlock—plus horizontal chips to filter by achievement category.
- **Locked Milestone Progress Tracking**: Locked milestones now display dimmed outlines with subtle lock indicators 🔒 and real-time numeric progress bars (e.g. `584h / 1,000h`, `3 / 7 days`).
- **Milestone Detail Inspector**: Tapping any milestone badge opens a dialog displaying its tier, unlock criteria, exact progress, and a dedicated **"💡 Completionist Tip"**.
- **Milestones Roadmap & Guide**: Added an interactive guide dialog (via header `?` icon) explaining reader levels, tier hierarchy, and progression tips.

## 2.7.2 "Immersive Floating Dock & Density Polish" (2026-09-26)
### Added
- **Edge-to-Edge Floating Glass Navigation Dock**: Transformed the bottom navigation bar into a true floating dynamic island dock. Background content flows edge-to-edge behind the dock with transparent margins outside the squircle, and list paddings ensure the last items are never clipped.
- **Auto-Hide Navigation Dock on Scroll**: Bottom dock now smoothly collapses out of the way on scroll down with a refined slide-and-fade animation, instantly reclaiming vertical space for title browsing and reading discovery.
- **Scroll-Collapsible Jump-Back Deck**: The "Continue Reading" Jump-Back deck in the Library tab now automatically collapses vertically when scrolling down through manga cards, while category tabs stay intact, pinned, and fully accessible at the top.
- **ShinKu Preference Toggle for Jump-Back Deck**: Added a dedicated setting under **Settings > ShinKu > Library** (`Show 'Continue Reading' Jump-Back Deck`) to toggle the deck for users who want maximum screen density.
- **Auto-Hide Dock Toggle**: Added a setting under **Settings > ShinKu > Navigation** to configure whether the bottom navigation dock auto-hides on scroll.

### Fixed
- **Suggested Titles Process Restoration**: Hardened `SuggestedTitlesScreen` against Android process death and state restoration by caching and restoring title IDs directly.

## 2.7.1 "Feed Stability & Linkage Hardening" (2026-09-16)
### Fixed
- **Discover Feed Crash (`NoSuchMethodError: toJavaInstant`)**: Fixed a fatal crash when accessing Modern Feed caused by extensions calling desugared `kotlin.time.jdk8.InstantConversionsJDK8Kt` methods on Android 12+.
- **Linkage Error Resilience**: Hardened exception boundaries across all feed carousels, genre sections, global search, and source paging (`BaseSourcePagingSource`) to safely intercept `Throwable` / `LinkageError` thrown by outdated or incompatible third-party extensions without aborting the app.
- **R8 ProGuard Compatibility**: Updated ProGuard keep rules to protect `kotlin.time` and `kotlin.time.jdk8` against member inlining and signature rewriting, ensuring ABI stability with dynamically loaded extension classloaders.

## 2.7.0 "Discover Feed & Squircle Unification" (2026-09-16)
### Added
- **Modern Discover Feed**: Redesigned the Feed tab with a modern Discover interface featuring a dynamic Featured Hero Carousel, interactive Genre Navigation Cloud, personalized "Titles For You", and curated Genre Highlights.
- **Interactive Skeleton & Instant Preload**: Added 0ms instant local library cache preloading and an interactive Discover Feed Skeleton (`DiscoverFeedSkeleton`), eliminating startup lag and preventing premature empty screen flashes while online sources compile.
- **Feed Source & Language Filtering**: Added a new Feed Filter & Settings dialog accessible from the top bar to toggle source feeds visibility, filter by specific source, or filter by language.
- **Offline & Empty State Resilience**: Gracefully clusters library titles by genre when offline or when no extensions return results, ensuring the Feed is always rich and interactive.
- **Suggested Titles Drilldown Screen**: Added a dedicated screen (`SuggestedTitlesScreen`) with full grid browsing for see-all actions on any feed section or genre.

### Changed
- **Squircle Unification Pass**: Completely eliminated lingering `CircleShape` usages across the app (dialogs, library indicators, hero cards, browse tabs, calendar days) in favor of cohesive `RoundedCornerShape` squircles.
- **AI Model Resolution & Zero Latency**: Updated Gemini API model mapping to execute official endpoints directly (`gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`) on the first attempt, eliminating HTTP 404 retry latency penalties.
- **Live Translation Hardening**: Added lenient JSON parsing and regex fallback parsers to handle relaxed or code-fenced translation responses seamlessly.
- **Privacy & Help Documentation**: Updated Privacy Policy and Help settings to route directly to Mihon documentation.

## 2.6.9 "Upstream Hardening & Obtainium Support" (2026-09-14)
### Added
- **Obtainium Package Auto-Discovery**: Added standard Fastlane metadata (`fastlane/metadata/android/en-US/package_name.txt`) with application ID `com.shinku.reader` so Obtainium, IzzyOnDroid, and other FOSS package managers detect and update ShinKu seamlessly without App ID errors.
- **Upstream Version Diagnostics**: Added upstream tracking (`SY 1.13.2 / Mihon 0.20.4`) into `BuildConfig` and crash log export for streamlined triage between ShinKu-specific logic and upstream behavior.

### Fixed
- **AniList Rate Limit Tightening**: Lowered API query permits from 85/min to 25/min in accordance with AniList's new rate-limit guidelines, preventing HTTP 429 errors during tracker sync.
- **Dependencies Bump**: Updated JUnit to 6.1.2.

## 2.6.8 "Upstream Sync & Anti-Bot Spoofing" (2026-09-07)
### Added
- **Anti-Bot Spoofing & Client Hints (`Sec-CH-UA`)**: Added `androidx.webkit` metadata spoofing to synchronize Client Hints with the HTTP User-Agent (bumped to Chrome 149), resolving Cloudflare Turnstile and Datadome verification loops.
- **Resumable Chapter Image Downloads**: Added support for HTTP 206 Partial Content downloads and byte ranges; interrupted downloads now resume from their existing progress rather than redownloading from scratch.
- **Automated Upstream Checker**: Added a new PowerShell automation tool (`scripts/check-upstream-updates.ps1`) to query and categorize live releases, pull requests, and commits from Mihon and TachiyomiSY upstreams.

### Fixed
- **Backup Category Restore Race Condition**: Ensured category entities finish restoring before manga entries and app preferences are inserted, preventing missing category assignments upon backup restoration.
- **AniList Token Expiration**: Fixed a timestamp multiplier bug that prevented token refresh detection, and added a proactive 1-minute expiration buffer.
- **Reader Page Slider Lock**: Added a release listener to the reader page navigator slider to ensure navigation bars and system status bars restore their proper state after page seeking.
- **Shizuku Service Initialization**: Added ProGuard keep rules for rootless installer interface and broadened permission detection across custom environments.
- **Update Check on Rotation**: Guarded startup update checks to prevent redundant API queries when rotating the screen.
- **Async Database Migration Execution**: Fixed cursor contention and deadlock potential when applying category sorting migrations.

## 2.6.7 "AI Resilience & Vision Performance" (2026-08-30)
### Added
- **Multi-Model Rate-Limit Fallback**: Implemented automatic failover across Gemini model tiers (`preferredModel` ➔ `gemini-2.5-flash` ➔ `gemini-1.5-flash`) on HTTP 429 (quota exhaustion), 404, or 503 errors.
- **AI Query & Translation Caching**: Added LRU in-memory response caches for Vibe Searches, Similar Manga recommendations, chapter story recaps, and live reader translation blocks.
- **MLKit Vision OCR Caching**: Added generation/content-based caching for image OCR text recognition, making repeated page translation views instantaneous.

## 2.6.6 "Protobuf Extension Repo Migration & Safeguards" (2026-08-30)
### Added
- **Protobuf Extension Catalog (`index.pb`)**: Added native decoding and gzip decompression for modern Protocol Buffer extension catalogs (`index.pb`), fully resolving the orphaned extensions issue caused by Keiyoushi deprecating legacy `index.min.json`.
- **Dual-Format Fallback**: Automatically tries `index.pb` first, then seamlessly falls back to `index.min.json` and `index.json` for legacy or self-hosted repositories.
- **Dynamic APK & Icon Resolution**: Added support for direct and relative `apkUrl` and `iconUrl` schemas in extension catalogs.
- **Tokyo Night Theme**: Added a new Tokyo Night color scheme option under Appearance settings.
- **Vibe Search Improvements**: Refined AI engine integration with Gemini for semantic vibe search.

### Fixed
- **Flexible Repo URL Normalization**: Repository addition now accepts base URLs and automatically strips redundant suffixes (`/index.min.json`, `/index.json`, `/index.pb`, `/repo.json`, or trailing slashes).
- **Automatic Orphan State Recovery**: Installed extensions automatically clear obsolete/orphaned status upon successful repository synchronization.

## 2.6.5 "AsuraScans & Library Refresh Fix" (2026-07-18)
### Fixed
- **Chapter Memo Caching:** Resolved `Refresh Chapter List` error toast when opening chapters on extensions relying on `SChapter.memo` (such as AsuraScans) by dynamically caching and restoring the memo field using a normalized URL and suffix matcher.
- **Library Refresh Deadlock:** Resolved a deadlock issue where library refresh would freeze at 2% by replacing the global source-wide update lock with a fine-grained, per-manga lock mapping.

## 2.6.4 "Upstream Synchronization & CDN Fix" (2026-07-12)
### Fixed
- **Reader Page Retry Crash:** Fixed a reader crash where clicking "Retry" on page load failures threw an `UnsupportedOperationException: null` for standard sources (like Mangabat) by only clearing image URLs on EHentai-based sources.

### Changed
- **Upstream Dependency Sync:** Updated SQLDelight to `2.3.2`, activity-compose to `1.13.0`, moko-resources to `0.26.1`, and Okio to `3.17.0` to keep in sync with modern upstream Mihon features.
- **Automatic Download Cache Invalidation:** Automated invalidation of local download directory cache references upon completing database restoration to prevent broken local links.

## 2.6.3 "Live Translation Overhaul" (2026-06-26)
### Added
- **Multi-Language Translation Settings**: Added an on-the-fly, dual-dropdown translation settings dialog to the reader (accessible via FAB long-press). Allows readers to explicitly set both the source language (with a robust Auto-Detect default) and the target language, as well as instantly swap languages.
- **Global Default Preferences**: Added global default controls for translation source and target languages under ShinKu Settings.

### Fixed
- **Speech Bubble Text Merging**: Implemented vertical and horizontal layout proximity-aware line clustering in `TextRecognitionInteractor.kt`, preventing speech segmentation and reading order errors.
- **OCR Deduplication**: Replaced overlapping text resolution with a quality-based length check to preserve long translated paragraphs while removing single-character OCR noise.
- **Smart Padding**: Inflated balloon bounding boxes by 15% to ensure clean background color sampling, preventing color conflicts with black text glyphs.
- **FAB Gestures**: Resolved click listener interception, ensuring both single-taps (toggling translation) and long-presses work seamlessly.

## 2.6.2 "Extension Compatibility Update" (2026-06-25)
### Fixed
- **Extension Interface Compatibility**: Restored full compatibility with all compiled extensions (such as AllManga) by delegating coroutine methods in `HttpSource` to their legacy RxJava counterparts using `awaitSingle()`. This resolves runtime errors such as `UnsupportedOperationException: null` when opening titles or loading chapters.

## 2.6.1 "Extension Updates Fix & Resource Optimization" (2026-06-20)
### Fixed
- **Extension Update Latency**: Resolved package manager latency where updated extensions remained stuck in the updates pending list.
- **Deduplicated Extension Repositories**: Deduplicated available extensions by package name in ExtensionApi to resolve version code mismatches between multiple repositories.

### Optimized
- **Resource Footprint**: Converted logo, splash, and source icons from PNG to lossless WebP, optimizing APK size.

## 2.6.0 "Manga Reader Perfection" (2026-06-01)
### Added
- **Immersive Reader (Phase 1-4)**:
    - **Atmospheric Audio**: Dynamic, genre-matching ambient sounds for total immersion.
    - **Adaptive Palette**: The reader UI now seamlessly transitions colors based on page content.
    - **Light Adaptive Night-Read**: Automatically adjusts contrast and brightness for low-light environments.
    - **AI Story Recaps**: Gemini-powered summaries of previous chapters to keep you up to speed.
    - **Unsharp Sharpening Upscale**: High-fidelity on-device upscaling for crisp lines and text.
    - **ShinKu Reading Wrapped**: Beautiful, shareable year-in-review and reading velocity stats.
    - **E-Ink Hardware Profile**: Specialized optimization for E-Ink displays, including instant transitions and auto-monochrome.
    - **Volume Keys Override**: Precise control mapping for physical volume buttons in the reader.

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
