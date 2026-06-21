# Implementation Plan: UI Performance & Resource Optimization

## Phase 1: Compose Stability Audit
- [x] **Generate Compiler Metrics:** Run `./gradlew assembleDevRelease -Pandroidx.compose.compiler.plugins.kotlin.metricsDestination=...`
- [x] **Fix Unstable Models:** Updated `FeedItemUI` and `SourceFeedUI` to use `ImmutableList` and `@Immutable`.
- [x] **Optimize Key Screens:** Audited and optimized `LibraryTab` categories state, converting it and `LibraryContent` / `LibraryTabs` to utilize performance-optimized `ImmutableList<Category>`.

## Phase 2: Resource Pruning
- [x] **Unused Resource Analysis:** Audited `res/drawable` for legacy Tachiyomi/Mihon remnants.
- [x] **Icon Optimization:** Convert remaining PNG icons to WebP or Vector Drawables where possible.

## Phase 3: Database & Network Tuning
- [x] **Implement DB Vacuum:** Safely scheduled weekly Database Maintenance / VACUUM periodic task via background WorkManager.
- [x] **Brotli Support:** Verified Brotli compression is enabled in `NetworkHelper`.

## Phase 4: Verification
- [ ] **Performance Benchmarking:** Attempted to run `macrobenchmark` but requires physical device/connected emulator.
- [x] **Build Check:** Successfully ran `./gradlew assembleDevRelease`.
- [x] **Static Verification:** Verified stability improvements via Compose Compiler Reports.
