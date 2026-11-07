# Remaining code optimization review

Reviewed 2026-11-03 against the current working tree. The main Discover limiter is now six overall/three per source, and release optimization has been verified. This follow-up identifies remaining work; no behavior was changed or new runtime benchmark performed for these findings.

## Recommended order

### 1. Translation cache correctness and I/O

**Confirmed code path:** `ReaderViewModel.retranslateCurrentPage()` around line 2023 calls `TranslationCache.removePage()`, which only removes the memory entry (`ReaderPage.kt:67`). It then calls `onTranslation()`. That method reads `TranslationCache.get()` around line 1598; a memory miss reloads the still-present disk entry. A saved translation can therefore be restored instead of fresh OCR/translation running. This was traced in source, not reproduced on-device in this audit.

**Confirmed work amplification:** `TranslationDiskCache.getPage()` decodes the entire chapter file; `putPage()` reads and rewrites the full chapter. Disk misses across many pages and repeated writes do unnecessary serialization. `onTranslation()` is called directly from reader UI callbacks and performs its cache lookup before entering `launchIO`, so a cold disk lookup can execute on the UI event path. Chapter clearing is also synchronous.

**Concurrency risk:** writes are direct `File.writeText()` calls with no per-chapter synchronization or atomic replacement. Overlapping read/modify/write operations can lose updates; interrupted writes can leave invalid cache JSON. These risks have not been reproduced in this review.

**Change:** give explicit retranslation a force-refresh path that bypasses/removes both cache layers; put disk operations on an explicit I/O path; serialize updates per chapter; use atomic replacement; coalesce writes or store page records. Define cache identity/invalidation for page-image changes and translation configuration, rather than silently discarding paid results whenever settings change. Atomic replacement and synchronization are distinct: [Android AtomicFile documentation](https://developer.android.com/reference/android/util/AtomicFile) explicitly states that it does not supply file locking.

**Verify:** actual OCR/engine invocation on retranslate; persistence across restart; two overlapping writes; corrupt/interrupted cache handling; language/configuration changes; long chapters; cancellation; UI frame timing on a cold cache.

### 2. Suggested Titles pagination and request ownership

`SuggestedTitlesScreenModel.kt` still uses `limitedParallelism(4)` and catches `Throwable` around source calls without preserving cancellation. The main feed's new request policy does not cover this separate screen.

Around lines 191–216, the model derives `hasMore` from whether the aggregated page contains new unique manga. Source failures are converted to empty lists, and duplicate-only pages also produce no unique entries. Either can set `hasMore = false` even when another page or a retry could provide more titles. Conversely, a nonempty final page sets `hasMore = true` because the source's `MangasPage.hasNextPage` information was discarded. A shared page number can also advance past a failed source when another source succeeded.

**Change:** retain per-source cursors/end-of-results/failure state, provide retries for failures, and distinguish duplicate-only pages from an actual end. Apply compatible cancellation/permit ownership, with an explicit decision about whether limits are shared with a still-active feed instance. Do not mechanically create an unrelated six/three budget in each screen and call it an app-wide cap.

**Verify:** one failed source plus one healthy source; duplicate-only page followed by fresh results; final nonempty page with `hasNextPage = false`; repeat load taps; leaving the screen while loading; retention of initial recommendations.

### 3. Lightweight recommendation inputs and progressive results

`FeedScreenModel.kt:597` calls the complete `GetReadingStats.await()` report during each refresh to obtain `bestGenres`. `GetReadingStats.kt:29,68–69` reads all history, all manga and the history-linked chapters, then calculates streaks, charts, genre/author statistics and other analytics. This is more work than a genre preference lookup needs. It is not an N+1 query in this method: manga and chapter data are already fetched in batches, but those batches and computations are broad.

The top feed also waits for all popular/latest/behavior results before publishing its combined network state around lines 338–340. A slow source can delay healthy network results; local-library fallback may already be visible, so do not describe this as always a blank screen.

**Change:** expose a lightweight history/genre summary with invalidation on relevant history/metadata changes. Reuse or cache expensive analytics independently. Publish usable source sections progressively while retaining refresh ownership and stable deduplication/order.

**Verify:** query counts and allocation size on a large history/library; genre-summary freshness; first visible network-backed content time; time until all sections settle; slow/failing sources; cancelled refreshes. The earlier source-only harness explicitly excluded this local computation and UI rendering.

### 4. Library query batching

`LibraryScreenModel.kt:171` fetches manga and all chapters separately for up to five resume-deck titles. The merged download-badge path around line 743 uses a blocking query per merged manga, and tracker grouping around line 1408 fetches tracks through another blocking bridge even though track data is also subscribed elsewhere.

**Change:** retrieve the needed manga/current chapter rows together, batch merged counts, reuse subscribed tracker data, and remove blocking bridges from suspending processing. This flow already runs on IO; it is not a confirmed main-thread freeze. [Kotlin documents that runBlocking blocks its calling thread](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html), regardless of whether the work inside uses an IO dispatcher.

**Verify:** large/merged libraries, download badge changes, category switches, tracker grouping, resume after a chapter disappears, query counts and end-to-end library presentation time.

### 5. Cover-update budgets

`LibraryUpdateJob.kt:628` still selects `Int.MAX_VALUE` for both source and manga permits in Extreme cover updates. Chapter updates already have finite limits; the two paths should not be conflated. The cover worker also catches cancellation as an ordinary source error.

**Change:** use measured finite cover budgets and preserve cancellation. Treat source/host rate limiting and backoff separately from concurrency; a semaphore alone is not a requests-per-second guarantee.

**Verify:** large cover refresh, simultaneous reading/downloads, cancellation, rate-limited sources and actual completion time. Do not copy the Discover limits blindly into this different workload.

### 6. Incremental AI embedding work and model recovery

`MangaEmbeddingJob.kt:68` embeds the full library every run without checking stored metadata versions first. It saves `lastMetadataUpdate` but does not use it to skip unchanged records in this job. The native embedder is closed only after the normal loop, rather than in a guaranteed cleanup path.

The model is downloaded directly to the final filename around line 114; later runs only test whether that file exists. An interrupted download can leave a partial model that future retries attempt to load instead of downloading again. This is a code-level recovery risk, not a reproduced incident.

**Change:** embed only changed/missing items, track model/text-input version, ensure native cleanup on failure/cancellation, and publish a validated model download atomically. Keep the existing scheduled-work constraints: charging, unmetered network and idle. These constraints already reduce the urgency of speculative battery claims.

**Verify:** unchanged library does no inference; metadata/model changes re-embed correctly; cancellation closes resources; interrupted model download recovers on retry; concurrent library changes do not corrupt embeddings.

### 7. Profile image/reader memory before changing budgets

Coil allows a cache ceiling of 40% of the app heap, with 32 fetch execution slots and 12 decoder slots (`App.kt:260–270`). Reader images, OCR snapshots and scaling can add additional allocations. This is a capacity configuration, not proof of a leak, actual 40% allocation, or an OOM.

**Change only after measurement:** capture peak/retained heap and frame timings during library scrolling → chapter open → area OCR → chapter translation → return to library, on representative phone memory classes. Then tune cache, bitmap lifetime, decode and preload budgets to those observations.

## Work to start while mockup generation is paused

Prioritize translation force-refresh/cache safety and Suggested Titles pagination correctness, followed by lightweight feed statistics and library batching. Then measure cover and bitmap budgets. These code changes do not require the pending image mockups, and they should be validated independently from the visual redesign.

This was source inspection plus primary documentation checks. No fixes, unit tests, builds, phone mutations or image-generation calls were performed in this follow-up review.


## Implementation status — 2026-11-03

The owner authorized this implementation batch. Translation now persists independent pages using atomic replacement, migrates legacy chapter files, serializes cache operations, and explicitly invalidates both layers for retranslation. UI cache lookups were moved off event paths or changed to memory-only reads; overlays carry their target language. Saved output is retained across engine/model preference changes until explicit retranslation/clear; automatic image-content invalidation remains a follow-up because page URLs may be temporary.

Suggested Titles now retains per-source cursors and hasNextPage flags, retries errors without skipping the failed page, advances duplicate-only pages with a bounded automatic loop and manual continuation, preserves cancellation, and shares the six/three request limiter with Discover. Feed genre input uses a grouped SQL query instead of full analytics, and top sections publish progressively. Library resume input and merged lookups are batched, and grouping reuses subscribed tracker data. Query additions do not alter table schemas, so no migration is required.

Extreme cover updates now have a finite candidate budget (20 sources, 2 manga per source); this has not yet been timed on a representative large cover workload. Embeddings use stored metadata plus model/input fingerprints, skip unchanged input, stage and validate model downloads, and close the native embedder in a finally block. The first run with the new fingerprint index may re-embed existing records once. Image cache budgets were left unchanged; installed-app profiling is documented separately in reader-memory-device-profile.md.

All 26 focused JVM regression tests passed (7 translation persistence/concurrency tests, 3 pagination model tests, 10 request/cancellation tests, 1 fingerprint test, 5 existing extension compatibility tests). These do not validate native model inference or OCR/UI behavior on the changed APK. Device checks will happen after the owner returns. Release build/signature status is recorded in PROJECT_MEMORY.md when complete.

Local build completion: assembleDevRelease succeeded on 2026-11-03; all five newly assembled APK signatures verified. SHA-256 records are in build/optimization-apk-checksums.json. Device validation is deferred at the owner's request.
