# Implementation Plan: Performance & Maintenance 2026

## Overview
This plan focuses on high-impact performance optimizations for networking and image loading, while also performing necessary maintenance by removing legacy build configurations.

## Phases

### Phase 1: Networking & Image Loading
- [x] **Increase Network Cache:** In `core/common/src/main/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt`, increase `maxSize` from 5MB to 100MB to reduce redundant image re-downloads.
- [x] **Optimize Coil Cache:** In `app/src/main/java/com/shinku/reader/App.kt`, increase `maxSizePercent` for `MemoryCache` to `0.4` (40%) to keep more manga pages in RAM.
- [x] **Fine-tune Coil Parallelism:** Adjust `fetcherCoroutineContext` and `decoderCoroutineContext` to better utilize modern multi-core devices.

### Phase 2: Startup Performance
- [x] **Asynchronous Widget Init:** In `app/src/main/java/com/shinku/reader/App.kt`, move `WidgetManager(...).apply { init(scope) }` inside the `scope.launch(Dispatchers.IO)` block to prevent blocking the main thread during startup.

### Phase 3: Build Maintenance
- [x] **Remove `standard` Flavor:** In `app/build.gradle.kts`, remove the `standard` product flavor as it is discontinued and causes R8 instability. Update any related logic (e.g., `androidComponents` filter).

### Phase 4: Verification
- [x] **Build Check:** Run `./gradlew assembleDevRelease`.
- [x] **Runtime Verification:** Ensure widgets still update correctly and image loading feels snappier.
