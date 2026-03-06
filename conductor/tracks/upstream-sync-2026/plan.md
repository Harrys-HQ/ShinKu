# Implementation Plan: Upstream Sync 2026

## Overview
This plan breaks down the synchronization process from Mihon and TachiyomiSY into manageable phases to ensure stability and reduce the risk of regressions.

## Phases

### Phase 1: Database Performance
- [x] Add `history.last_read` index to improve history view loading performance.
- [x] Review `mangas` and `chapters` tables for any other missing indexes that were added upstream (e.g. composite indexes).

### Phase 2: Migration Logic Improvements
- [x] Update `MigrateMangaUseCase.kt` to copy the `lastPageRead` property during migration.
- [x] Ensure source order is preserved correctly during migration.

### Phase 3: Dependency Updates
- [x] Update `Kotlin` to `2.3.10` in `gradle/libs.versions.toml`.
- [x] Update `Compose BOM` to `2026.02.00`.
- [x] Update `Paging` to `3.4.1`.
- [x] Update `Coil` to `3.4.0`.
- [x] Test project compilation and resolve any new lint or compile errors.

### Phase 4: Stability & Bug Fixes
- [x] Implement Cloudflare Guard blank page fix in `CloudflareInterceptor.kt`.
- [x] Fix extension installer "Pending" state issue in `ExtensionInstaller.kt`.
- [x] Port Mihon's Thread Starvation fix (#2955) by reviewing coroutine dispatchers and Coil fetcher configurations.

### Phase 5: New Features
- [x] Add Library Duplicate Detection utilizing trackers.
- [x] Add "Bookmarked Chapters" download option.
- [x] Add VPN Support for automatic library updates.

## Current Status
**Active Phase:** Completed
**Status:** All upstream sync tasks for February 2026 have been successfully implemented, verified with a full build, and are ready for release.

