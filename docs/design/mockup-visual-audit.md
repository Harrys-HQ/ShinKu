# Mockup implementation percentage audit

Updated 2026-11-12. Baseline: a931760a62 plus current UI worktree.

The previous all-mockups-applied claim is withdrawn. Working controls and retained UI are not proof of a completed redesign. Generated numbered boards still have visual implementation or acceptance work; none is certified 100%.

## How to read the percentages

Source/reference recheck of all 14 existing images and their mapped UI files. Previous blanket 90–95% estimates are archived as previousEstimate and suspended, not silently promoted or rescored. Several old scores do not reproduce their recorded percentages under the stated weighting. Unscored means implementation is partial or evidence is incomplete; it does not mean 0%. Board 05 retains its prior provisional score and historical runtime evidence. New screen batches use existing Sanctuary tokens and briefs without generating more mockups. The November 11 direct code pass is recorded separately in ../boards-01-14-code-pass-2026-11-11.md; supported adaptations and pending phone acceptance remain explicit.

These are review estimates anchored to explicit criteria, not exact measured percentages. Layout includes hierarchy, grouping and placement; styling includes surfaces, icons, typography and spacing. Controls cover supported actions and navigation; states cover selection/loading/error/confirmation behavior. Retained UI earns credit where it already meets the design. A 3 means mostly present, not visually accepted. A missing capture is an evidence limitation, not proof a feature is absent. No score represents phone installation.

All 14 numbered images were rechecked on 2026-11-10. Current relevant source and available local captures were compared. Existing captures predate some latest fixes; current source takes precedence. Fixture screenshots validate only their rendered control, not the surrounding scaffold/theme. The file named dock-browse.png actually shows History and was excluded as Browse evidence. Theme choice, manga artwork, sample counts and configurable grid density are not fidelity defects by themselves. Private runtime images remain in ignored build directories; no reading records are reproduced here.

## Numbered boards

| Board | Mockup | Implementation tag | Panel tags (in image order) |
| --- | --- | --- | --- |
| 01 | Discovery & search | Partial — unscored; acceptance pending | Discover: Unscored; Global search: Unscored; Source catalogue: Unscored |
| 02 | Sources, extensions & repositories | Partial — unscored; acceptance pending | Sources: Unscored; Extensions: Unscored; Repositories: Unscored |
| 03 | Source migration | Partial — unscored; acceptance pending | Find affected titles: Unscored; Migration options: Unscored; Review & progress: Unscored |
| 04 | Updates, history & downloads | Partial — unscored; acceptance pending | Updates: Unscored; History: Unscored; Downloads: Unscored |
| 05 | Library organization | 95% — visual acceptance pending | Library filters: 95%; Categories: 95%; Bulk selection: 95% |
| 06 | Reading & chapter navigation | Partial — unscored; acceptance pending | Paged reader: Unscored; Webtoon reader: Unscored; Chapters & pages: Unscored |
| 07 | Translation in the reader | Partial — unscored; acceptance pending | Area selection: Unscored; Inspect bubbles: Unscored; Chapter translation: Unscored |
| 08 | Reader settings & page actions | Partial — unscored; acceptance pending | Display controls: Unscored; Translation actions: Unscored; Page actions: Unscored |
| 09 | Getting started | Partial — unscored; acceptance pending | Welcome: Unscored; Storage & permissions: Unscored; Theme & finish: Unscored |
| 10 | Backups, restore & sync | Partial — unscored; acceptance pending | Create backup: Unscored; Restore preview: Unscored; Sync settings: Unscored |
| 11 | Tracking & privacy | Partial — unscored; acceptance pending | Trackers: Unscored; Link a title: Unscored; Security & privacy: Unscored |
| 12 | Core settings & appearance | Partial — unscored; acceptance pending | Settings: Unscored; Appearance: Unscored; Library settings: Unscored |
| 13 | Reader, browse & download preferences | Partial — unscored; acceptance pending | Reader preferences: Unscored; Browse preferences: Unscored; Download preferences: Unscored |
| 14 | Performance & maintenance | Partial — unscored; acceptance pending | Performance mode: Unscored; Maintenance & Tools: Unscored; Source health: Unscored |
| 15 | Reading journey & identity | Partial — unscored; acceptance pending | Journey overview: Unscored; Genres & patterns: Unscored; Milestones & profile: Unscored |
| 16 | Empty, loading & offline states | Partial — unscored; acceptance pending | Empty library: Unscored; Partial loading: Unscored; Offline & no results: Unscored |
| 17 | Dialogs, permissions & authentication | Partial — unscored; acceptance pending | Confirm an action: Unscored; App lock: Unscored; Website sign-in: Unscored |
| 18 | Recovery, diagnostics & release information | Partial — unscored; acceptance pending | Restore issue: Unscored; Crash recovery: Unscored; About & changes: Unscored |
| 19 | Tablet & landscape layouts | Partial — unscored; acceptance pending | Tablet library: Unscored; Landscape reader: Unscored; Large-text phone: Unscored |
| 20 | Light, high-contrast & E-Ink | Partial — unscored; acceptance pending | Light theme: Unscored; Accessible controls: Unscored; E-Ink profile: Unscored |
| 21 | Specialist source integrations | Partial — unscored; acceptance pending | MangaDex follows: Unscored; Source integration settings: Unscored; Metadata editor: Unscored |
| 22 | Extension details & merged manga | Partial — unscored; acceptance pending | Extension details: Unscored; Source preferences: Unscored; Merged manga & notes: Unscored |
| 23 | Batch tools & advanced organization | Partial — unscored; acceptance pending | Batch add: Unscored; Smart search: Unscored; Advanced categories: Unscored |
| 24 | Widgets & system surfaces | Partial — unscored; acceptance pending | Updates widget: Unscored; Widget size variants: Unscored; Notifications & shortcuts: Unscored |

Supported direct code pass covers01–24 without new mockups. Remaining live authentication, reader, restore, source integrations, widget hosts and full visual acceptance are tracked per panel and in the dated reports; no board is certified100%.

## Panel evidence and remaining work

### 01 — Discovery & search (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Discover | Unscored | Unscored | Sanctuary pill tabs, featured carousel with genre badge and 2-line description, descriptive shelf subtitles, genre metadata on cards and dock clearance exist. | Feed shelves and captions are styled, but populated/loading/fallback compositions still need fresh runtime comparison. | Keep real recommendations and source limits; no invented totals. | Source only; runtime comparison pending |
| Global search | Unscored | Unscored | Query summary header, rounded Sanctuary source result cards with globe badge, language subtitle, See all pill, card genre subtitles and bottom progress card exist. | Source result cards exist; source/language/genre controls differ from the illustrative board. Preserve supported search filters. | Global genre/language chips and total counts use supported data, not sample values. | Source only; runtime comparison pending |
| Source catalogue | Unscored | Unscored | Sanctuary filter/mode controls, secondary source/genre metadata on grid cards and rounded Sanctuary loading-more footer card exist. | Catalogue paging and display modes remain; compare the actual filter sheet and populated grid rather than assume the reference tabs were reproduced. | Retain actual source filters and real paging; no fabricated page count. | Source only; runtime comparison pending |

Source evidence:

- [TabbedScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/components/TabbedScreen.kt)
- [FeedScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/FeedScreen.kt)
- [GlobalSearchScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/GlobalSearchScreen.kt)
- [BrowseSourceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/BrowseSourceScreen.kt)

### 02 — Sources, extensions & repositories (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Sources | Unscored | Unscored | Sanctuary pill tabs, horizontal language filter chips, rounded source rows, Latest pill, pin toggle and overflow menu button exist. | Language chips, source cards, pin and overflow exist. Explicit chip target raised to 48dp in this pass; full source-list capture still pending. | Keep all four real Browse routes. | Source only; runtime comparison pending |
| Extensions | Unscored | Unscored | Language filter chips, section headers with item counts, vX.Y.Z · language metadata order, explicit untrusted warning banner and action pills exist. | Section counts and trust warning exist. Installed/available/untrusted row states need fresh runtime comparison. | Review trust remains explicit; generated extra tab is not a route replacement. | Source only; runtime comparison pending |
| Repositories | Unscored | Unscored | Installed repositories heading, Sanctuary repo cards with globe icon badge and overflow menu, inline + Add repository card and enhanced Add repository dialog with security notice exist. | Repository cards and add/review flow exist. Do not add invented freshness, available-extension counts or verification badges. | No invented repository freshness, extension count or verification badge. | Source only; runtime comparison pending |

Source evidence:

- [SourcesScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/SourcesScreen.kt)
- [ExtensionsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/ExtensionsScreen.kt)
- [ExtensionReposScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/browse/ExtensionReposScreen.kt)

### 03 — Source migration (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Find affected titles | Unscored | Unscored | Top explanatory Sanctuary info card, Sanctuary filter pills for All sources / Missing sources (N) / Failed updates and rounded source rows exist. | Confirmed adaptation: selection remains source-centric, rather than the title-centric list pictured in the board. | Retains source-first selection flow and keeps missing-source versus title counts distinct. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/migration-missing.png` |
| Migration options | Unscored | Unscored | Header and subtitle, grouped Sanctuary switch card for migration flags, preservation info card, collapsible Advanced card and primary action button exist. | Grouped migration flags and advanced choices exist; title/source context is not the exact illustrated selection page. | Preserve saved flags and destructive-download confirmation. | Source only; runtime comparison pending |
| Review & progress | Unscored | Unscored | Matching cards now label searches processed and search completion separately from manual review; existing migration confirmations remain. | Phone acceptance of matching, missing candidates and actual migration progress remains pending. | No fictional pause/resume engine or guaranteed source matching. | Source only; runtime comparison pending |

Source evidence:

- [MigrateSourceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/MigrateSourceScreen.kt)
- [MigrationConfigScreenSheet.kt](../../app/src/main/java/com/shinku/reader/feature/migration/config/MigrationConfigScreenSheet.kt)
- [MigrationListScreenContent.kt](../../app/src/main/java/com/shinku/reader/feature/migration/list/MigrationListScreenContent.kt)

### 04 — Updates, history & downloads (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Updates | Unscored | Unscored | Top Upcoming and Refresh Sanctuary action pills, portrait cards, tinted chapter tag pills with unread dot, Read/Resume button and dock clearance exist. | Update cards and read/download actions exist; capture multi-selection and bottom-inset behavior at large text. | Use actual update groups and existing downloads. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/dock-updates.png` |
| History | Unscored | Unscored | Inline Search reading history pill bar, portrait cards, Ch. X tag badge + timestamp, Resume button and overflow removal exist. | History cards and resume exist; verify complete long titles and chapter/page text in populated runtime state. | History does not supply the mockup's page totals. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/dock-history.png` |
| Downloads | Unscored | Unscored | Enhanced queue summary card with download icon badge, live downloading/queued counts, aggregate progress bar and Pause/Resume all button exist. | Queue summary and actual pause/resume are present. Finished entries are not retained solely to reproduce the generated sample. | No fictional finished archive. | Source only; runtime comparison pending |

Source evidence:

- [UpdatesScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/updates/UpdatesScreen.kt)
- [HistoryItem.kt](../../app/src/main/java/com/shinku/reader/presentation/history/components/HistoryItem.kt)
- [DownloadQueueScreen.kt](../../app/src/main/java/com/shinku/reader/ui/download/DownloadQueueScreen.kt)

### 05 — Library organization (95% — visual acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Library filters | 95% | 4 / 3 / 4 / 4 | Sheet heading, live active badge, three compact icon/gradient cards, Reset/Done and More filters are implemented and runtime-reviewed. Extra filters and Group remain accessible. | Prior filter implementation has default/1.6x captures and interaction checks; final owner-device/theme acceptance remains pending. | Downloaded/Unread/Completed are primary; Started/Bookmarked/custom interval/adult/tracker filters remain under More filters. Group remains a fourth tab. Large text reflows the row. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library-filters.png` |
| Categories | 95% | 4 / 3 / 4 / 4 | Intro, real distinct library-title counts, edit/More, inline Add including empty state, and key-based reorder indices are implemented. In-memory counts/invalidation and drag/callback tests pass. | Real category counts, edit/add/delete and reorder are implemented with earlier runtime evidence; no new full-device certification. | Counts are computed from actual distinct favorite titles, including zero; illustrated counts/names are never seeded. Deletion retains confirmation. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library05-categories.png` |
| Bulk selection | 95% | 4 / 3 / 4 / 4 | Download/Category/Mark read/More toolbar, dividers, shared gradient, readable wrapping labels and closing secondary menus are implemented. Bottom-slot placement and callbacks pass runtime checks. | Primary bulk actions and More routing are implemented with earlier runtime evidence; retain confirmations and conditional capabilities. | Download remains conditional on supported selection; all optional existing actions remain under More. User grid density is unchanged. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library05-bulk-toolbar.png` |

Source evidence:

- [LibraryFilterItem.kt](../../app/src/main/java/com/shinku/reader/presentation/library/components/LibraryFilterItem.kt)
- [LibrarySettingsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/library/LibrarySettingsDialog.kt)
- [CategoryListItem.kt](../../app/src/main/java/com/shinku/reader/presentation/category/components/CategoryListItem.kt)
- [MangaBottomActionMenu.kt](../../app/src/main/java/com/shinku/reader/presentation/manga/components/MangaBottomActionMenu.kt)
- [CategoryScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/category/CategoryScreen.kt)
- [CategoryScreenModel.kt](../../app/src/main/java/com/shinku/reader/ui/category/CategoryScreenModel.kt)
- [categories.sq](../../data/src/main/sqldelight/com/shinku/reader/data/categories.sq)
- [CategoryRepositoryImpl.kt](../../data/src/main/java/com/shinku/reader/data/category/CategoryRepositoryImpl.kt)

Individual part log (functional-part completion is separate from whole-panel fidelity):

| Part | Tag | Evidence / remaining acceptance |
| --- | --- | --- |
| Filters: heading and active badge | 95% | Real count updates/reset verified; final phone/theme appearance acceptance pending. |
| Filters: icon/card rows and More filters | 95% | Default and actual 1.6× emulator text reviewed; primary choices, extra filters and locked Downloaded tested. |
| Filters: Reset/Done and other tabs | 95% | Reset, persistent Done and Display transition pass; final phone styling acceptance pending. |
| Categories: real title counts | 100% | Functional scope complete: zero/singular/plural, distinct memberships, non-library exclusion and query invalidation tested in isolated memory DB. |
| Categories: page/cards/add/edit/More | 95% | Populated/empty layouts and add/rename/delete callbacks reviewed at default and 1.6× text; final phone appearance acceptance pending. |
| Categories: reorder positions | 100% | Functional scope complete: drag test checks real category id and index 1 despite the intro row. |
| Bulk: four primary actions and bottom placement | 95% | Default/1.6× screenshots, bottom-position assertion and primary actions pass; final integrated phone appearance acceptance pending. |
| Bulk: secondary actions and dismissal | 100% | Functional scope complete: unread/delete/migrate/download callbacks and dismissal pass; all conditional existing routes retained. |

[Dated progress checkpoints](mockups/05-implementation-progress.md).

### 06 — Reading & chapter navigation (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Paged reader | Unscored | Unscored | Restrained top controls, rounded lower control deck with chapter prev/next buttons, page slider, bottom action bar, and token-driven page indicator exist. ImageView translation overlays now follow drawable/source dimensions, image matrix and padding; bubble text is clipped to its bounds. | Reader deck and slider are styled. Existing configurable icon actions remain rather than the exact pictured button composition. | Preserve gesture-driven visibility and reading modes. | Source review; Native FIT_CENTER/source-size/padding/hidden-overlay and tiny-bubble containment regressions pass; this is rendering-fixture evidence, not full-reader phone acceptance. November12 phone: corresponding native/component fixtures passed in isolated devDebug packages; optimized production Reader/OCR integration remains unverified. |
| Webtoon reader | Unscored | Unscored | Native error content uses a theme-aware bordered card, wrapping text, 48dp actions and content height in Webtoon; permanent failures keep existing retry suppression. | Full-reader loading/error/retry capture at normal and large text remains pending. | Do not display loading and retry as the same state. | Source only; runtime comparison pending |
| Chapters & pages | Unscored | Unscored | Navigate sheet header and subtitle, Chapters/Pages pill tabs, search with leading icon, highlighted current chapter badge, and numbered page chip grid exist. | Navigate header/search and page chips exist. Page chips are a supported adaptation, not the thumbnail filmstrip in the image. | Numbered page chips replace unsupported thumbnails; bookmarks are chapter-level. | Source only; runtime comparison pending |

Source evidence:

- [ReaderPageIndicator.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageIndicator.kt)
- [ChapterListDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/ChapterListDialog.kt)
- [ReaderAppBars.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/appbars/ReaderAppBars.kt)
- [ReaderBottomBar.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/appbars/ReaderBottomBar.kt)
- [ChapterNavigator.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/components/ChapterNavigator.kt)
- [ReaderPageImageView.kt](../../app/src/main/java/com/shinku/reader/ui/reader/viewer/ReaderPageImageView.kt)

### 07 — Translation in the reader (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Area selection | Unscored | Unscored | Bounded normalized crop geometry, wrapping Reset/Cancel/Details/Translate footer and48dp icon controls; existing production crop callbacks retained. | Full-reader gesture/viewport integration and all-device visual acceptance remain pending. | Crop geometry stays tied to actual viewport coordinates. | Source + runtime captures (including actual 1.6× text); Synthetic bitmap and fake callbacks; crop pixel bounds and minimum-size unit regressions pass. November12 phone: corresponding native/component fixtures passed in isolated devDebug packages; optimized production Reader/OCR integration remains unverified.; local `build/reader-viewport-review/normal/reader-crop-tools.png` |
| Inspect bubbles | Unscored | Unscored | One selected bubble, separate original/translated copy cards, position count, previous/next navigation, combined copy and page retry are implemented. | The actual selected text is presented without a fabricated cropped-image preview. Full-reader translation integration still needs phone acceptance. | No cropped preview or isolated-bubble retry is currently supported; retry is page-scoped. | Source only; runtime comparison pending |
| Chapter translation | Unscored | Unscored | Progress labels wrap, completed count/language stack, body scrolls and a labelled overlay switch row exposes one toggle action. Run in background and Cancel remain distinct. | Actual provider jobs/background continuation/cancellation need live integration acceptance. | Keep Run in background; unsupported Pause and invented per-page states are excluded. | Source + runtime captures (including actual 1.6× text); Synthetic long language/count fixture; overlay/background/Cancel callbacks pass. November12 phone: corresponding native/component fixtures passed in isolated devDebug packages; optimized production Reader/OCR integration remains unverified.; local `build/reader-viewport-review/normal/reader-batch-progress.png` |

Source evidence:

- [AreaOcrCropOverlay.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/AreaOcrCropOverlay.kt)
- [BubbleInspectorDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/BubbleInspectorDialog.kt)
- [BatchTranslationProgressDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/BatchTranslationProgressDialog.kt)
- [CropSelection.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/CropSelection.kt)

### 08 — Reader settings & page actions (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Display controls | Unscored | Unscored | Reading, theme, display and expanded advanced settings use shared bordered section cards, retaining real preference bindings and tabs. | The reference icon composition is adapted to supported controls. Full-reader and broad theme acceptance remain pending. | Retain supported background/filter options, not invented swatches. | Source only; runtime comparison pending |
| Translation actions | Unscored | Unscored | Existing scoped translation actions retained; long result title wraps, language headings reserve copy controls, language picker scrolls and dropdown labels reserve arrow space. | Live OCR/provider behavior and full-reader entry-point integration remain pending. | Keep real engine/language paths; cache clearing must not imply manga deletion. | Source + runtime captures (including actual 1.6× text); Fixtures cover page/chapter callbacks, long original-text copy visibility, language dropdown/swap draft Cancel/Confirm, processing Back and long error dismissal; short-landscape target field reviewed. November12 phone: corresponding native/component fixtures passed in isolated devDebug packages; optimized production Reader/OCR integration remains unverified.; local `build/reader-viewport-review/normal/reader-translation-options.png` |
| Page actions | Unscored | Unscored | Sanctuary card action buttons with tinted primary icon containers for Set cover, Copy, Share, Save, and AI Companion cards exist. | Page actions retain richer AI tools and dual-spread handling. Orientation selection remains a separate route; this is not the exact compact menu pictured. | Unsupported page bookmark excluded; keep chapter bookmark. | Source only; runtime comparison pending |

Source evidence:

- [ReaderSettingsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/settings/ReaderSettingsDialog.kt)
- [TranslationOptionsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/TranslationOptionsDialog.kt)
- [ReaderPageActionsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageActionsDialog.kt)
- [AreaTranslationResultDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/AreaTranslationResultDialog.kt)
- [ReaderAiDialogs.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/ReaderAiDialogs.kt)

### 09 — Getting started (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Welcome | Unscored | Unscored | Dedicated scrollable welcome view uses the existing crest, Sanctuary hero, three benefit cards and pinned Setup/Restore callbacks. | Existing app crest replaces generated manga artwork. Complete storage/theme/onboarding phone walkthrough remains pending. | Use appropriate owned/licensed art; do not copy sample manga art as an app asset. | Source only; runtime comparison pending |
| Storage & permissions | Unscored | Unscored | Sanctuary storage card with Configured/Required status badge, directory path, 48dp selection button, and storage help card exist. | Storage status/actions and permission steps exist; storage and permissions remain separate real steps. | Actual four steps differ from sample step numbering. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/onboarding-storage-required.png` |
| Theme & finish | Unscored | Unscored | Theme selector cards, Sanctuary guide card, restore card, and optional AI notice exist. | Theme and guide/restore routes exist as separate steps rather than one combined theme-and-finish screen. | AI setup stays optional; preserve all actual setup steps. | Source only; runtime comparison pending |

Source evidence:

- [OnboardingScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/OnboardingScreen.kt)
- [StorageStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/StorageStep.kt)
- [ThemeStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/ThemeStep.kt)
- [GuidesStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/GuidesStep.kt)

### 10 — Backups, restore & sync (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Create backup | Unscored | Unscored | Sanctuary intro card with backup icon, scope notice, library/settings section cards, and bottom action bar exist. | Backup header and scope choices exist; preserve the supported SAF destination picker and credential exclusions. | Downloaded images/API keys are not included merely because the concept suggests them. | Source only; runtime comparison pending |
| Restore preview | Unscored | Unscored | Already-decoded backup contents provide actual title, chapter, category, history and setting counts; IO validation has a checking state and propagates cancellation. | SAF selection, validation errors, scope confirmation and background restore notifications remain the actual flow. No fictional cover grid, pause or rollback engine. | Progress remains notification-based; no unsupported cancellation guarantee. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/restore-confirmation.png` |
| Sync settings | Unscored | Unscored | Sanctuary cloud sync hero card with last sync timestamp, configured status badge, scope notice, and Sync now action exist. | Sync settings retain real service configuration and scope choices. Do not replace supported sync backends with a fictional local-folder-only flow. | Keep Drive/WebDAV/Nextcloud; local-folder sync is not an implemented transport. | Source only; runtime comparison pending |

Source evidence:

- [CreateBackupScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/CreateBackupScreen.kt)
- [RestoreBackupScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/RestoreBackupScreen.kt)
- [SettingsDataScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDataScreen.kt)

### 11 — Tracking & privacy (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Trackers | Unscored | Unscored | Sanctuary tracking banner card, tracker rows with Connect/Disconnect pill buttons, and sync options exist. | Tracker intro and connection rows exist. Fresh connected/disconnected/error-state captures remain pending. | Preserve actual tracker capabilities and logout confirmation. | Source only; runtime comparison pending |
| Link a title | Unscored | Unscored | Search results include a context card explaining title matching/linking followed by status and progress editing. | Tracker search/link and status/progress editing remain separate supported steps; connected-account phone acceptance is pending. | Search is per tracker; cross-tracker mixed results are unsupported. | Source only; runtime comparison pending |
| Security & privacy | Unscored | Unscored | Sanctuary security banner card, biometric status pill badge, and grouped security preferences exist. | Security intro, biometric capability and privacy controls exist. Preserve device-dependent authentication and actual incognito semantics. | No invented sensitive-content blur; developer tools keep their existing route. | Source only; runtime comparison pending |

Source evidence:

- [SettingsTrackingScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsTrackingScreen.kt)
- [TrackerSearch.kt](../../app/src/main/java/com/shinku/reader/presentation/track/TrackerSearch.kt)
- [SettingsSecurityScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsSecurityScreen.kt)

### 12 — Core settings & appearance (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Settings | Unscored | Unscored | Search pill bar at top, prominent ShinKu settings hero card with AutoAwesome icon and Open pill, and grouped command row settings exist. | Settings search pill, featured ShinKu card and grouped routes exist; the grouped hierarchy differs from the single flat reference list. | General Settings and ShinKu-specific hub remain distinct. | Source only; runtime comparison pending |
| Appearance | Unscored | Unscored | Live theme selectors, cover preview card with reading progress subtitle and Resume pill button, and rounded Sanctuary preference groups exist. | This pass replaces fixed Chapter 42 and the squeezed Resume pill with an explicitly labeled sample preview and wrapping text. Placeholder cover remains a documented adaptation. | Global font/density/OS navigation controls are unsupported, excluded from scope. | Source + runtime captures (including actual 1.6× text); Appearance sample fixture only; complete Appearance page not certified.; local `build/ui-continuation-review/final-default/appearance-sample-preview.png` |
| Library settings | Unscored | Unscored | Shared settings rendering now gives grouped and standalone preferences explicit Sanctuary card surfaces, borders, icon badges and wrapping titles. | Phone acceptance of complete category/update/behavior groups remains pending. | Some display/sort controls belong in Library sheet; do not duplicate conflicting state. | Source only; runtime comparison pending |

Source evidence:

- [SettingsMainScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsMainScreen.kt)
- [SettingsAppearanceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsAppearanceScreen.kt)
- [SettingsLibraryScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsLibraryScreen.kt)
- [PreferenceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/PreferenceScreen.kt)

### 13 — Reader, browse & download preferences (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Reader preferences | Unscored | Unscored | Defaults now form an explicit group; shared bordered settings cards and specific reader icons extend beyond the intro. | Preserve inherited reader options and actual orientation/default-mode dialogs; full phone acceptance remains pending. | Keep real reading modes, orientation and advanced options. | Source only; runtime comparison pending |
| Browse preferences | Unscored | Unscored | Real catalogue display modes, searchable installed-source-language selection with draft/Cancel/Apply and preserved unknown values, plus enabled-source navigation are implemented. | Global-search defaults absent from supported preferences remain a documented adaptation; whole-screen phone acceptance is pending. | Global default search target/view and duplicate language settings are not assumed available. | Source only; runtime comparison pending |
| Download preferences | Unscored | Unscored | Shared bordered groups, wrapping rows and specific Wi-Fi/delete icons extend the existing download/storage controls. | Storage cleanup and destructive confirmations remain on their actual routes and need separate phone acceptance. | Download-ahead is numeric; no invented global delete-all control. | Source only; runtime comparison pending |

Source evidence:

- [SettingsReaderScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsReaderScreen.kt)
- [SettingsBrowseScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsBrowseScreen.kt)
- [SettingsDownloadScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDownloadScreen.kt)

### 14 — Performance & maintenance (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Performance mode | Unscored | Unscored | Standard/Low power/E-Ink profile pills bind the existing stored preference and show a selected checkmark; independent performance controls remain. | Whole-screen phone and device-dependent refresh/scaling acceptance remains pending. | Preserve platform performance mode keys and native refresh rates. | Source only; runtime comparison pending |
| Maintenance & Tools | Unscored | Unscored | Shared settings cards extend maintenance routes; source-health untested states stay explicit and successful sources label retained errors as historical. | Whole-screen maintenance and destructive-confirmation acceptance remains pending. | Preserve individual maintenance sub-screens. | Source only; runtime comparison pending |
| Source health | Unscored | Unscored | Source-health cards preserve actual timestamps and retry behavior, distinguish untested records and qualify retained prior failures. | Advanced/debug routes and broader populated/error phone states still need acceptance; source-health changes do not certify them. | Keep dynamic latency and failure classification. | Source only; runtime comparison pending |

Source evidence:

- [SettingsShinKuSettingsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsShinKuSettingsScreen.kt)
- [SourceHealthScreenContent.kt](../../app/src/main/java/com/shinku/reader/presentation/more/sourcehealth/SourceHealthScreenContent.kt)
- [SettingsAdvancedScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsAdvancedScreen.kt)
- [DebugInfoScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/debug/DebugInfoScreen.kt)

### 15 — Reading journey & identity (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Journey overview | Unscored | Unscored | Full-width wrapping totals and collection/chapter/tracker cards; selectable 48dp stats tabs; readable XP/streak reflow. | Whole-screen profile/overview acceptance across phone/tablet themes remains pending. | Lifetime totals stay lifetime totals; date ranges are explicitly scoped to reading activity. | Source + runtime captures (including actual 1.6× text); Totals/collection fixture only; hero, quests and tabs not visually certified.; local `build/ui-continuation-review/final-default/journey-complete-totals.png` |
| Genres & patterns | Unscored | Unscored | 7/28/84-day calendar-safe activity windows, empty explanations, date labels, chart text alternatives and truthful genre ranks. | Runtime comparison and broader TalkBack/RTL/theme review remain pending. | No numeric genre proportions are available in StatsData, so rank-derived bars were removed rather than presented as measured activity. | Source + runtime captures (including actual 1.6× text); Activity/empty-hourly fixtures only; genre labels remain source-reviewed.; local `build/ui-continuation-review/final-default/journey-activity-28-days.png` |
| Milestones & profile | Unscored | Unscored | Per-session share-stat opt-ins start unchecked; preview/export receive the same filtered payload. Export text wraps into a measured-height image; avatars are sampled/center-cropped and default crest is untinted. Explicit vertical footer and bounds checks keep actions separate. Milestone columns adapt to width/font scale with full labels and scrolling details. | Whole-screen phone, alternate themes, RTL and broader accessibility acceptance remain pending; component fixtures and synthetic PNG exports are not full live-flow certification. | No new profile backend, ranking or automatic publication; current sharing still requires the explicit Share action. | Source + runtime captures (including actual 1.6× text); controlled milestone/share fixtures and exported images; full phone flow pending; local `build/states16-review/default/journey15-share-options.png` |

Source evidence:

- [StatsScreenContent.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/StatsScreenContent.kt)
- [StatsHeroCard.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/StatsHeroCard.kt)
- [JourneyOverviewMetrics.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/JourneyOverviewMetrics.kt)
- [JourneyCollectionOverview.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/JourneyCollectionOverview.kt)
- [ActivityHeatmapCard.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/ActivityHeatmapCard.kt)
- [CircadianVelocityPanel.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/CircadianVelocityPanel.kt)
- [GenreAffinityPanel.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/GenreAffinityPanel.kt)
- [ReaderIdentityDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/more/components/ReaderIdentityDialog.kt)
- [SanctuaryShareCardDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/SanctuaryShareCardDialog.kt)
- [ShareCardContent.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/ShareCardContent.kt)
- [ShareCardOptions.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/ShareCardOptions.kt)
- [SanctuaryShareCardPreview.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/SanctuaryShareCardPreview.kt)
- [SanctuaryShareCardRenderer.kt](../../app/src/main/java/com/shinku/reader/presentation/more/stats/components/SanctuaryShareCardRenderer.kt)

### 16 — Empty, loading & offline states (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Empty library | Unscored | Unscored | Single scrollable card, book icon, real Browse-sources route, Restore backup and retained guide, with dock clearance and optional AI setup. | Full phone/theme/RTL/accessibility acceptance remains pending. No new mockup generated. | Preserve actual library-empty/filter semantics; do not fabricate covers or require AI setup. | Source + runtime captures (including actual 1.6× text); synthetic state/callback fixture; full live-flow acceptance pending; local `build/states16-review/default/states16-library-empty.png` |
| Partial loading | Unscored | Unscored | Discover retains loaded shelves/pending-feed skeletons while actual failed source requests get retry and source routes. Timeout/exception feedback excludes empty success and cancellation; limits/scheduling unchanged. | Full phone/theme/RTL/accessibility acceptance remains pending. No new mockup generated. | Nullable AI title matching is separate from catalogue failures; retry uses the existing refresh flow. | Source + runtime captures (including actual 1.6× text); synthetic state/callback fixture; full live-flow acceptance pending; local `build/states16-review/default/states16-discover-loaded.png` |
| Offline & no results | Unscored | Unscored | Search separates completed no-match, resolved empty-source selection and source failures. Unfiltered error counts remain visible beside actionable results even when failed rows are filtered out. Existing filter actions preserve the query. | Full phone/theme/RTL/accessibility acceptance remains pending. No new mockup generated. | No durable offline result cache is claimed. Show actual failures and retained loaded results; no invented Offline/Showing saved results badge. | Source + runtime captures (including actual 1.6× text); synthetic state/callback fixture; full live-flow acceptance pending; local `build/states16-review/default/states16-search-partial.png` |

Source evidence:

- [LibraryEmptyState.kt](../../app/src/main/java/com/shinku/reader/presentation/library/components/LibraryEmptyState.kt)
- [LibraryTab.kt](../../app/src/main/java/com/shinku/reader/ui/library/LibraryTab.kt)
- [BrowseTab.kt](../../app/src/main/java/com/shinku/reader/ui/browse/BrowseTab.kt)
- [FeedScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/FeedScreen.kt)
- [FeedRequests.kt](../../app/src/main/java/com/shinku/reader/ui/browse/feed/FeedRequests.kt)
- [FeedScreenModel.kt](../../app/src/main/java/com/shinku/reader/ui/browse/feed/FeedScreenModel.kt)
- [GlobalSearchScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/GlobalSearchScreen.kt)
- [SearchScreenModel.kt](../../app/src/main/java/com/shinku/reader/ui/browse/source/globalsearch/SearchScreenModel.kt)
- [EmptyScreen.kt](../../presentation-core/src/main/java/com/shinku/reader/presentation/core/screens/EmptyScreen.kt)

### 17 — Dialogs, permissions & authentication (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Confirm an action | Unscored | Unscored | Selected count, separate unchecked library/download choices, disabled empty confirmation and explicit action labels. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards17-remove-scopes.png` |
| App lock | Unscored | Unscored | Private lock guidance background before the unchanged device biometric/credential prompt. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards17-lock-background.png` |
| Website sign-in | Unscored | Unscored | Current website host in toolbar; existing cookies, header restrictions and browser actions retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |

Source evidence:

- [DeleteLibraryMangaDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/library/DeleteLibraryMangaDialog.kt)
- [UnlockActivity.kt](../../app/src/main/java/com/shinku/reader/ui/security/UnlockActivity.kt)
- [UnlockScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/security/UnlockScreen.kt)
- [WebViewScreenContent.kt](../../app/src/main/java/com/shinku/reader/presentation/webview/WebViewScreenContent.kt)

### 18 — Recovery, diagnostics & release information (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Restore issue | Unscored | Unscored | Invalid backup offers another file through the platform picker; real restore confirmation/job remain. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards18-unreadable-backup.png` |
| Crash recovery | Unscored | Unscored | Restart and Copy error details prioritized; selectable exception details and explicit Share diagnostics. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards18-recovery.png` |
| About & changes | Unscored | Unscored | Bounded scrolling release entries, loading state and Close; actual bundled changelog remains. November12 correction removes the withdrawn95% completion claim from bundled and website release notes. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards18-changes.png` |

Source evidence:

- [RestoreBackupScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/RestoreBackupScreen.kt)
- [CrashScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/crash/CrashScreen.kt)
- [WhatsNewDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/about/WhatsNewDialog.kt)
- [InfoScreen.kt](../../presentation-core/src/main/java/com/shinku/reader/presentation/core/screens/InfoScreen.kt)
- [changelog_release.xml](../../app/src/main/res/raw/changelog_release.xml)
- [CHANGELOG.md](../../docs/CHANGELOG.md)

### 19 — Tablet & landscape layouts (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Tablet library | Unscored | Unscored | Existing adaptive tablet navigation rail retained; actual wide configuration checked. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); actual1280x800dp wide emulator configuration; sample reader state/existing emulator Library; real tablet and live reader pending; local `build/remaining-review/wide/boards19-home-navigation.png` |
| Landscape reader | Unscored | Unscored | Reader settings use a trailing panel in landscape at least600dp wide; phone sheet and Done preserved. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); actual1280x800dp wide emulator configuration; sample reader state/existing emulator Library; real tablet and live reader pending; local `build/remaining-review/wide/boards19-reader-controls.png` |
| Large-text phone | Unscored | Unscored | Wrapping/scrolling controls and48dp actions exercised at actual1.6x system font. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards17-remove-scopes.png` |

Source evidence:

- [AdaptiveSheet.kt](../../app/src/main/java/com/shinku/reader/presentation/components/AdaptiveSheet.kt)
- [ReaderSettingsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/settings/ReaderSettingsDialog.kt)
- [AdaptiveSheet.kt](../../presentation-core/src/main/java/com/shinku/reader/presentation/core/components/AdaptiveSheet.kt)

### 20 — Light, high-contrast & E-Ink (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Light theme | Unscored | Unscored | Existing light scheme retained; light recovery and metadata fixtures checked. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + partial runtime capture; light-theme metadata card sample only; whole-app light/contrast acceptance pending; local `build/remaining-review/light/boards21-source-metadata.png` |
| Accessible controls | Unscored | Unscored | Note formatting has localized labels, selected state/contrast and48dp targets; shared borders remain visible. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards22-notes-formatting.png` |
| E-Ink profile | Unscored | Unscored | Existing E-Ink profile chooses monochrome, flat shared surfaces, full outlines, no shared glow/theme animation or library backdrop. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards20-eink.png` |

Source evidence:

- [TachiyomiTheme.kt](../../app/src/main/java/com/shinku/reader/presentation/theme/TachiyomiTheme.kt)
- [MangaNotesTextArea.kt](../../app/src/main/java/com/shinku/reader/presentation/manga/components/MangaNotesTextArea.kt)
- [LibraryTab.kt](../../app/src/main/java/com/shinku/reader/ui/library/LibraryTab.kt)
- [SanctuaryTokens.kt](../../presentation-core/src/main/java/com/shinku/reader/presentation/core/theme/SanctuaryTokens.kt)

### 21 — Specialist source integrations (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| MangaDex follows | Unscored | Unscored | MangaDex follows explains account/source scope while retaining paging, auth and real error/display behavior. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Source integration settings | Unscored | Unscored | Existing source integration preferences retain shared Sanctuary cards and actual source capabilities. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Metadata editor | Unscored | Unscored | Read-only source metadata wraps in full-width cards and retains long-press copy; local override editor is separate. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards21-source-metadata.png` |

Source evidence:

- [MangaDexFollowsScreen.kt](../../app/src/main/java/com/shinku/reader/exh/md/follows/MangaDexFollowsScreen.kt)
- [MetadataViewScreen.kt](../../app/src/main/java/com/shinku/reader/exh/ui/metadata/MetadataViewScreen.kt)
- [SourceMetadataCard.kt](../../app/src/main/java/com/shinku/reader/presentation/components/SourceMetadataCard.kt)

### 22 — Extension details & merged manga (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Extension details | Unscored | Unscored | Extension details header uses a bordered card; long action labels stack. Existing trust/install flow retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Source preferences | Unscored | Unscored | Extension-owned native controls retained; nested titles wrap, empty icon spacing removed, dialog/incognito bindings preserved. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Merged manga & notes | Unscored | Unscored | Edit/merge dialogs use established shapes; notes keep existing autosave/limits with accessible selected formatting controls. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); sample component/callback fixture; real source/account/device state not certified; local `build/remaining-review/default/boards22-notes-formatting.png` |

Source evidence:

- [ExtensionDetailsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/ExtensionDetailsScreen.kt)
- [SourcePreferencesScreen.kt](../../app/src/main/java/com/shinku/reader/ui/browse/extension/details/SourcePreferencesScreen.kt)
- [EditMangaDialog.kt](../../app/src/main/java/com/shinku/reader/ui/manga/EditMangaDialog.kt)
- [EditMergedSettingsDialog.kt](../../app/src/main/java/com/shinku/reader/ui/manga/merged/EditMergedSettingsDialog.kt)
- [MangaNotesTextArea.kt](../../app/src/main/java/com/shinku/reader/presentation/manga/components/MangaNotesTextArea.kt)

### 23 — Batch tools & advanced organization (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Batch add | Unscored | Unscored | ShinKu Advanced tools links to the existing gallery batch screen. Multiline input, disabled empty Add, explicit confirmation and real validator/progress remain; no import occurs merely by opening. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); real settings-to-input navigation at1.0x/1.6x; prior confirmation callback fixture retained in dated report; no network import or whole-flow certification; local `build/phone-audit-route-review/1.0/boards23-batch-route.png` |
| Smart search | Unscored | Unscored | Actual original title shown while existing source matching runs; real match/fallback routing retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Advanced categories | Unscored | Unscored | Category/tag cards and localized ordering/edit/delete labels retain existing callbacks and disabled bounds. Source categories is also reachable from ShinKu Advanced tools; existing Library tag-sort route retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source + runtime captures (including actual 1.6× text); tag-order controls fixture plus real settings-to-source-category navigation at1.0x/1.6x; no category creation/edit/delete; local `build/remaining-review/default/boards23-organization.png` |

Source evidence:

- [BatchAddScreen.kt](../../app/src/main/java/com/shinku/reader/exh/ui/batchadd/BatchAddScreen.kt)
- [BatchInputConfirmation.kt](../../app/src/main/java/com/shinku/reader/presentation/components/BatchInputConfirmation.kt)
- [SmartSearchScreen.kt](../../app/src/main/java/com/shinku/reader/exh/ui/smartsearch/SmartSearchScreen.kt)
- [SourceCategoryListItem.kt](../../app/src/main/java/com/shinku/reader/presentation/category/components/sources/SourceCategoryListItem.kt)
- [SortTagListItem.kt](../../app/src/main/java/com/shinku/reader/presentation/category/components/genre/SortTagListItem.kt)
- [SettingsShinKuSettingsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsShinKuSettingsScreen.kt)

### 24 — Widgets & system surfaces (Partial — unscored; acceptance pending)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Updates widget | Unscored | Unscored | Actual cover titles exposed as descriptions; empty widget opens existing Updates shortcut; lock/cache gating retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Widget size variants | Unscored | Unscored | Empty host-size list safely falls back to one cover; largest reported size and existing row/column limits retained. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |
| Notifications & shortcuts | Unscored | Unscored | Existing notifications and launcher shortcuts retained as Android-controlled surfaces; no invented system layouts. | See dated report for supported adaptations and remaining live/device/host acceptance. No all-board visual certification. | Preserve existing source/platform/workflow capabilities; see ../boards-17-24-code-pass-2026-11-11.md. | Source only; runtime comparison pending |

Source evidence:

- [BaseUpdatesGridGlanceWidget.kt](../../presentation-widget/src/main/java/com/shinku/reader/presentation/widget/BaseUpdatesGridGlanceWidget.kt)
- [UpdatesWidget.kt](../../presentation-widget/src/main/java/com/shinku/reader/presentation/widget/components/UpdatesWidget.kt)
- [UpdatesMangaCover.kt](../../presentation-widget/src/main/java/com/shinku/reader/presentation/widget/components/UpdatesMangaCover.kt)
- [GlanceUtils.kt](../../presentation-widget/src/main/java/com/shinku/reader/presentation/widget/util/GlanceUtils.kt)

## Earlier concept panels

| Panel | Tag | Present | Remaining | Evidence |
| --- | --- | --- | --- | --- |
| Library / Continue Reading | 75% partial | Continue Reading hero, category tabs, cover grid and glass dock exist. | Align cover caption/card spacing and final hero/action proportions. | Source + partial runtime capture; [LibraryTab.kt](../../app/src/main/java/com/shinku/reader/ui/library/LibraryTab.kt) |
| Manga details | 55% partial | Cover/header/actions and chapter functions remain, with wrapped labels. | Match unified header/action cards, metadata and chapter action composition. | Source only; runtime comparison pending; [MangaScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/manga/MangaScreen.kt) |
| More / complete statistics | 75% partial | Live profile/XP/statistics, command cards and dock clearance exist. | Align compact metric tiles, shortcuts and command-card hierarchy; preserve actual data. | Source + partial runtime capture; [MoreScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/MoreScreen.kt) |
| ShinKu settings hub | 80% partial | Four primary cards, separate Advanced, searchable routing and inline info footer exist. | Match visible search field, section label and distinct Advanced row/divider styling. | Source + partial runtime capture; [SettingsShinKuSettingsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsShinKuSettingsScreen.kt) |
| AI / translation setup | 60% partial | Masked keys/reveal, real connection state and key-specific Gemini model discovery exist. | Consolidate provider/model/key/test and translation groups into the approved hierarchy. | Source + partial runtime capture; [ShinKuSettingsSectionScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/ShinKuSettingsSectionScreen.kt) |
| Reader controls | 60% partial | Reading/Display/Translation tabs and supported control routing exist. | Same grouped icon-row/header/primary-button visual gaps as boards 06–08; not independent work. | Source + partial runtime capture; [ReaderSettingsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/settings/ReaderSettingsDialog.kt) |

Reader controls overlaps boards 06–08. Earlier concepts are not additional independent delivery units; do not add these percentages into a project-wide total.

## Next implementation order

1. Board 05: verify the latest artifact and final visual acceptance on the owner’s phone; see the per-part log.
2. Finish board 15 and remaining confirmed gaps in boards 08 and 12-14.
3. Boards 02–03 and 09–11: finish the page compositions around existing functional flows.
4. Continue boards 16-24 from existing Sanctuary components and briefs, without new images. Capture actual screen states for acceptance.

A percentage rises only after the listed gap is implemented and evidence is updated. 100% requires a complete supported-scope comparison, documented adaptations and no known visual gaps.

The initial audit was documentation-only. The subsequent authorized board-05 implementation is recorded in [its progress log](mockups/05-implementation-progress.md), with new code, runtime checks and APK verification. Phone installation and final visual acceptance remain separate. Tests are functional validation, not pixel-similarity measurements.
