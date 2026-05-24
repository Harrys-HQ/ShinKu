# Implementation Plan: UI Performance & Resource Optimization

## Phase 1: Compose Stability Audit
- [x] **Generate Compiler Metrics:** Run `./gradlew assembleDevRelease -Pandroidx.compose.compiler.plugins.kotlin.metricsDestination=...`
- [x] **Fix Unstable Models:** Updated `FeedItemUI` and `SourceFeedUI` to use `ImmutableList` and `@Immutable`.
- [ ] **Optimize Key Screens:** Audit `LibraryScreen` and `MangaScreen` for unnecessary recompositions using the Layout Inspector.

## Phase 2: Resource Pruning
- [x] **Unused Resource Analysis:** Audited `res/drawable` for legacy Tachiyomi/Mihon remnants.
- [ ] **Icon Optimization:** Convert remaining PNG icons to WebP or Vector Drawables where possible.

## Phase 3: Database & Network Tuning
- [ ] **Implement DB Vacuum:** Reverted for stability; caused startup hang/deadlock with Migrator.
- [x] **Brotli Support:** Verified Brotli compression is enabled in `NetworkHelper`.

## Phase 4: Verification
- [ ] **Performance Benchmarking:** Attempted to run `macrobenchmark` but requires physical device/connected emulator.
- [x] **Build Check:** Successfully ran `./gradlew assembleDevRelease`.
- [x] **Static Verification:** Verified stability improvements via Compose Compiler Reports.
