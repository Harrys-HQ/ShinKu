# Generated mockups: implementation review

**Board-05 follow-up — 2026-11-06:** Library organization has now received its dedicated implementation pass: **95% provisional visual acceptance for filters, categories and bulk selection**. Per-part checkpoints and the latest 67 JVM / 22 Android / three repeated large-text checks are in the [progress log](mockups/05-implementation-progress.md). Final phone/theme acceptance remains separate; the older validation record below is historical.

**Completion correction — 2026-11-06:** the original record conflated code/functional coverage with full visual implementation. The initial audit confirmed board-05 gaps, subsequently addressed by the follow-up above. The blanket all-mockups-applied claim remains withdrawn. See the [visual audit](mockup-visual-audit.md).

Updated 2026-11-05. This review covers the supported scope of every currently generated board (01–13 / 39 panels) and the six earlier concept panels. Boards 14–24 still await images. Generated pixels, sample data and unsupported controls are not product specifications.

## Earlier concepts

| Panel | Applied behavior |
| --- | --- |
| Library / Continue Reading | Existing implemented hero, library cards and tools retained; boards 01–08 add readable labels and correct dock clearance. |
| Manga details | Existing cover/header/actions and chapter layout retained; metadata, action labels and chapter text wrap across multiple lines. Existing reading/bookmark/download callbacks remain. |
| More / statistics | Existing live profile, progress and statistics retained. Nickname, metric values and command labels wrap; XP columns share available width; content clears the actual dock inset. Streak remains calculated from real history. |
| ShinKu settings hub | Four main destinations: AI/translation, Reading/appearance, Performance and Maintenance. Advanced remains a separate fifth destination so inherited controls remain accessible. Search reaches each section and routes results to the corresponding page. |
| AI / translation setup | Provider-specific keys are masked in summaries and editors, with explicit reveal. Actual user-triggered connection tests expose not-tested/testing/success/failure status, reset when relevant configuration changes. Existing provider/model defaults and translation controls remain. |
| Reader controls | Supported reading/display/translation patterns implemented through boards 06–08; details are in the earlier review. |

## Numbered boards

Boards 01–08 and their capability adaptations are documented in [the 24-panel review](mockups-01-08-implementation.md), including the glass dock background/foreground correction.

| Board / panels | Implementation and adaptations |
| --- | --- |
| 09: Welcome; Storage and permissions; Theme and finish | Welcome offers setup or restore. Both pass through the required storage step; optional permissions, theme and guides retain their existing controls. Step count/progress reflects actual steps. AI credentials are explicitly optional. Restore opens the existing backup flow after setup. |
| 10: Backup; Restore; Sync | Backup scope notice distinguishes records/settings from downloaded images and excludes API keys. Restore shows the selected filename, preserves validation and selection controls, and requires confirmation before starting the existing job. Notification progress is retained; no unsupported cancellation guarantee. Existing Google Drive, WebDAV and Nextcloud remain. Sync scope and actual last timestamp are shown; Sync now is available only once the selected service has configuration. Configuration presence is not a successful connection check. |
| 11: Tracking; Tracker search; Security | Tracker rows show actual connected/not-connected labels and connect/disconnect actions; existing logout confirmation remains. Search stays specific to the selected tracker, preserves match confirmation and allows longer titles. Security shows actual biometric availability and clarifies incognito scope; existing lock/secure-screen behavior remains. |
| 12: Settings root; Appearance; Library | Root settings links to ShinKu. Theme controls remain live, with a noninteractive preview using actual theme tokens. Existing library display/grouping controls remain. Preference groups use rounded shared surfaces, retain lazy rows/search highlighting, and allow longer titles. Generated font/density/OS navigation controls are omitted. |
| 13: Reader; Browse; Downloads | Existing reader and browse settings retain supported selectors and navigation. Shared rounded groups and wrapped titles apply throughout. Downloads groups existing behavior controls and links to storage/cleanup; download-ahead remains numeric, and existing deletion scopes/confirmations remain. No global delete-all action is invented. |

## Validation record

The full JVM suite discovered 48 tests: 47 passed and one existing test was skipped. The Android 15/API 35 read-only emulator passed all 14 controlled tests, covering the existing mockup controls, download queue and translation geometry plus hub routing/search, credential reveal/cancel, onboarding storage gating and restore confirmation. Focused follow-up checks also passed after the final hub styling and shared-card width correction; these wait for each destination to render before capturing screenshots. The prescribed optimized devRelease build and all five APK signature checks passed. APK hashes and signatures belong in `build/generated-mockups-checksums.json`; local logs and screenshots stay under ignored build directories. This pass changes no schema, version, source ABI, backup identifier or deep-link scheme.

The physical phone keeps its original font scale and view size. Phone installation is tracked separately in the [checklist](mockups/checklist.md).

## Limits

This records functional changes so far and does not establish completion of the available visual designs. Boards 14–24 have briefs but no generated mockup yet. Tablet/RTL/large-text and every external tracker/authentication/storage failure path have not received exhaustive runtime validation. No remote AI requests, tracker logins, restore jobs, or real reading-history modifications are needed for the controlled UI checks.
