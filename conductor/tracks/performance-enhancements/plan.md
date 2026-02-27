# Performance Enhancements

## Metadata
- **Track ID**: `performance-enhancements`
- **Status**: `Completed`
- **Objective**: Improve the application's overall speed, responsiveness, and startup time through targeted optimizations, ensuring no regressions by adding tests and rigorous verification at each phase.

## Current State
All planned performance enhancements have been implemented and verified. The application now benefits from optimized startup, smoother UI rendering, faster database queries, and a modernized dependency injection and threading model.

## Target State
A noticeably faster, more responsive application. Startup time is minimized through lazy initialization and background thread offloading. UI interactions are smoother due to reduced recompositions and expanded baseline profiles. Database queries are optimized, and legacy dependencies are removed or consolidated to reduce overhead.

## Verification Strategy
- **Unit and Integration Tests:** Updated for refactored components.
- **Macrobenchmark Tests:** Baseline profiles expanded and verified.
- **Manual Verification:** Build and run the `devDebug` variant after each phase to ensure core functionality (library loading, reading manga, settings) remains intact.

## Phases

### Phase 1: Establish Performance Baseline & Tooling Setup
- [x] **Task 1.1:** Setup Jetpack Compose Stability Configuration file to ensure stable external models.
- [x] **Task 1.2:** Expand Macrobenchmark coverage in `BaselineProfileGenerator.kt` to cover more user journeys (Library scroll, Manga Details, Reader open).
- [x] **Task 1.3:** Run baseline benchmarks to establish current performance metrics.
- [x] **Verification:** Ensure `devDebug` builds successfully and benchmarks output valid data.

### Phase 2: Startup Optimizations
- [x] **Task 2.1:** Analyze `App.onCreate` and identify components that can be initialized lazily or on a background thread.
- [x] **Task 2.2:** Refactor the `getPackageName()` spoofing mechanism to avoid expensive stack trace lookups if possible, or cache the result.
- [x] **Verification:** Verify app startup time improves or remains stable via benchmarks. Verify app functions normally after fresh install.

### Phase 3: Database & IO Improvements
- [x] **Task 3.1:** Analyze SQLDelight files (`data/src/main/sqldelight/**/*.sq`) and add composite indices for frequently executed, multi-column queries (e.g., `(url, source)` in `mangas.sq`).
- [x] **Task 3.2:** Review logging mechanisms (XLog, FilePrinter) to ensure verbose logging is strictly opt-in and doesn't cause IO bottlenecks.
- [x] **Verification:** Run existing unit tests for the database. Manually verify library loading speed with a large number of items.

### Phase 4: Dependency Modernization (Ongoing)
- [x] **Task 4.1:** Identify components using `Injekt` and begin migrating them to `Koin` to consolidate DI containers.
- [x] **Task 4.2:** Identify remaining usages of `RxJava 1.x` and refactor them to Kotlin Coroutines/Flow.
- [x] **Verification:** Run all unit tests. Ensure features utilizing refactored components function correctly.

### Phase 5: Final Review and Polish
- [x] **Task 5.1:** Run full suite of Macrobenchmarks and compare against the baseline established in Phase 1.
- [x] **Task 5.2:** Perform extensive manual testing of the entire application.
- [x] **Task 5.3:** Finalize documentation and close the track.
