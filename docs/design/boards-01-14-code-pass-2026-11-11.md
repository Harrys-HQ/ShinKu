# Boards 01–14: direct code completion pass

The owner authorized finishing the existing board direction in code before continuing to 15–24, without new image generation. This document tracks the current implementation, distinct from physical-device visual acceptance. It extends the [phone audit](phone-ui-audit-2026-11-11.md).

## Changes in this pass

| Board / shared scope | Implementation |
| --- | --- |
| 03 Migration | Search progress explicitly reports searches processed and search completion; it no longer labels machine matching as titles manually reviewed. Existing migration flags, confirmations and matching flow remain. |
| 06 Reader | Native retry/error layout has a theme-aware bordered card, wrapping text and minimum 48dp buttons. Permanent Webtoon errors use content height instead of a fixed 140dp box. Existing HTTP 404/410 retry suppression remains. |
| 07 Translation | Inspector presents one actual bubble at a time, separate original/translated cards, individual copy actions, combined copy and explicit previous/next navigation. Page retry still dismisses the inspector before requesting translation. No fabricated crop preview. |
| 08 Reader controls | Reading, theme, display/color and expanded advanced controls use shared section cards. Existing preference bindings, translation routes and cache confirmations remain. |
| 09 Onboarding | Dedicated scrollable welcome composition with the existing crest, Sanctuary hero, three readable feature cards and pinned Setup/Restore actions. Restore still follows the supported storage/setup flow. No generated artwork or fake library. |
| 10 Restore | Decoded-file summary shows actual title, chapter-record, category, history-record and app-setting counts. Validation runs on IO with a checking state and cancellation propagation. Restore remains disabled until validation and scope selection permit it; confirmation remains required. Background restoration/notification progress is retained rather than adding fictional in-page pause/rollback controls. |
| 11 Tracking | Search results gain a context card explaining matching/linking and subsequent status/progress editing. Actual result selection and linking remain unchanged; no guessed manga cover, tracker account status or new combined-write workflow. |
| 12–14 Shared settings | Explicit card surfaces and borders for standalone and grouped preferences, shared icon badges and specific icons for prominent controls. Titles can wrap fully. Lazy rendering and search navigation are preserved. Disabled rows no longer create zero-height entries that disagree with highlight indexing. |
| 13 Browse | Real catalogue display-mode preference, searchable installed-source-language picker with draft/Cancel/Apply behavior and preserved unknown selections, and a route to existing source-enable controls. Existing adult-content, library visibility and repository controls remain. No unsupported global-search-target preference. |
| 14 Performance | Visible profile pills bind the existing Standard/Low power/E-Ink preference and show a selected checkmark. Mode summaries and independent performance switches remain. |
| 14 Source health | Successful/stable sources explicitly identify retained old errors as historical, preserving diagnostics rather than presenting them as an unqualified current failure. |

The shared settings renderer also reaches inherited settings families used by later boards. That reuse is not individual acceptance of boards 15–24.

## Supported adaptations

Existing source-centric migration, numbered reader page chips, real sync providers, tracker linking followed by progress editing, and background restore notifications remain valid product flows. The generated reference boards guide hierarchy, spacing, surfaces and control clarity; they do not authorize fictional data, unsupported pause engines or altered reading semantics. The remaining source/reference records must retain these distinctions.

Boards 01–05 already contain substantial implementation from earlier passes. This pass does not certify their populated, offline, trust, bulk, long-title or end-of-list states merely because other pages changed. The [panel audit](mockup-visual-audit.md) remains the evidence ledger.

## Validation

- Initial settings compile and subsequent reader/backup/welcome compile passed in both dev variants.
- Final formatted debug/release compilation, debug assembly and test APK assembly passed. JVM run: 72 discovered, 71 passed, one existing skip; zero failures/errors. The new backup regression counts synthetic decoded data, including empty sections. Log: `build/boards-full-verified-debug.log`.
- Five new Android regressions cover language draft/search/apply, stored performance profiles, selected-bubble navigation/page retry, welcome callbacks/backup counts and settings highlight routing past disabled rows.
- The initial full emulator run was interrupted by Android replacing WebView. Event log records the test process killed with `stop com.google.android.webview due to installPackageLI`; no assertion result is inferred for unfinished tests. Retained log: `build/boards-full-android-tests-webview-interrupted.log`. A subsequent run exposed a fixture reopening a cancelled dialog before its closing composition rendered, and an existing downloader test requiring emulator connectivity. The fixture now waits for the closed UI; only the disposable emulator's Play Store is disabled, with connectivity restored. Neither fix removes assertions or changes phone settings.
- Final full Android suite: **OK (30 tests)**. Six affected cases repeated at actual emulator font scale 1.6: **OK (6 tests)**. Logs: `build/boards-full-android-tests.log`, `build/boards-full-large-font-tests.log`; runner completed and restored emulator font scale 1.0.
- Reviewed sample-data captures in `build/boards-full-review/default` and `large-font`: welcome actions remain pinned, body content scrolls at large text, bubble cards and retry/close remain readable, language selection shows its checkmark and correct Apply count, performance pills wrap, backup counts wrap and reader settings retain Done below scrolling sections. The highlight fixture confirms the correct visible row after disabled preferences. These fixtures do not certify full live reader, restore, tracker or onboarding flows.
- File-scoped formatting and `git diff --check` passed. Required `assembleDevRelease --build-cache --parallel --quiet --console=plain` completed successfully; all five APK signatures verify. Logs: `build/boards-full-release.log`, `build/boards-full-signatures.log`; artifact record: `build/boards-full-checksums.json`.

The new ARM64 review APK remains version **2.8.1 / 144**, built **November 11, 12:03 Shanghai**. SHA256: `7065DB96E12F193C2232843E84E384DE7B6C5C056A3F8B788CB0347A3C6C5CDB`. This hash distinguishes it from the older APK with the same version. Path: `app/build/outputs/apk/dev/release/ShinKu-dev-arm64-v8a-release.apk`. No version bump, publication or release is claimed.

The owner reconnected the phone after the local test checkpoint. The verified November 11 APK was installed in place at 13:25 Shanghai. Targeted physical review confirms revised Reader/Browse/Appearance/Downloads/Performance/Maintenance layouts and historical source-error wording; lower Updates/History cards scroll clear as the dock hides. See the [updated phone audit](phone-ui-audit-2026-11-11.md). Full live reader/translation, onboarding, restore/sync and authenticated tracker/security acceptance remain separate. No live reading session, phone preference edit, restore, migration, cleanup, extension update or external publication was initiated. The physical phone may stay locked during subsequent local code work.
