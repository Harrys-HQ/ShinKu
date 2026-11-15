# UI continuation and board recheck — 2026-11-10

Baseline: `a931760a62`, initially clean worktree. The owner authorized continuing the UI, revisiting boards 01–14, and skipping new mockups to conserve quota. Existing reference images, numbered briefs and Sanctuary components are sufficient to continue the remaining screen families. No image service was called.

## What the recheck establishes

All 14 canonical PNGs were opened and compared with their mapped source files and supporting components. This is a **source/reference recheck**, not fresh runtime acceptance of all 42 panels. Historical fixtures cover selected controls only. The previous blanket 90–95% figures are suspended and archived under `previousEstimate` in [implementation-audit.json](mockups/implementation-audit.json): some numerical scores did not even reproduce their tags under the stated weighting. Unscored does not mean zero implementation. Board 05 retains its prior provisional 95% and recorded default/1.6× evidence; this is still not owner-device acceptance.

| Board | Confirmed implementation and limits |
| --- | --- |
| 01 Discovery/search | Shelf hierarchy, source result cards and captions exist. Source catalogue filters/display modes differ from the pictured tabs. Shared Browse tabs now have an explicit 48dp minimum. Populated/fallback runtime comparisons remain pending. |
| 02 Sources/extensions | Language chips, source cards, pin/overflow, extension counts/trust warning and repository flows exist. Language chips now have a 48dp minimum. Generated repository freshness/counts are not supported requirements. |
| 03 Migration | Intro, grouped flags and comparison cards exist. First selection remains source-centric; matching progress is not the same as titles manually reviewed. No fictional pause engine should be added for fidelity. |
| 04 Updates/history/downloads | Styled rows, resume and real queue controls exist. Rich selection/long-caption/inset states still need current captures. Completed queue entries are not retained just to reproduce the reference. |
| 05 Library organization | Prior cards, explicit filters, real category counts, reordering and bulk More routing have dedicated evidence. Owner-device/theme acceptance remains separate. |
| 06 Reader/navigation | Deck, page indicator, Navigate search/tabs and page chips exist. Page chips adapt the thumbnail strip; webtoon loading/retry styling was not proved complete by the earlier summary. |
| 07 Translation | Crop/progress surfaces and inspector controls exist. Inspector remains a list-based AlertDialog with scroll navigation rather than the illustrated single-bubble sheet; background continuation adapts the pictured pause action. |
| 08 Reader sheets/actions | Tab header/Done and translation confirmation exist. Underlying display controls and richer AI/page actions are retained; exact row and menu compositions still differ. |
| 09 Onboarding | Welcome cards, storage/permission status, theme and guide/restore steps exist. Welcome does not reproduce the full illustrated hero, and real steps remain separate. |
| 10 Backup/sync | Scope choices, validation and confirmations exist. Rich restore-content/progress composition is incomplete. Preserve actual sync services and SAF pickers rather than sample-only flows. |
| 11 Tracking/privacy | Connection rows, styled search results, Track action and security capability status exist. Linking and status/progress editing remain separate flows. |
| 12 Settings/appearance | Search, featured ShinKu route and grouping exist. This pass replaces fixed “Chapter 42” and squeezed faux Resume action with a labeled, wrapping sample preview. Placeholder cover is an explicit adaptation. Library settings mostly retain existing groups. |
| 13 Reader/browse/download settings | Intro cards are present. Browse still only exposes hide-library/adult/repository rows; pictured defaults and language sheet are absent here. Cleanup remains on its real storage routes. |
| 14 Maintenance/performance | Actual mode ListPreference, maintenance routes and source cards exist; the segmented mode control is not reproduced. This pass fixes zero-check records receiving a tested rating and raises retry to 48dp. Advanced/debug acceptance is still pending. |

Per-panel limits, source paths and historical records are kept in the [generated audit](mockup-visual-audit.md). The [checklist](mockups/checklist.md), gallery and CSVs are generated from the same JSON. `build_review.py` supports deliberately unscored records, validates retained scores, and no longer directs the next agent to generate more mockups.

## Implemented next batch: board 15 first pass

- Journey totals and collection/chapter/tracker cards use wrapping, full-width content; tabs expose selection semantics and minimum 48dp height. Hero content was extracted into a separate file.
- Activity exposes 7/28/84-day windows with explicit dates and active-day counts. These filters apply to activity only; lifetime totals retain their meaning. Calendar dates replace fixed 24-hour subtraction, including daylight-saving boundaries. Each chart exposes textual reading durations.
- Empty activity and hourly history have explicit explanations. Empty history no longer implies a peak-hour persona. Genre data provides ranks only, so artificial rank-derived bar lengths were removed; longer genre/author labels can wrap.
- Profile and share-dialog content scroll independently of their actions. Existing explicit Share remains; the initial share theme is Obsidian. Milestone help has a 48dp target.

Board 15 remains **partial**, particularly granular share-stat opt-ins, exported-card long-text layout and milestone-grid composition. No share image was published. Boards 16–24 remain queued; their briefs should be implemented directly from the established components in small verified batches.

## Validation

- Final `compileDevDebugKotlin` / `compileDevReleaseKotlin` and dev debug/test APK builds passed (`build/ui-continuation-color-build.log`). File-scoped formatting and `git diff --check` passed; no repository-wide formatting claim.
- JVM: **71 discovered, 70 passed, one existing skip**, zero errors/failures (`build/ui-continuation-verified-debug.log`). Three new regressions exercise both daylight-saving transitions, calendar aggregation/window boundaries and empty history.
- Android 15/API 35 read-only emulator: full suite **OK (25 tests)** before the final color/inset correction (`build/ui-continuation-android-tests.log`). The three affected Journey/preview cases then passed again at default and actual **1.6×** font scale: `build/ui-continuation-final-default-tests.log` and `build/ui-continuation-final-large-font-tests.log`.
- Visual QA rejected preliminary captures with inherited black content text and missing fixture scaffold insets. Card content colors are now explicit; fixtures use a real Scaffold and scroll to the preview summary. Corrected final default/large-text captures were reviewed locally under `build/ui-continuation-review/final-*`. Long totals and preview summary remain readable and wrap; these fixtures do not certify whole-screen hero, milestones, all themes, RTL or all 42 reference panels.
- Required optimized `assembleDevRelease` passed, all **five** APK signatures verified. Artifact metadata/SHA256: `build/ui-continuation-checksums.json`; release/signature logs: `build/ui-continuation-release.log`, `build/ui-continuation-signatures.log`. Version remains **2.8.1 / 144**. The background runner initially failed during Android environment initialization; rerunning in the previously working direct Gradle environment succeeded.
- New fixtures use synthetic data. No physical-phone data or settings were used; emulator font scale was restored to 1.0. Physical-phone installation and optimized-runtime visual acceptance remain unverified.

No version bump, commit, push, GitHub publication or issue closure is part of this UI continuation. Extension ABI, backup identifiers, schema and deep links are outside the changes.

Latest ARM64 artifact: `app/build/outputs/apk/dev/release/ShinKu-dev-arm64-v8a-release.apk`, SHA256 `B7E8C98D95D1A654A3400B5FD2691E2B49EBDCD1398C6E7BDB82DC80D0075493`, built `2026-11-10T14:15:40.4971075+08:00`. Local review build only.
