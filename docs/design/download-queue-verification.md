# Download queue state and controlled verification

Date: 2026-11-04. Scope: finish the daily-screen queue checks before moving to the next mockup group.

## State correction

The header previously observed the WorkManager scheduling flow. The downloader itself runs a separate coroutine job, so those states can disagree. A worker that is no longer reported running does not establish that the engine has stopped. The earlier phone screenshot showed this discrepancy but did not establish its scheduling trigger or duration.

`DownloadRunState` now owns the engine job and exposes its running state. Start, completion and cancellation update that state; a completed cancellation from an older job cannot clear the state of a replacement. `DownloadManager.isDownloaderRunning` exposes this engine state to the queue and More screen. The queue model uses the existing current value directly instead of creating another flow initialized to false.

WorkManager scheduling, source requests, download queue persistence and reading-history behavior are unchanged. The worker's separate running helpers still serve scheduling decisions. This pass does not claim to repair every possible background-worker lifecycle issue.

The controlled UI run also exposed a sort-menu defect: selecting a child sort option closed only its submenu, leaving the parent menu over the queue. All four queue sort choices now dismiss both menus after applying the order.

## Regression coverage

- JVM tests cover natural completion, repeated starts, delayed cancellation of a replaced job, and an already canceled owner. The full dev JVM suite passed: 48 discovered, 47 passed, one existing skip, zero failures/errors. Log: `build/queue-state-tests.log`.
- `DownloadQueueTest` is a device suite restricted by an explicit `.debug` application-ID guard. It also refuses input when the active window belongs to another package. It seeds synthetic chapter objects and generates valid tiny PNGs with Android's encoder, held behind a cancellable gate so downloads cannot finish before the assertions. Files stay in the debug app's private cache; no personal reading records or real source downloads are used.
- The first device case checks an active engine with no active WorkManager worker, then verifies Pause all and Resume all. The second covers button-driven resume/pause, chapter and upload-date sorting, drag ordering, per-item cancellation, completion/empty state and unchanged history count.
- The initial connected-device command compiled but could not run because the phone disconnected before installation (`No connected devices`). This is an environment failure, not a reproduced regression failure. No pre-fix device failure has been demonstrated by this new suite.
- An existing Medium_Phone emulator was then started headlessly with `-read-only -no-snapshot`; its temporary disk overlay was discarded on exit. It ran Android 15 / API 35. Both queue scenarios and both Android geometry tests passed together: **4 tests, zero failures**, `OK (4 tests)`. Log: `build/queue-emulator-final-tests.log`; assertion progress: `build/queue-emulator-verification.log`.
- The first emulator sequence exposed the sort menu remaining open; the later sequence passed after the parent dismissal fix. The test waits for sorted RecyclerView rows and animations before dragging, and selects row controls by visible position. Earlier gesture assertions used moving rows and were corrected in the test harness; no production drag-order algorithm change was needed.
- The full queue scenario checked that the history entry count was unchanged. The emulator runtime sample contained zero fatal-exception, fatal-signal or OOM markers. Log: `build/queue-emulator-runtime.log`. No physical-phone, optimized-APK runtime, Android 17, extension-network, background-rescheduling or frame-performance coverage is implied by this Android 15 debug-app test.
- Scoped formatting was applied to implementation/test Kotlin files. Logs: `build/queue-state-format.log` and `build/queue-final-format.log`. No repository-wide formatter claim.

## Build and device status

Debug app and test APK assembly passed. The controlled emulator queue cases have now passed, superseding the earlier pending interaction checks in the daily-device report. The prescribed `assembleDevRelease --build-cache --parallel --quiet --console=plain` completed successfully; all five APK signatures verified. Build log: `build/queue-state-release.log`; signature log: `build/queue-state-signatures.log`; SHA-256 and artifact timestamps: `build/queue-state-checksums.json`.

The final ADB check found no connected devices, so this preview is ready locally and has not been installed on the phone. Version remains 2.8.1-preview.1/143; use the new checksum record to distinguish it from the earlier installed preview. Phone text size, rotation, reading records and download files were not changed in this pass. No version bump, push or public release was performed.
