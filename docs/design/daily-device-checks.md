# Daily screens device checks

Later work: [numbered mockups 01–08 and glass-toolbar verification](mockups-01-08-implementation.md) records the broader UI pass, ten passing Android emulator checks and a newly verified optimized build. That newer preview awaits phone installation; the observations below describe the earlier installed daily-screen build.

Date: 2026-11-04. Device: CPH2691, Android 17. Tested the optimized ARM64 daily-screen preview, version `2.8.1-preview.1` / 143. Its SHA-256 matched `build/daily-mockup-checksums.json` before installation with `adb install -r`.

## Verified

| Check | Result |
| --- | --- |
| Installation and launch | Passed; existing app data retained. |
| Updates cards | Portrait covers, wrapped long titles/chapter labels, distinct Read/Resume labels and page-position text rendered on the current dark palette. |
| Updates selection | Long-press selected one entry; checkbox selected a second; counter changed to two. Dock and reading actions were replaced by selection controls. Back exited selection. |
| History cards/menu | Wrapped titles and Resume buttons rendered. Overflow opened the removal confirmation; Back canceled it without deleting history. |
| End-of-list clearance | Fast-scrolled both Updates and History to the end. Final reading actions could be exposed above the dock; scroll-driven dock hiding/reappearance was observed. |
| Landscape | Updates rendered with readable title, chapter/page text and actions. Portrait was restored immediately afterward. This is a sampled layout check, not full landscape coverage of all screens. |
| Download queue | Empty state and two-entry source-grouped queue rendered. Resume initiated downloads; page progress updated, completed entries left the queue, and the empty state returned. |
| Test data cleanup | Both temporary downloads were removed through the existing selected-chapter confirmation. Their Updates indicators returned to undownloaded. No history-removal or mark-read action was confirmed. |
| Android geometry tests | Both `TranslationGeometryTest` cases passed on the real device, zero failures/errors. |
| Runtime log sample | Zero fatal-exception, ANR, fatal-signal or OOM markers in the captured production-process log. |

## Test setup correction

The initial connected test run executed zero tests: `AndroidJUnitRunner` was declared but missing from the test APK, causing `ClassNotFoundException` before tests could start. Added the catalog entry `androidx.test:runner:1.7.0` and `androidTestImplementation(libs.test.runner)`. This matches the explicit runner dependency documented in the [AndroidX Test setup](https://developer.android.com/jetpack/androidx/releases/test#declaring_dependencies). The fix affects test dependencies; production source code was unchanged during these checks.

Rerun command: `:app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shinku.reader.data.cache.TranslationGeometryTest --no-daemon --console=plain`.

Logs: `build/daily-review-instrumentation.log` (initial setup failure) and `build/daily-review-instrumentation-fixed.log` (two passing tests). Connected-test XML reports zero failures/errors. The prior full JVM result remains 43 passed and one existing skip; it was not needlessly rerun for a test-only dependency fix.

## Follow-up findings and limits

Update: the queue state/menu corrections and controlled Android 15 emulator checks are recorded in [download queue verification](download-queue-verification.md). Both queue scenarios passed, covering the previously pending interaction sequence without personal app data. The historical observations below describe the earlier phone build; the new optimized preview still needs physical-phone installation/review.

- A queue screenshot showed “Downloads paused”/“Resume all” alongside an updated page counter while the test downloads were completing. This is a status discrepancy requiring a controlled reproduction; the underlying cause and duration are not established. The header uses the existing WorkManager-derived running flow. Do not claim pause/resume transitions fully passed or infer an engine failure from this screenshot alone.
- Drag reordering, sort interactions and per-item cancellation were not fully exercised before the two test downloads completed. They remain pending controlled queue checks.
- A temporary 150% text-size change recreated the activity and returned to Library. That screenshot is Library, despite the historical filename `history-font150.png`; it is not evidence of History at large text. The owner requested the original size, so further large-text checks were stopped and font scale was restored to `1.0`. Light-theme, TalkBack and broad accessibility checks remain pending.
- UIAutomator repeatedly failed to obtain an idle hierarchy. The helper refused stale XML. Device results above use direct screenshots and coordinate actions mapped to their actual 1080 × 2376 dimensions, rather than stale node data. No automated UI assertion suite or frame-time benchmark is claimed.

Raw screenshots and runtime logs contain private app content and remain under ignored `build/device-benchmark/daily-review/`; they are not copied into repository documentation.

## Final device state

Original settings verified: font scale `1.0`, rotation `0` (portrait), accelerometer rotation `0`. Display timeout was read, not modified. Production preview remains installed and the phone was returned to Library. Separate debug/test packages were absent at cleanup inspection; attempted uninstalls returned an absent-package error. No production app uninstall, data clear, version bump, push or publication was performed.
