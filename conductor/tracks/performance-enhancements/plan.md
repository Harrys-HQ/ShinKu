# Performance Enhancements

## Metadata
- **Track ID**: `performance-enhancements`
- **Status**: `In Progress`
- **Objective**: Improve the application's overall speed, responsiveness, and startup time through targeted optimizations, ensuring no regressions by adding tests and rigorous verification at each phase.

## Current State
The ShinKu application is fully functional but has areas where performance can be optimized. Key areas identified include app startup sequence, Jetpack Compose UI rendering (stability), database indexing, and dependency consolidation (DI and RxJava).

## Target State
A noticeably faster, more responsive application. Startup time is minimized through lazy initialization and background thread offloading. UI interactions are smoother due to reduced recompositions and expanded baseline profiles. Database queries are optimized, and legacy dependencies are removed or consolidated to reduce overhead.

## Verification Strategy
- **Unit and Integration Tests:** Must be written or updated for every refactored component.
- **Macrobenchmark Tests:** Baseline profiles will be expanded and verified using the `macrobenchmark` module.
- **Manual Verification:** Build and run the `devDebug` variant after each phase to ensure core functionality (library loading, reading manga, settings) remains intact.

## Phases

### Phase 1: Establish Performance Baseline & Tooling Setup
- [ ] **Task 1.1:** Setup Jetpack Compose Stability Configuration file to ensure stable external models.
- [ ] **Task 1.2:** Expand Macrobenchmark coverage in `BaselineProfileGenerator.kt` to cover more user journeys (Library scroll, Manga Details, Reader open).
- [ ] **Task 1.3:** Run baseline benchmarks to establish current performance metrics.
- [ ] **Verification:** Ensure `devDebug` builds successfully and benchmarks output valid data.

### Phase 2: Startup Optimizations
- [ ] **Task 2.1:** Analyze `App.onCreate` and identify components that can be initialized lazily or on a background thread.
- [ ] **Task 2.2:** Refactor the `getPackageName()` spoofing mechanism to avoid expensive stack trace lookups if possible, or cache the result.
- [ ] **Verification:** Verify app startup time improves or remains stable via benchmarks. Verify app functions normally after fresh install.

### Phase 3: Database & IO Improvements
- [ ] **Task 3.1:** Analyze SQLDelight files (`data/src/main/sqldelight/**/*.sq`) and add composite indices for frequently executed, multi-column queries (e.g., `(url, source)` in `mangas.sq`).
- [ ] **Task 3.2:** Review logging mechanisms (XLog, FilePrinter) to ensure verbose logging is strictly opt-in and doesn't cause IO bottlenecks.
- [ ] **Verification:** Run existing unit tests for the database. Manually verify library loading speed with a large number of items.

### Phase 4: Dependency Modernization (Ongoing)
- [ ] **Task 4.1:** Identify components using `Injekt` and begin migrating them to `Koin` to consolidate DI containers.
- [ ] **Task 4.2:** Identify remaining usages of `RxJava 1.x` and refactor them to Kotlin Coroutines/Flow.
- [ ] **Verification:** Run all unit tests. Ensure features utilizing refactored components function correctly.

### Phase 5: Final Review and Polish
- [ ] **Task 5.1:** Run full suite of Macrobenchmarks and compare against the baseline established in Phase 1.
- [ ] **Task 5.2:** Perform extensive manual testing of the entire application.
- [ ] **Task 5.3:** Finalize documentation and close the track.
