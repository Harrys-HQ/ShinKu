# Implementation Plan: Stability & Performance 2026

## Overview
This plan addresses a critical race condition during application startup and optimizes database performance by adding missing indexes as identified in the crash logs.

## Phases

### Phase 1: Stability Fix (XLog initialization)
The current implementation of `App.kt` calls `initExpensiveComponents(this)` before `setupExhLogging()`. Since `initExpensiveComponents` starts a background coroutine that initializes the `SourceManager` (which uses `xLogD`), a race condition occurs leading to an `IllegalStateException` because `XLog` is not yet initialized.

- [x]  **XLog Initialization Fix:** In `app/src/main/java/com/shinku/reader/App.kt`, move `setupExhLogging()` before `initExpensiveComponents(this)`.

### Phase 2: Performance Fix (Database Indexing)
Crash logs show "automatic index" warnings for `chapters(manga_id)` and `mangas_categories(manga_id)`. While `chapters` has an index on `manga_id`, adding more explicit indexes or ensuring they are correctly applied is necessary. `mangas_categories` lacks an index on `category_id`.

- [x]  **Add Index to `chapters.sq`:** Verified existing index; confirmed `merged.sq` join was the likely culprit for the warning.
- [x]  **Add Index to `mangas_categories.sq`:** Added explicit index for `category_id`.
- [x]  **Add Index to `merged.sq`:** Added explicit index for `manga_id` to optimize joins.
- [x]  **Verify Index Usage:** Run a build to ensure SQLDelight generates the necessary migrations or updates the schema.

### Phase 3: Verification
- [x]  **Build Check:** Run `./gradlew assembleDevRelease`.
- [x]  **Runtime Check:** Ensure the application starts without crashing on `XLog`.
- [x]  **Performance Check:** Monitor database logs for any remaining "automatic index" warnings.
