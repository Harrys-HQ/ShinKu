# ShinKu overall review and improvement backlog

Review date: 2026-11-02. Source baseline: `bd94670c13`.

## Follow-up implementation

The owner authorized starting Discover request limits/cancellation and release configuration. The initial implementation added four overall/two per-source request permits to the main Discover screen, replaceable refresh and genre jobs, cancellation-aware state publication, and one latest-only saved-feed loader. Release/releaseTest optimization and release lint are enabled. Dev debug compilation, 15 focused regression/extension-compatibility tests, and the prescribed optimized release assembly passed. Release lint found no issues. All five APKs were generated, and inspection confirmed the six tested compatibility classes remain in their DEX files. The subsequent adjustment to six/three is described below. No full-UI performance improvement or release publication is claimed. The findings below describe the original review baseline; the remaining optimization and UI/UX items are pending.

A [source-only device comparison on 2026-11-03](./discover-device-benchmark.md) subsequently found median source-completion times of 1.56 s for the old scheduling, 2.00 s for four/two, and 1.32 s for a six/three candidate in a fixed twelve-call workload. The owner approved six/three, now adopted in the limiter defaults. Actual full-feed UI timing has not been measured.

## Overall assessment

ShinKu has a recognizable product identity and a strong core use case. The resume deck puts reading first; discovery gives the library a useful entry point; reading journey adds personality. The inherited extension, offline reader, tracker, and backup ecosystem provides substantial depth.

The next investment should be consolidation: predictable loading and cancellation, safe translation persistence, production build optimization, and clearer everyday controls. Additional features will have less value if settings become harder to navigate or source failures appear as unexplained empty shelves.

This is a first-pass architecture/code review with a visual review of stored library, Discover, manga-detail, and More screenshots in `.github/readme-images/`. Screenshots can lag current code. Runtime responsiveness, accessibility, tablet behavior, battery impact, and crash frequency remain unmeasured. Findings below distinguish code behavior from risks and design proposals.

## Highest-priority engineering work

An [additional code audit](./code-optimization-follow-up.md) traces retranslation cache invalidation, Suggested Titles pagination, full-statistics recomputation during refresh, and embedding/model-download work. It also revalidates the cache, query, cover-budget and memory opportunities below. These remain pending implementation; no additional speedup is claimed.

### 1. Enforce the intended release configuration

**Confirmed:** `app/build.gradle.kts:90` defaults `enableMinify` to false; both code minification and resource shrinking depend on it. Release lint at line 159 also defaults off. Neither inspected CI workflow supplies these properties. Consequently the documented release assembly command does not itself guarantee the optimization required by `AGENTS.md`.

**Change:** Make intended production defaults explicit and add a check that catches accidental regressions. Validate extension loading, serialization, backups, and DI with the optimized APK before shipping; these compatibility paths make a blind R8 toggle inappropriate.

**Success:** The prescribed release command produces optimized artifacts, all five ABI outputs are accounted for, compatibility smoke checks pass, and APK size/startup measurements are recorded. Android documents these two flags as the controls for [release optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).

### 2. Give Discover real request budgets and refresh ownership

**Confirmed:** `FeedScreenModel.kt:91` uses `Dispatchers.IO.limitedParallelism(4)` as its stated rate-limit protection. Suspended source requests can outnumber four: Kotlin explicitly distinguishes [thread parallelism from coroutine concurrency](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-dispatcher/limited-parallelism.html). Featured loading fans out popular/latest/genre calls, then genre rows and AI recommendations add work. `init(force)` starts another job without keeping a handle to cancel the previous refresh. Numerous `catch (Throwable)` branches turn failures, including cancellation, into empty results.

**Risk:** Refreshes can overlap, consume unnecessary requests, and allow old results to replace newer results. This is supported by the control flow; actual 429 frequency is unknown.

**Change:** Use shared global and per-source semaphores held across each request, preserve cancellation, retain/cancel the refresh job and its children, and reject results from superseded refreshes. Publish each useful section when ready rather than holding all top-section results until the slowest completes. Preserve existing library fallbacks, health ranking, and eight-second source timeouts.

**Success:** Controlled tests prove maximum active request counts, cancelled refreshes cannot publish stale results, and one broken source does not prevent healthy results from appearing.

### 3. Bound Extreme cover updates

**Confirmed:** `LibraryUpdateJob.kt:628` uses `Int.MAX_VALUE` for both source and manga limits in `updateCovers()`. Chapter updates elsewhere already use finite semaphore limits.

**Risk:** Large cover refreshes can create a request burst with excessive resource use or source throttling. This is a missing bound, not proof that all such requests execute simultaneously on every source.

**Change:** Keep a finite global/per-source budget even in Extreme mode. Reduce concurrency or back off after repeated rate-limit responses.

**Success:** A large-library cover refresh stays within its configured request budget and remains cancellable.

### 4. Make translation persistence safe and cheaper

**Confirmed:** `TranslationDiskCache.kt:118` reads/decodes the full chapter to get one page. `putPage()` at line 144 reads the chapter and rewrites it through `saveChapter()` / `File.writeText()` without a per-chapter lock or atomic replacement. A chapter with many translated pages therefore incurs repeated full-file serialization. `ReaderPage.kt` exposes this through `TranslationCache`. Cache identity is chapter plus target language; engine/model/source language/page identity are absent.

**Risk:** Overlapping read-modify-write calls can lose a page update; interrupted writes can leave invalid JSON. Changing translation configuration or source images can reuse stale blocks. These are code-level risks, not reproduced user-data-loss incidents.

**Change:** Put cache I/O behind a repository with explicit I/O dispatching, per-key serialization and atomic replacement; consider page records or coalesced chapter writes. Version the key/schema and define when configuration changes should invalidate results. Add a size/age policy. Keep reading available if a corrupt cache is discarded.

**Success:** Tests cover two overlapping page writes, interrupted/corrupt files, invalidation, restart persistence, and chapter-size scaling. Existing `TranslationCacheTest.kt` primarily exercises model conversion/serialization rather than concurrent disk persistence.

## Next performance opportunities

| Area | Evidence and implication | Proposed work |
| --- | --- | --- |
| Library processing | `LibraryScreenModel.kt:743` blocks for merged download counts; line 1408 blocks while fetching tracks. Main processing already runs on IO. | Replace blocking bridges with suspending/batched queries and reuse the subscribed track map. Measure large-library/category-switch latency before and after. |
| Resume deck | Recent-history flow around line 171 separately fetches manga and all chapters for up to five entries. | Fetch the required manga/chapter rows together or retrieve only needed chapters. Verify progress remains correct when the historical chapter disappears. |
| Image memory | `App.kt:260` allows Coil up to 40% of app heap, with 32 fetch/12 decode execution slots. OCR and reader bitmaps add separate pressure. | Profile combined library/reader/OCR sessions on a low-memory device. Then adjust cache/decode/prefetch budgets by device class. A cache ceiling alone does not prove a leak. |
| Upscaling | `UpscalingInterceptor.kt:58` uses matrix scaling followed by a sharpening filter. It already uses rolling row buffers. | Preserve the existing buffer optimization; measure peak bitmap memory and processing time. Call it enhanced scaling/sharpening unless a neural model is actually introduced. |
| Measurement | Macrobenchmarks cover cold/warm/hot startup with different compilation modes; the profile generator visits tabs/settings. | Extend to realistic seeded library scrolling, Discover loading, chapter open/page turns and translation. Add frame timing and record device, dataset, build flags and thermal state. Regenerate profiles after relevant changes. |

## UI/UX opportunities

### Keep the Sanctuary identity; improve hierarchy

The rounded surfaces, restrained accent color and cover art work together well in the stored screenshots. Preserve that vocabulary. The main usability issue is competing information and controls within constrained space.

1. **Library:** Keep Resume prominent, but offer a compact deck state and a clear title/details action. Stored screenshots truncate both resume and cover titles. A list/detailed display option and reliable full-title access would help readers distinguish similar series. Clarify unread badges through accessible labels.
2. **Discover:** The hero consumes much of the initial viewport. Try a shorter hero and earlier access to search/recommendations. Add explanations such as “Because you read…” and distinguish library fallback content from fresh discoveries. The screenshot truncates “Extensions”; evaluate tab sizing or a simpler route to source management.
3. **Manga details:** The screenshot truncates the recommendations action and shows a vague “Soon” status. Prefer wrapping/shorter action labels and a specific status or explanation. Keep Resume easy to reach while verifying it does not block the final chapters/download actions.
4. **More / journey:** The stored card clips reading duration as `623h 5...`. Format summaries for the available width and expose full values. Keep essential utilities close enough that a large profile card does not make settings difficult to reach; consider a compact card option.
5. **Floating navigation:** In screenshots, content extends behind the dock. This can be intentional, but verify the final item and every action can scroll fully clear of it, with gesture and three-button navigation. Check auto-hide behavior, selection mode, screen-reader focus and large text on-device before treating overlap as a confirmed layout defect.

### Simplify ShinKu settings

`SettingsShinKuSettingsScreen.kt:85` assembles twelve groups covering maintenance, AI, translation, performance, immersion, navigation, interface, browsing, library, security/debug, and advanced tools on one page.

Keep one ShinKu parent destination and introduce focused subpages: AI & Translation, Reading & Appearance, Performance, and Maintenance. Put developer tools deeper. Preserve settings search and deep highlighting. Prefer task-based wording, localized strings, and one place to configure each capability.

For AI setup, show the selected provider's connection status, capability availability, a test action, and actionable error messages. Discovery and translation should explain what is sent to the configured service and whether a fallback is being used. No provider availability, price, or model quality was evaluated in this review.

### Improve onboarding and resilient states

Current onboarding has theme, storage, permissions and guides/backup restore. Add optional next actions after completion for restoring a library, setting up sources, or exploring AI. Keep plain reading usable without AI configuration.

Discover currently has a skeleton, a generic empty state, and several silent failure fallbacks. Distinguish no sources, no matching titles, offline content, authentication/Cloudflare problems, and AI configuration errors. Offer the relevant retry or setup action without blocking other sections.

Accessibility acceptance should include font scaling, TalkBack labels and focus, non-color status cues, contrast checks for dynamic themes, reduced motion, and E-Ink/low-power behavior. Android recommends [at least 48dp touch targets](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views); source inspection and screenshots cannot establish compliance.

## Architecture, quality and release hygiene

- The module split and existing compatibility safeguards are valuable. Preserve the extension ABI, backup identifiers and deep links during optimization.
- Avoid a wholesale rewrite. Extract translation orchestration/cache and Discover loading into testable services as those areas change; `ReaderViewModel.kt` currently combines many responsibilities.
- `docs/architecture.md` is too simple in two places: DI is bridged into Koin and preferences use SharedPreferences. Correct these when maintaining architecture documentation.
- API keys use ordinary preferences. A targeted credential-store review could consider Android Keystore-backed protection and migration. Existing app-backup credential exclusion and disabled OS backup are positive safeguards; do not report an established backup key leak.
- PR CI runs formatting plus debug assembly; release CI includes unit-test tasks. Add focused unit tests to PR CI for request cancellation/budgets, cache persistence, and compatibility. Verify test task discovery before relying on it as a gate.
- The release workflow triggers on the `release` branch and creates a draft release, while the project mandate also requires a push to `main` and a published five-APK release. Document their relationship before the next release.
- App version is `2.8.1-preview.1` / 143; docs metadata is stable `2.8.0` / 142. Confirm preview policy and synchronize the required files for the intended release. No version bump is proposed as part of this review.
- README's “instant 0ms preloading” is not a measured guarantee. Use language that distinguishes cached rendering from cold network loading.

## Suggested sequence

1. **Reliability and production baseline:** release flags/compatibility checks, Discover request budgets/cancellation, bounded cover refresh, translation-cache safety.
2. **Everyday usability:** settings subpages, long labels/stats, source and AI failure states, navigation clearance and accessibility checks.
3. **Measured optimization:** representative benchmarks, library query batching, image/translation memory budgets and refreshed baseline profiles.
4. **Discovery polish:** smaller hero variants, recommendation explanations and relevance feedback after the loading foundation is reliable.

Start with Discover request ownership and budgets for a contained user-visible improvement; run the release configuration audit alongside it. Choose the next task explicitly before changing app behavior.

## UI/UX implementation order after request tuning

This is the proposed UI/UX scope discussed with the owner. The UI tasks below have not been implemented as part of request-limit tuning.

| Batch | Scope | Acceptance |
| --- | --- | --- |
| 1. Everyday readability and reachability | Long resume/cover titles, clipped manga action labels, reading-duration formatting, and content clearance around floating navigation/Resume controls. | Full information remains accessible; final rows and their actions can scroll clear of overlays. Check narrow phone layouts, larger fonts, gesture/three-button navigation, and screen-reader labels. Preserve Sanctuary styling. |
| 2. ShinKu settings organization | One ShinKu parent with focused AI & Translation, Reading & Appearance, Performance, and Maintenance pages. Keep developer tools deeper. | Existing preference keys/values survive; settings search, result highlighting, back navigation, and localization work across subpages. |
| 3. Discover clarity | Clear loading/partial results, empty/offline/source-error states and relevant retries; readable source-management tabs; truthful recommendation explanations. Evaluate a compact hero only after core usability fixes. | Healthy content stays usable when another source fails. Readers can understand what loaded, what needs retrying, and why a recommendation appears. Keep existing suggestion counts and source/genre controls. |
| 4. AI setup feedback | Selected-provider connection/capability status, connection test, clear translation/discovery fallback state, and actionable errors. | Users can tell whether the selected service is configured and usable without a failed reading session. Verify configured/unconfigured/failing paths. |

Start with batch 1, then review its actual phone presentation before expanding the design work. Broader visual changes should use concrete previews and the existing Sanctuary components/tokens.

Batch 1 source entry points are `presentation/library/components/HeroJumpBackCard.kt`, `presentation/manga/components/MangaInfoButtons.kt` and `MangaVibesAndRecap.kt`, `presentation/more/components/CommandProfileHeader.kt`, and `ui/home/HomeScreen.kt` under the app's `com/shinku/reader` package, plus the shared `presentation-core/.../components/material/NavigationBar.kt`. These are inspection starting points; a `maxLines = 1` declaration by itself does not establish a usability bug.

## Whole-app visual coverage pack

The owner requested the remaining screens/states be planned before a coordinated UI implementation pass. The [review gallery](./design/mockups/index.html) and [coverage checklist](./design/mockups/coverage.md) map 24 additional boards / 72 planned panels, including inherited specialist flows and widgets. A source CSV maps 229 named entry points and shared templates; this does not equal 229 distinct rendered screens.

Rendering is incomplete: boards 01–03 are generated and the remaining 21 have saved exact prompts but no images because the built-in generator reached its usage limit. Its estimated reset is November 4, 2026, 00:55:48 Asia/Shanghai. The gallery labels pending boards explicitly. The two earlier concept directions are liked; no new app UI has been implemented or runtime/accessibility-certified. Review generated sample data and navigation notes before using any board as an implementation reference.

## Validation performed

Read the project protocols, module/build configuration, workflows, selected startup/DI/library/feed/reader/cache/settings/onboarding/network/backup code, existing test/benchmark sources, release metadata, and four repository screenshots. Checked technical guidance against Android and Kotlin primary documentation.

No build, tests, benchmark, emulator/device session, production release inspection, or accessibility scan was run. No app code, version, release, remote branch, or issue was changed. Remaining work requires implementation and runtime verification; this review does not certify production readiness.
