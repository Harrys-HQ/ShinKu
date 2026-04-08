# Track: Performance & Maintenance 2026

## Status
- [x] Increase Network Cache from 5MB to 100MB
- [x] Optimize Coil Image Loader (Parallelism & Cache)
- [x] Offload Widget Initialization from Main Thread
- [x] Remove discontinued `standard` build flavor
- [x] Verify build stability with `assembleDevRelease`

## Description
Further performance optimizations for networking, image loading, and startup speed, along with cleanup of legacy build configurations.

## Files
- [Implementation Plan](./plan.md)
- [NetworkHelper.kt](../../../core/common/src/main/kotlin/eu/kanade/tachiyomi/network/NetworkHelper.kt)
- [App.kt](../../../app/src/main/java/com/shinku/reader/App.kt)
- [build.gradle.kts](../../../app/build.gradle.kts)
