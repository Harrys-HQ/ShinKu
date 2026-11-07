# Mockup 05 — implementation progress log

Started 2026-11-06. Target: [Library organization](05-library-tools-v1.png).

Percentages use the [shared implementation rubric](../mockup-visual-audit.md). Code written is not the same as runtime acceptance. This log records each panel and the individual changes inside it; the main audit is updated after verification.

| Checkpoint | Filters | Categories | Bulk selection | Board | Evidence |
| --- | --- | --- | --- | --- | --- |
| Before this implementation pass | 45% | 55% | 60% | 55% | Existing source/partial captures; confirmed gaps |
| First code pass | 45% | 55% | 60% | 55% | Changes written; verification pending, tags deliberately held |
| First filter runtime review | 80% | 55% | 60% | 65% | Debug build and filter test passed; title, count, compact rows and footer visually inspected |
| Functional/runtime pass | 90% | 90% | 85% | 90% | 67 JVM passes/one existing skip; 22 Android passes including counts, reordering, actions and locking. Final visual refinements still under review |
| Final local implementation/review | **95%** | **95%** | **95%** | **95%** | Final 22-test Android suite passed; three relevant cases repeated at genuine 1.6× emulator text passed. Final screenshots reviewed; five optimized APKs assembled and signature-verified |

| Part | Change | Progress | Remaining evidence |
| --- | --- | --- | --- |
| Filters: sheet header | Library filters title and real active-count badge | **95%** | Count/reset and default/1.6× text verified; final phone/theme appearance acceptance |
| Filters: compact rows | Icon/card rows with three choices; large-font reflow | **95%** | Actual 1.6× reflow, More filters and locking verified; final phone/theme appearance acceptance |
| Filters: footer | Equal Reset/Done actions; other tabs retained | **95%** | Reset/Done/Display verified at both text sizes; final phone/theme appearance acceptance |
| Categories: title counts | Reactive distinct library-title counts; excludes non-library entries | 100% of this functional part | Isolated database counts, duplicates, non-library exclusion and invalidation passed |
| Categories: layout | Intro, count subtitle, edit/More, inline Add including empty state | **95%** | Default/1.6× populated/empty/add/edit/More review passed; final phone/theme appearance acceptance |
| Categories: reorder | Resolve category keys instead of list indices around intro/Add | 100% of this functional part | Drag test verified actual category id and index 1 around the intro row |
| Bulk: primary actions | Download, Category, Mark read, More | **95%** | Default/1.6× bottom-placement assertion, callbacks, gradients/dividers/wrapping verified; final phone/theme appearance acceptance |
| Bulk: secondary actions | Unread/delete and all optional actions in closing overflow | 100% of this functional part | Download/unread/delete/migrate callbacks and menu dismissal passed; existing confirmations retained |

No phone display/font settings or reading records are part of this change. Existing controls and supported preferences stay available. Latest phone installation is verified separately from local implementation.

The initial bulk fixture incorrectly applied bottom alignment inside an AnimatedVisibility child; the production screen already uses the scaffold bottom slot. The fixture was corrected to exercise that same slot and assert the toolbar is near the bottom. A local-density override did not enlarge the separate Dialog window, so that capture is not evidence of large-text support. Genuine large-text verification uses the disposable emulator's font setting, restored afterward. Neither fixture issue is counted as an app rendering defect or a passing visual check.

100% on a narrowly defined functional part does not certify the entire panel's visual fidelity. Panel/board tags remain provisional until the remaining visual evidence is reviewed.

## Final verification and limits

- All known board-05 implementation gaps listed in the initial audit are addressed. Final per-panel scores are L4 / Styling3 / Controls4 / States4: 92.5 raw, rounded to the shared 95% tag. Styling remains provisional pending the owner's phone/theme appearance acceptance and broader theme/RTL/accessibility coverage. This is not an assertion of an exact measured 5% gap or five percent of coding hours remaining.
- JVM: 68 discovered, 67 passed, one existing skip, zero failures/errors. Log: `build/library05-verified-debug.log`. The first worker launch hit the documented Windows PATH quoting issue; process-local cleanup and `--no-daemon` resolved it without changing Windows environment settings.
- Android: final **OK (22 tests)** in `build/library05-final-android-tests.log`. The initial final filter rerun timed out because scrolling could target a horizontal container before expansion settled. Waiting for the expanded filter content and preferring vertical scroll actions resolved the test; assertions were retained. Final screenshots are in ignored `build/library05-review/default/`.
- Actual 1.6× emulator font: **OK (3 tests)** in `build/library05-large-font-tests.log`. Corresponding screenshots in `build/library05-review/large-font/` visibly show enlarged text and stacked filter controls. The emulator's original font scale **1.0 was restored**. No physical-phone setting was changed.
- Required optimized `assembleDevRelease --build-cache --parallel --quiet --console=plain` passed. Five architecture APK signatures verified; versions remain 2.8.1-preview.1 / 143. Final hashes/timestamps: `build/library05-checksums.json`; build/signature logs: `build/library05-release.log`, `build/library05-signatures.log`. ARM64 artifact built **2026-11-06 12:34 Shanghai**, in the usual APK output folder.
- UI/runtime checks use debug fixtures on Android 15/API 35. They do not establish optimized-APK runtime behavior, exhaustive light/RTL/tablet/TalkBack coverage or final appearance on the owner's Android 17 phone. Production library data, reading history and streak were not modified. No release publication or issue closure is part of this pass.
