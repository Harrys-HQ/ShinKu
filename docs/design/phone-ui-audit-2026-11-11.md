# Physical-phone UI audit — 2026-11-11

## Installed artifact

CPH2691 connected through ADB. The installed app was 2.8.1 / 144, last updated November 8. Its signing-certificate SHA256 matched the November 10 review APK. The installed APK was copied locally as code-only rollback evidence, then the review APK was installed successfully with `adb install -r`; no uninstall or data-clear command was used.

Review APK SHA256: `B7E8C98D95D1A654A3400B5FD2691E2B49EBDCD1398C6E7BDB82DC80D0075493`. Version remains 2.8.1 / 144. This confirms physical installation; it does not establish visual completion of every board.

Display at the start: physical 1264×2780, existing logical override 1080×2376, font scale 1.0. These settings and the selected theme were left unchanged. Private screenshots and UI trees remain ignored under `build/phone-ui-review/`; personal reading records and account details are not reproduced here.

## First inspection pass

| Scope | Phone evidence | Finding / remaining work |
| --- | --- | --- |
| 01 Discovery | `browse` | Populated hero, recommendation shelf and scrollable tab strip present. Partial shelf/title views do not certify full-title access or bottom clearance. Search/catalogue states still need separate checks. |
| 02 Sources/extensions | `sources`, `extensions` | Language chips, source actions and installed/update sections present. No update/install/trust action invoked. Repository dialogs and untrusted states remain untested. |
| 03 Migration | `migration` | Source-centric selection confirmed. No migration or source selection started; configuration/comparison states remain untested. |
| 04 Updates/history | `updates`, `history` | Styled cards and readable wrapping titles present. Read/download/resume actions not invoked. Partially covered rows behind the dock require scroll-to-end verification; screenshots alone do not establish unreachable content. Download queue remains untested. |
| 05 Library organization | `library-filters`, `categories` | Filter heading, active badge, icon cards, explicit choices and Reset/Done present. Category count labels and edit/More/Add controls render. No filter, category, reordering or bulk action changed. Bulk mode and large-text phone coverage remain pending. |
| 12 Settings/appearance | `settings-root`, `appearance`, `library-preferences` | Grouped settings hub and labeled sample preview render with readable text in the owner's theme. Library preferences retain existing groups. Full page scroll/search and other theme states remain pending. |
| 13 Core preferences | `reader-preferences`, `browse-preferences`, `download-preferences` | Confirms the source finding: intro cards exist, but much of the underlying layout is inherited preference rows. Browse has the intro plus hide-library/adult/repository rows; the richer reference composition is not implemented here. No switches or preference values changed. |
| 14 Maintenance/performance | `shinku-hub`, `performance`, `maintenance`, `maintenance-detail` | Performance remains a list preference with switches, not the reference segmented card. Source-health cards and timestamps render. A stable/successful source can display an older failure message without identifying it as historical; clarify that status before visual acceptance. No checks, categorization, migration or cleanup initiated. |

Reader/translation boards 06–08 need a controlled chapter session. Onboarding 09, backup/restore/sync 10 and tracker/security flows 11 are not certified by this pass. They require their own supported state coverage. Tests on a separate sample-data app must be distinguished from optimized live-app evidence.

An additional More-screen scroll check (`more-bottom`) reached Settings, Data/storage, About and Help with the dock hidden. This supports lower-utility reachability in that state; it does not certify Updates/History end-of-list clearance. The temporary phone-side UI-tree file was removed after capture.

No app source was changed in this first inspection pass, so the previously verified artifact remains the installed review build. No new mockups, reading sessions, reading-progress actions, manual refresh/download/extension-update actions, preference changes, backup/restore actions, schema changes, version bump or publication were performed. Normal app navigation may still run its existing background loading and jobs.

## Updated APK checkpoint — November 11, 13:25 Shanghai

Installed the new ARM64 APK in place successfully; package reports version 2.8.1 / 144 and lastUpdateTime 2026-11-11 13:25:41. Verified local SHA256 before installation: `7065DB96E12F193C2232843E84E384DE7B6C5C056A3F8B788CB0347A3C6C5CDB`. This supersedes the November 10 installation above. No uninstall or data clear.

Targeted phone captures under the same ignored folder confirm:

- `updated-reader-top`: Defaults group, icon badges, bordered rows and fully wrapping Reader titles/summaries render.
- `updated-browse-settings`, `updated-languages`: actual display-mode row, enabled-source route and searchable language dialog render; Cancel/Apply remain visible. Cancelled without changing selections.
- `updated-performance`: stored Standard profile shows a checkmark; other pills wrap; independent switches and summaries remain visible. No profile/switch edited.
- `updated-maintenance`, `updated-source-health`: routes remain accessible and retained old errors are explicitly labelled historical alongside successful last checks. No manual check or maintenance action started.
- `updated-appearance`: sample preview remains explicitly labelled and wraps within the grouped surface.
- `updated-download-top`, `updated-download-bottom`: inherited groups use the new card rendering; final Download ahead row and information footer scroll above the gesture area.
- `updated-updates-lower`, `updated-history-lower`: downward scrolling hides the dock and exposes complete lower cards/actions. These resolve the initial suspicion that any dock-covered row was necessarily unreachable; terminal-list and large-text phone coverage remain separate.

This is targeted acceptance of the revised settings/status layouts, not certification of all 42 panels. Full live reader/translation, onboarding, restore/sync and authenticated tracker/security flows remain outside this navigation-only checkpoint. Their sample-data tests and 1.6× emulator captures are recorded in the [code-pass report](boards-01-14-code-pass-2026-11-11.md). Phone font scale remains 1.0 and logical display override remains 1080×2376. No reading session, progress mutation, preference edit, restore, download, cleanup or external publication was initiated. Removed the temporary phone-side UI-tree file after capture.

## Latest APK checkpoint — November11, 20:38 Shanghai

Installed the combined boards15–16 ARM64 APK in place successfully. Package reports2.8.1/144 and lastUpdateTime2026-11-11 20:38:37. Independently hashed the installed base APK on-device: `BE2F496F62A6F9422EF678FC9C02C9CF26A1288F95C6DF8BFB9455AD1B40C6D1`, matching the verified local artifact. This supersedes the earlier installation above.

Private captures `states16-phone-journey`, `states16-phone-share`, `states16-phone-share-options` and `states16-phone-badges` confirm readable Journey cards, the preserved crest and identity-only default preview, initially unchecked statistic choices, scrolling remaining options and separate reachable Avatar/Share/Close controls. Milestone cards render with wrapping labels in the available width. Closed the preview without sharing, selecting statistics, editing identity or equipping a badge. Returned to the main screen afterward. Counts/profile details are not reproduced here.

Board16 empty-library/loading/failure states were not induced in the live app; their controlled emulator evidence remains separate. Removed the temporary phone UI-tree file and confirmed font1.0 and the pre-existing1080×2376 logical override unchanged. No live reading, progress/preference edit, restore, download, cleanup or publication initiated.

## Remaining acceptance

**November12 02:18:59 installation update:** the boards01–24 ARM64 artifact below was installed in place successfully. Its installed base APK checksum independently matches `704B85FB7E6059F6CA138600818FB84E6E242EDA721AC7484FDA4C8EFEB297AB`. The phone remains locked; latest-build visual review is pending. Existing font1.0, physical1264×2780/density560 and override1080×2376/density480 were read without modification. No reading/progress/preference/restore/download/cleanup action was initiated. This supersedes the installation-pending checkpoint below.

Latest local boards01–24 build: version2.8.1/144, ARM64 SHA256 `704B85FB7E6059F6CA138600818FB84E6E242EDA721AC7484FDA4C8EFEB297AB`, built21:44:07 on November11. The phone disconnected during final checks, so this artifact is not installed or physically reviewed. The installed20:38 checkpoint above remains the earlier boards01–16 artifact. See [the latest code pass](boards-17-24-code-pass-2026-11-11.md) and `build/remaining-checksums.json`. Existing in-place installation authorization persists when reconnected; keep phone settings and reading data unchanged.

1. The confirmed settings-composition gaps and source-health historical-error wording have been addressed and checked above.
2. Verify scroll-to-end dock clearance and full-title access rather than equating initially covered content with unreachable controls.
3. Inspect the remaining board states with safe test data, then test larger text and alternate themes with settings restored afterward.
4. Keep visual acceptance partial until each supported panel/state has actual evidence. Physical installation alone is not a 100% completion certificate.
