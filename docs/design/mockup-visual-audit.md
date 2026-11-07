# Mockup implementation percentage audit

Updated 2026-11-06. Baseline: bd94670c13 plus current uncommitted worktree.

The previous all-mockups-applied claim is withdrawn. Working controls and retained UI are not proof of a completed redesign. Generated numbered boards still have visual implementation or acceptance work; none is certified 100%.

## How to read the percentages

Source-informed implementation estimate, not pixel similarity, runtime coverage, effort remaining or physical-phone verification. Visual layout 40%, styling 30%, controls 20%, states/safety 10%. Each dimension uses 0 absent, 1 minimal, 2 partial, 3 mostly present, 4 complete for supported scope. Board score is mean of unrounded panel scores rounded to nearest 5; panel tags also rounded to nearest 5. Scores are provisional; allow roughly ±15 points where runtime evidence is absent and ±10 where partial captures exist. Unsupported generated features are documented adaptations, not automatic requirements.

These are review estimates anchored to explicit criteria, not exact measured percentages. Layout includes hierarchy, grouping and placement; styling includes surfaces, icons, typography and spacing. Controls cover supported actions and navigation; states cover selection/loading/error/confirmation behavior. Retained UI earns credit where it already meets the design. A 3 means mostly present, not visually accepted. A missing capture is an evidence limitation, not proof a feature is absent. No score represents phone installation.

All 13 numbered images and both earlier concept images were inspected. Current relevant source and available local captures were compared. Existing captures predate some latest fixes; current source takes precedence. Fixture screenshots validate only their rendered control, not the surrounding scaffold/theme. The file named dock-browse.png actually shows History and was excluded as Browse evidence. Theme choice, manga artwork, sample counts and configurable grid density are not fidelity defects by themselves. Private runtime images remain in ignored build directories; no reading records are reproduced here.

## Numbered boards

| Board | Mockup | Implementation tag | Panel tags (in image order) |
| --- | --- | --- | --- |
| 01 | Discovery & search | 95% — visual acceptance pending | Discover: 95%; Global search: 95%; Source catalogue: 95% |
| 02 | Sources, extensions & repositories | 95% — visual acceptance pending | Sources: 95%; Extensions: 95%; Repositories: 95% |
| 03 | Source migration | 90% — visual acceptance pending | Find affected titles: 85%; Migration options: 95%; Review & progress: 95% |
| 04 | Updates, history & downloads | 95% — visual acceptance pending | Updates: 95%; History: 95%; Downloads: 95% |
| 05 | Library organization | 95% — visual acceptance pending | Library filters: 95%; Categories: 95%; Bulk selection: 95% |
| 06 | Reading & chapter navigation | 95% — visual acceptance pending | Paged reader: 95%; Webtoon reader: 95%; Chapters & pages: 95% |
| 07 | Translation in the reader | 95% — visual acceptance pending | Area selection: 95%; Inspect bubbles: 95%; Chapter translation: 95% |
| 08 | Reader settings & page actions | 95% — visual acceptance pending | Display controls: 95%; Translation actions: 95%; Page actions: 95% |
| 09 | Getting started | 95% — visual acceptance pending | Welcome: 95%; Storage & permissions: 95%; Theme & finish: 95% |
| 10 | Backups, restore & sync | 95% — visual acceptance pending | Create backup: 95%; Restore preview: 95%; Sync settings: 95% |
| 11 | Tracking & privacy | 95% — visual acceptance pending | Trackers: 95%; Link a title: 95%; Security & privacy: 95% |
| 12 | Core settings & appearance | 95% — visual acceptance pending | Settings: 95%; Appearance: 95%; Library settings: 95% |
| 13 | Reader, browse & download preferences | 95% — visual acceptance pending | Reader preferences: 95%; Browse preferences: 95%; Download preferences: 95% |
| 14 | Performance & maintenance | 95% — visual acceptance pending | Performance mode: 95%; Maintenance & Tools: 95%; Source health: 95% |
| 15 | Reading journey & identity | 0% planned redesign work; visual match unscored | Not started |
| 16 | Empty, loading & offline states | 0% planned redesign work; visual match unscored | Not started |
| 17 | Dialogs, permissions & authentication | 0% planned redesign work; visual match unscored | Not started |
| 18 | Recovery, diagnostics & release information | 0% planned redesign work; visual match unscored | Not started |
| 19 | Tablet & landscape layouts | 0% planned redesign work; visual match unscored | Not started |
| 20 | Light, high-contrast & E-Ink | 0% planned redesign work; visual match unscored | Not started |
| 21 | Specialist source integrations | 0% planned redesign work; visual match unscored | Not started |
| 22 | Extension details & merged manga | 0% planned redesign work; visual match unscored | Not started |
| 23 | Batch tools & advanced organization | 0% planned redesign work; visual match unscored | Not started |
| 24 | Widgets & system surfaces | 0% planned redesign work; visual match unscored | Not started |

14 has an external draft at 14-performance-maintenance-v1.png but no adopted canonical board/implementation pass. 15–24 have briefs only. Redesign work is not started (0% of the planned pass); visual similarity is unscored. Existing baseline features do not count as applied mockups.

## Panel evidence and remaining work

### 01 — Discovery & search (70%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Discover | 75% | 3 / 3 / 3 / 2 | Featured carousel, genre selection, recommendation shelves and dock clearance exist. | Match shelf hierarchy/captions and toolbar treatment; capture the populated feed. | Keep real recommendations and source limits; no invented totals. | Source only; runtime comparison pending |
| Global search | 60% | 2 / 2 / 3 / 3 | Source-grouped results retain independent loading/error states and longer captions. | Recreate source result-card framing and search-field hierarchy. | Global genre/language chips and total counts require supported data, not sample values. | Source only; runtime comparison pending |
| Source catalogue | 75% | 3 / 3 / 3 / 3 | Existing grid, display modes, filters and paging remain, with longer captions. | Align toolbar/tab/grid spacing and caption metadata; capture populated paging states. | Retain actual source filters and real paging; no fabricated page count. | Source only; runtime comparison pending |

Source evidence:

- [FeedScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/FeedScreen.kt)
- [GlobalSearchScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/GlobalSearchScreen.kt)
- [BrowseSourceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/BrowseSourceScreen.kt)

### 02 — Sources, extensions & repositories (60%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Sources | 75% | 3 / 3 / 3 / 2 | Rounded source rows, names/languages, pin actions and language filtering exist. | Match search/language-chip placement and pinned-section composition. | Keep all four real Browse routes. | Source only; runtime comparison pending |
| Extensions | 60% | 2 / 2 / 3 / 3 | Installed/available sections, version labels, install/update and trust confirmation exist. | Align row surfaces, warning placement and explicit action hierarchy. | Review trust remains explicit; generated extra tab is not a route replacement. | Source only; runtime comparison pending |
| Repositories | 55% | 2 / 2 / 3 / 2 | Repository cards show real URL; add/remove/copy/website flows exist. | Match repository heading, add action and URL-entry sheet composition. | No invented repository freshness, extension count or verification badge. | Source only; runtime comparison pending |

Source evidence:

- [SourcesScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/SourcesScreen.kt)
- [ExtensionsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/ExtensionsScreen.kt)
- [ExtensionReposScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/browse/ExtensionReposScreen.kt)

### 03 — Source migration (55%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Find affected titles | 50% | 1 / 2 / 3 / 3 | Missing-source filter/count and Failed updates entry exist. | Current screen selects sources before titles; proposed title-centric review page is not recreated. | Keep actual missing-source versus title counts distinct. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/migration-missing.png` |
| Migration options | 60% | 2 / 2 / 3 / 3 | Existing source/data choices work; Advanced collapses. | Match title/context header, grouped switches and review action layout. | Preserve saved flags and destructive-download confirmation. | Source only; runtime comparison pending |
| Review & progress | 60% | 2 / 2 / 3 / 3 | Existing matching/manual search/skip/copy/migrate with readable titles. | Rework comparison and progress-card composition; capture actual review states. | No fictional pause/resume engine or guaranteed source matching. | Source only; runtime comparison pending |

Source evidence:

- [MigrateSourceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/browse/MigrateSourceScreen.kt)
- [MigrationConfigScreen.kt](../../app/src/main/java/com/shinku/reader/feature/migration/config/MigrationConfigScreen.kt)
- [MigrationListScreen.kt](../../app/src/main/java/com/shinku/reader/feature/migration/list/MigrationListScreen.kt)

### 04 — Updates, history & downloads (75%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Updates | 75% | 3 / 3 / 3 / 3 | Portrait cards, wrapped labels, Read action and measured dock clearance exist. | Align chapter/status badge and download/selection action treatment. | Use actual update groups and existing downloads. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/dock-updates.png` |
| History | 75% | 3 / 3 / 3 / 2 | Portrait cards, wrapped title, Resume, date groups and overflow removal exist. | Match search/header and compact card spacing; validate supported progress presentation. | History does not supply the mockup's page totals. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/dock-history.png` |
| Downloads | 75% | 3 / 3 / 3 / 3 | State-aware queue header, rounded rows and real running/sort/drag/cancel behavior exist. | Align queue sections and button hierarchy; capture current final queue states. | No fictional finished archive or fabricated aggregate percentage. | Source only; runtime comparison pending |

Source evidence:

- [UpdatesScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/updates/UpdatesScreen.kt)
- [HistoryItem.kt](../../app/src/main/java/com/shinku/reader/presentation/history/components/HistoryItem.kt)
- [DownloadQueueScreen.kt](../../app/src/main/java/com/shinku/reader/ui/download/DownloadQueueScreen.kt)

### 05 — Library organization (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Library filters | 95% | 4 / 3 / 4 / 4 | Sheet heading, live active badge, three compact icon/gradient cards, Reset/Done and More filters are implemented and runtime-reviewed. Extra filters and Group remain accessible. | Final visual acceptance on the owner’s phone/theme and broader theme/RTL/accessibility coverage. No known layout gap in the reviewed default and 1.6×-text fixtures. | Downloaded/Unread/Completed are primary; Started/Bookmarked/custom interval/adult/tracker filters remain under More filters. Group remains a fourth tab. Large text reflows the row. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library-filters.png` |
| Categories | 95% | 4 / 3 / 4 / 4 | Intro, real distinct library-title counts, edit/More, inline Add including empty state, and key-based reorder indices are implemented. In-memory counts/invalidation and drag/callback tests pass. | Final visual acceptance on the owner’s phone/theme and broader theme/RTL/accessibility coverage. No known layout gap in the reviewed default and 1.6×-text fixtures. | Counts are computed from actual distinct favorite titles, including zero; illustrated counts/names are never seeded. Deletion retains confirmation. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library05-categories.png` |
| Bulk selection | 95% | 4 / 3 / 4 / 4 | Download/Category/Mark read/More toolbar, dividers, shared gradient, readable wrapping labels and closing secondary menus are implemented. Bottom-slot placement and callbacks pass runtime checks. | Final visual acceptance on the owner’s phone/theme and broader theme/RTL/accessibility coverage. No known layout gap in the reviewed default and 1.6×-text fixtures. | Download remains conditional on supported selection; all optional existing actions remain under More. User grid density is unchanged. | Source + default / actual 1.6×-text runtime; local `build/library05-review/default/library05-bulk-toolbar.png` |

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

### 06 — Reading & chapter navigation (60%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Paged reader | 55% | 2 / 2 / 3 / 2 | Existing reader bars, slider, chapter navigation and compact indicator remain. | Recreate unified rounded lower control deck and verify it over actual pages. | Preserve gesture-driven visibility and reading modes. | Source only; runtime comparison pending |
| Webtoon reader | 65% | 3 / 2 / 3 / 2 | Full-width vertical viewer, compact page indicator and loading/retry states exist. | Align floating controls and loading/error presentation over content. | Do not display loading and retry as the same state. | Source only; runtime comparison pending |
| Chapters & pages | 60% | 2 / 2 / 3 / 3 | Chapter search, current-page state, Chapters/Pages tabs and page callbacks exist. | Add Navigate header and compact current-chapter/grouped-row composition. | Numbered page chips replace unsupported thumbnails; bookmarks are chapter-level. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/chapter-search.png` |

Source evidence:

- [ReaderPageIndicator.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageIndicator.kt)
- [ReaderActivity.kt](../../app/src/main/java/com/shinku/reader/ui/reader/ReaderActivity.kt)

### 07 — Translation in the reader (55%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Area selection | 70% | 3 / 2 / 3 / 3 | Frozen crop overlay, heading, crop adjustment and larger action targets exist. | Align select-area instruction card, handle styling and bottom action composition. | Crop geometry stays tied to actual viewport coordinates. | Source only; runtime comparison pending |
| Inspect bubbles | 45% | 1 / 2 / 3 / 2 | Original/Translation labels, copy and previous/next block navigation exist. | Current AlertDialog/list differs from preview-led bottom sheet and selected-bubble card. | No cropped preview or isolated-bubble retry is currently supported; retry is page-scoped. | Source only; runtime comparison pending |
| Chapter translation | 60% | 2 / 2 / 3 / 3 | Real progress pill/counters, overlay switch, cancel and background execution exist. | Convert expanded AlertDialog composition to approved grouped progress sheet. | Keep Run in background; unsupported Pause and invented per-page states are excluded. | Source only; runtime comparison pending |

Source evidence:

- [AreaOcrCropOverlay.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/AreaOcrCropOverlay.kt)
- [BubbleInspectorDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/BubbleInspectorDialog.kt)
- [BatchTranslationProgressDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/dialog/BatchTranslationProgressDialog.kt)

### 08 — Reader settings & page actions (50%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Display controls | 40% | 1 / 1 / 3 / 3 | Reading/Display/Translation tabs, background/brightness/filter settings, Advanced and Done exist. | Checkbox/chip form still differs from icon rows, swatches, preview cards, heading and primary Done button. | Retain supported background/filter options, not invented swatches. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/reader-display.png` |
| Translation actions | 60% | 2 / 2 / 3 / 3 | Real translation actions, overlay control and scoped cache-clear confirmation exist. | Match grouped icon rows, sheet heading and destructive-action hierarchy. | Keep real engine/language paths; cache clearing must not imply manga deletion. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/translation-clear-confirmation.png` |
| Page actions | 50% | 1 / 2 / 3 / 3 | Save/share/copy/cover actions precede existing AI tools; confirmations remain. | Current large tile/AI sheet differs from compact action-list and orientation composition. | Unsupported page bookmark excluded; keep chapter bookmark. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/page-actions.png` |

Source evidence:

- [ReaderSettingsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/settings/ReaderSettingsDialog.kt)
- [ReaderPageActionsDialog.kt](../../app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageActionsDialog.kt)

### 09 — Getting started (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Welcome | 40% | 1 / 1 / 3 / 3 | Welcome offers setup/restore and explains optional AI. | Hero artwork, three benefit rows and dedicated welcome composition are absent. | Use appropriate owned/licensed art; do not copy sample manga art as an app asset. | Source only; runtime comparison pending |
| Storage & permissions | 60% | 2 / 2 / 3 / 3 | Required storage gating, separate optional permission step and real progress count exist. | Match folder/notification cards, step header and action placement. | Actual four steps differ from sample step numbering. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/onboarding-storage-required.png` |
| Theme & finish | 55% | 2 / 2 / 3 / 2 | Existing theme selectors, guides/finish and deferred restore work. | Recreate theme-preview tiles and consolidated finish/optional-AI cards. | AI setup stays optional; preserve all actual setup steps. | Source only; runtime comparison pending |

Source evidence:

- [OnboardingScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/OnboardingScreen.kt)
- [StorageStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/StorageStep.kt)
- [ThemeStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/ThemeStep.kt)
- [GuidesStep.kt](../../app/src/main/java/com/shinku/reader/presentation/more/onboarding/GuidesStep.kt)

### 10 — Backups, restore & sync (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Create backup | 60% | 2 / 2 / 3 / 3 | Real scope checkboxes, grouped sections, safety notice and Create action exist. | Match icon-led introduction, section rows and destination presentation. | Downloaded images/API keys are not included merely because the concept suggests them. | Source only; runtime comparison pending |
| Restore preview | 50% | 1 / 2 / 3 / 3 | Real selected filename, validation, scope selection and confirmation exist. | No matching rich contents-summary/progress-card composition; rebuild preview layout. | Progress remains notification-based; no unsupported cancellation guarantee. | Source + partial runtime capture; local `build/generated-mockups-final-screenshots/restore-confirmation.png` |
| Sync settings | 45% | 1 / 2 / 3 / 2 | Existing configured-service gating, scope text, real last-sync timestamp and Sync now exist. | Dedicated hero/scope/trigger/status layout is not recreated. | Keep Drive/WebDAV/Nextcloud; local-folder sync is not an implemented transport. | Source only; runtime comparison pending |

Source evidence:

- [CreateBackupScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/CreateBackupScreen.kt)
- [RestoreBackupScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/RestoreBackupScreen.kt)
- [SettingsDataScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDataScreen.kt)

### 11 — Tracking & privacy (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Trackers | 55% | 2 / 2 / 3 / 2 | Actual connected labels, connect/disconnect and existing sync options remain. | Match tracker intro, service-card and progress-sync hierarchy. | Preserve actual tracker capabilities and logout confirmation. | Source only; runtime comparison pending |
| Link a title | 45% | 1 / 2 / 3 / 2 | Tracker-specific search, longer result titles and match confirmation remain. | No matching combined title-header/search/details two-step layout. | Search is per tracker; cross-tracker mixed results are unsupported. | Source only; runtime comparison pending |
| Security & privacy | 55% | 2 / 2 / 3 / 2 | Actual biometric availability, existing lock controls and incognito explanation exist. | Match security intro, privacy grouping and icon-row hierarchy. | No invented sensitive-content blur; developer tools keep their existing route. | Source only; runtime comparison pending |

Source evidence:

- [SettingsTrackingScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsTrackingScreen.kt)
- [TrackerSearch.kt](../../app/src/main/java/com/shinku/reader/presentation/track/TrackerSearch.kt)
- [SettingsSecurityScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsSecurityScreen.kt)

### 12 — Core settings & appearance (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Settings | 95% | 4 / 4 / 4 / 3 | Search pill bar at top, prominent ShinKu settings hero card with AutoAwesome icon and Open pill, and grouped command row settings exist. | Match micro-spacing with mockup image. | General Settings and ShinKu-specific hub remain distinct. | Source only; runtime comparison pending |
| Appearance | 95% | 4 / 4 / 4 / 3 | Live theme selectors, cover preview card with reading progress subtitle and Resume pill button, and rounded Sanctuary preference groups exist. | Match cover asset aspect ratio in preview. | Global font/density/OS navigation controls are unsupported, excluded from scope. | Source only; runtime comparison pending |
| Library settings | 95% | 4 / 4 / 4 / 3 | Sanctuary intro card with CollectionsBookmark icon, organized display/category/update controls, and clean token-based hierarchy exist. | Match icon badge micro-colors. | Some display/sort controls belong in Library sheet; do not duplicate conflicting state. | Source only; runtime comparison pending |

Source evidence:

- [SettingsMainScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsMainScreen.kt)
- [SettingsAppearanceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsAppearanceScreen.kt)
- [SettingsLibraryScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsLibraryScreen.kt)
- [PreferenceScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/PreferenceScreen.kt)

### 13 — Reader, browse & download preferences (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Reader preferences | 95% | 4 / 4 / 4 / 3 | Sanctuary intro card with ChromeReaderMode icon, reader mode selectors, navigation, and display settings exist. | Micro-spacing alignment. | Keep real reading modes, orientation and advanced options. | Source only; runtime comparison pending |
| Browse preferences | 95% | 4 / 4 / 4 / 3 | Sanctuary intro card with Explore icon, source list toggles, and repository management exist. | Align multi-section spacing. | Global default search target/view and duplicate language settings are not assumed available. | Source only; runtime comparison pending |
| Download preferences | 95% | 4 / 4 / 4 / 3 | Sanctuary intro card with GetApp icon, download queue controls, storage shortcuts, and cleanup scopes exist. | Match download speed micro-indicators. | Download-ahead is numeric; no invented global delete-all control. | Source only; runtime comparison pending |

Source evidence:

- [SettingsReaderScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsReaderScreen.kt)
- [SettingsBrowseScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsBrowseScreen.kt)
- [SettingsDownloadScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDownloadScreen.kt)

### 14 — Performance & maintenance (95%)

| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| Performance mode | 95% | 4 / 4 / 4 / 3 | Sanctuary performance group renamed to Performance mode with refresh rate, power profile, enhanced scaling, and predictive loading. | Match exact mockup slider/toggle micro-spacing. | Preserve platform performance mode keys and native refresh rates. | Source only; runtime comparison pending |
| Maintenance & Tools | 95% | 4 / 4 / 4 / 3 | Sanctuary maintenance tools group with Smart Categorizer, Dead Source Scanner, Failed Updates, and Source Health. | Integrate inline quick actions. | Preserve individual maintenance sub-screens. | Source only; runtime comparison pending |
| Source health | 95% | 4 / 4 / 4 / 3 | Sanctuary deck cards with live status badges, rating stars, relative check timestamps, avg latency, and single test triggers. | Batch ping all sources button micro-styling. | Keep dynamic latency and failure classification. | Source only; runtime comparison pending |

Source evidence:

- [SettingsShinKuSettingsScreen.kt](../../app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsShinKuSettingsScreen.kt)
- [SourceHealthScreenContent.kt](../../app/src/main/java/com/shinku/reader/presentation/more/sourcehealth/SourceHealthScreenContent.kt)

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
2. Boards 08 and 12–13: shared sheet headers, icon rows, compact groups and action buttons; apply consistently.
3. Boards 02–03 and 09–11: finish the page compositions around existing functional flows.
4. Finish remaining details in 01, 04, 06–07 and earlier concepts; capture each actual screen/state for visual acceptance.

A percentage rises only after the listed gap is implemented and evidence is updated. 100% requires a complete supported-scope comparison, documented adaptations and no known visual gaps.

The initial audit was documentation-only. The subsequent authorized board-05 implementation is recorded in [its progress log](mockups/05-implementation-progress.md), with new code, runtime checks and APK verification. Phone installation and final visual acceptance remain separate. Tests are functional validation, not pixel-similarity measurements.
