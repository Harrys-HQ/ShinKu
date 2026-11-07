# ShinKu Mockup UI/UX Handoff Strategy & Execution Playbook (For Model 3.8)

Updated: 2026-11-07
Completed Baseline: **Boards 01–05** are at **90%–95%** (compiled & verified in `devRelease`). **Board 14** canonical image is generated at `docs/design/mockups/14-maintenance-v1.png`.

---

## 1. Non-Negotiable Rules for Model 3.8 (Read Before Any Edit)

1. **Image-First Inspection (Never Guess From Text Alone):**
   - Before editing any screen for Board `XX`, call `read_file` on its canonical image (`docs/design/mockups/XX-...-v1.png`) AND its prompt (`docs/design/mockups/XX-prompt.txt`).
   - Compare each of the 3 panels in the PNG against the exact target Kotlin files listed in Section 3 below.

2. **Strict Sanctuary Design Tokens (Zero Hardcoded Primitives):**
   - **Cards / Containers:** Use `Surface` with:
     - `shape = SanctuaryShapes.deckCard` (or `SanctuaryShapes.medium` for compact pills/chips, `SanctuaryShapes.tag` for inline badges, `SanctuaryShapes.pill` for progress bars)
     - `color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)`
     - `border = SanctuaryBrushes.subtleBorder()`
   - **Leading Icon Badges:** Use a `36.dp`–`40.dp` `Surface` with `shape = SanctuaryShapes.medium`, `color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)`, and an `18.dp`–`20.dp` `Icon` tinted `MaterialTheme.colorScheme.primary`.
   - **Selected vs. Unselected Filter Pills:**
     - Selected: `color = MaterialTheme.colorScheme.primary`, `contentColor = MaterialTheme.colorScheme.onPrimary`, `border = null`
     - Unselected: `color = MaterialTheme.colorScheme.surfaceColorAtElevation(SanctuaryElevation.subtle)`, `contentColor = MaterialTheme.colorScheme.onSurfaceVariant`, `border = SanctuaryBrushes.subtleBorder()`
   - **Reference Exemplars (Copy Patterns From Here):**
     - Pill tabs: `app/src/main/java/com/shinku/reader/presentation/components/TabbedScreen.kt`
     - Grouped cards & headers: `app/src/main/java/com/shinku/reader/feature/migration/config/MigrationConfigScreenSheet.kt`
     - List cards & inline search/action pills: `app/src/main/java/com/shinku/reader/presentation/updates/UpdatesUiItem.kt`, `app/src/main/java/com/shinku/reader/presentation/history/HistoryScreen.kt`
     - Filter/category sheets (Board 05 exemplar): `app/src/main/java/com/shinku/reader/presentation/library/components/LibraryFilterItem.kt`, `app/src/main/java/com/shinku/reader/presentation/category/CategoryScreen.kt`

3. **Anti-Monolith & Overflow Mandate:**
   - Keep UI composable files under **350–400 lines**. If a file grows past 400 lines, split item/row composables into a sibling `*Items.kt` or `components/*.kt` file (see `MigrationListScreenContent.kt` + `MigrationListItems.kt`).
   - All interactive targets must be `>= 44.dp–48.dp` (`Modifier.heightIn(min = 48.dp)`).
   - Never force single-line text where it can clip at `1.6×` font scale; use `FlowRow` or multi-line `maxLines = 2..3` with `TextOverflow.Ellipsis`.

4. **Supported-Scope Discipline & Test Preservation:**
   - Do **not** invent fake metrics, unsupported toggles, or hardcoded sample manga/counts.
   - Check `app/src/androidTest/java/com/shinku/reader/ui/MockupControlsTest.kt` before renaming any existing control label or tag — preserve exact strings asserted by tests.

5. **Fast-Loop Verification Gate:**
   - After completing a batch of boards, always run:
     ```powershell
     .\gradlew.bat compileDevDebugKotlin compileDevReleaseKotlin --console=plain
     ```
   - Only run full `.\gradlew.bat assembleDevRelease --build-cache --parallel --quiet --console=plain` after Kotlin compilation succeeds with 0 errors.

---

## 2. Batching Strategy (How to Divide Work Across Sessions)

To keep context small, fast, and 100% accurate on Model 3.8, execute **3 boards per session**:

- **Batch A (Reader & Translation UI):** Boards **06, 07, 08**
- **Batch B (Onboarding, Backup/Sync & Tracking/Security):** Boards **09, 10, 11**
- **Batch C (Core Settings, Preferences & Maintenance):** Boards **12, 13, 14**
- **Batch D (Earlier Concept Polish):** `MangaScreen.kt` (Manga details, 55% → 95%), `ShinKuSettingsSectionScreen.kt` (AI/Translation setup, 60% → 95%), `LibraryTab.kt` & `MoreScreen.kt` (75% → 95%)
- **Batch E (Remaining Image Generation + Implementation):** Boards **15–24** (in 2–3 board chunks)

---

## 3. Board-by-Board File & Task Map (Boards 06–14)

### Batch A: Boards 06–08 (Reader & Translation)

#### Board 06 — Reading & chapter navigation (`docs/design/mockups/06-reader-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/ui/reader/ReaderActivity.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageIndicator.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/ChapterNavigator.kt` (and chapter/page sheet composables under `presentation/reader/`)
- **Required Changes:**
  1. **Paged reader (Panel 1):** Unify the lower reader control deck with rounded Sanctuary surface styling (`SanctuaryShapes.deckCard`), clean chapter prev/next buttons, page slider, and action row (`Chapters`, `Translation`, `Display`, `Settings`).
  2. **Webtoon reader (Panel 2):** Style the floating compact page pill and ensure loading vs. retry states over vertical pages use distinct Sanctuary cards.
  3. **Chapters & pages sheet (Panel 3):** Add the `"Navigate"` sheet header, Sanctuary pill tabs (`Chapters` / `Pages`), highlighted `"Current chapter"` row badge, and numbered page chip grid.

#### Board 07 — Translation in the reader (`docs/design/mockups/07-translation-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/reader/dialog/AreaOcrCropOverlay.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/dialog/BubbleInspectorDialog.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/dialog/BatchTranslationProgressDialog.kt`
- **Required Changes:**
  1. **Area selection (Panel 1):** Style the top instruction card (`"Select area to translate"` + subtitle) and bottom action bar (`Cancel` + primary `Translate selection`) using Sanctuary surfaces and 48.dp touch targets while keeping 1:1 viewport crop coordinates intact.
  2. **Inspect bubbles (Panel 2):** Convert/style `BubbleInspectorDialog` into a Sanctuary sheet/card layout with a header (`"Inspect translated bubble"`), bubble index navigation (`Previous` / `Next`), distinct `Original text` and `Translated text` Sanctuary cards with copy actions, and page-level retry.
  3. **Chapter translation (Panel 3):** Style `BatchTranslationProgressDialog` as a grouped Sanctuary progress sheet with progress bar, page counter, `Show translated overlay` switch card, `Run in background` primary button, and `Cancel` button.

#### Board 08 — Reader settings & page actions (`docs/design/mockups/08-reader-controls-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/reader/settings/ReaderSettingsDialog.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/settings/ReadingModePage.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/settings/GeneralPage.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/settings/ColorFilterPage.kt`
  - `app/src/main/java/com/shinku/reader/presentation/reader/ReaderPageActionsDialog.kt`
- **Required Changes:**
  1. **Display controls (Panel 1):** Add sheet heading, Sanctuary pill tabs (`Reading`, `Display`, `Translation`), icon-led mode/background selector cards, grouped brightness/filter sliders, and a primary `Done` button.
  2. **Translation actions (Panel 2):** Group translation controls into Sanctuary cards with leading icon badges (`Translate page`, `Select area`, `Translate chapter`, `Inspect bubbles`, `Show overlay` switch) and keep the scoped `Clear chapter translation cache` confirmation intact.
  3. **Page actions (Panel 3):** Style `ReaderPageActionsDialog` with grouped Sanctuary action rows (`Save image`, `Share page`, `Copy image`, `Set as cover`) above the AI/translation tools.

---

### Batch B: Boards 09–11 (Onboarding, Backups & Tracking)

#### Board 09 — Getting started (`docs/design/mockups/09-onboarding-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/onboarding/OnboardingScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/onboarding/StorageStep.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/onboarding/ThemeStep.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/onboarding/GuidesStep.kt`
- **Required Changes:**
  1. **Welcome (Panel 1):** Add hero header card + 3 Sanctuary benefit rows with leading icon badges, primary `Get started` button, and `Restore from backup` secondary action.
  2. **Storage & permissions (Panel 2):** Wrap storage folder selection and permission items in rounded Sanctuary cards with status pills (`Required` / `Configured`).
  3. **Theme & finish (Panel 3):** Style theme preview cards and the final optional AI / guides completion cards with Sanctuary surfaces.

#### Board 10 — Backups, restore & sync (`docs/design/mockups/10-backup-sync-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/CreateBackupScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/data/RestoreBackupScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDataScreen.kt`
- **Required Changes:**
  1. **Create backup (Panel 1):** Add top Sanctuary intro card, group backup scope switches/checkboxes into rounded Sanctuary cards, and keep the privacy/safety notice card + bottom `Create backup` button.
  2. **Restore preview (Panel 2):** Wrap selected backup file summary, restore options, and warning/progress notes in rounded Sanctuary cards.
  3. **Sync settings (Panel 3):** Style the cloud sync hero status card (`Last synced`, `Sync now` button, and service configuration rows) in `SettingsDataScreen.kt`.

#### Board 11 — Tracking & privacy (`docs/design/mockups/11-tracking-privacy-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsTrackingScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/track/TrackerSearch.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsSecurityScreen.kt`
- **Required Changes:**
  1. **Trackers (Panel 1):** Add intro banner card, style tracker service rows with `Connected` / `Connect` pills, and group progress-sync switches.
  2. **Link a title (Panel 2):** Style `TrackerSearch` result cards with cover thumbnail, full wrapping title, status/year metadata pills, and explicit bottom `Link title` confirmation bar.
  3. **Security & privacy (Panel 3):** Add intro banner card and group app-lock / incognito / privacy switches into Sanctuary cards.

---

### Batch C: Boards 12–14 (Settings, Preferences & Maintenance)

#### Board 12 — Core settings & appearance (`docs/design/mockups/12-settings-appearance-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsMainScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsAppearanceScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsLibraryScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/PreferenceScreen.kt`
- **Required Changes:**
  1. **Settings root (Panel 1):** Add inline search pill bar at the top of `SettingsMainScreen`, feature the prominent `ShinKu settings` hero card at the top, and group standard settings into rounded Sanctuary cards with tinted icon containers.
  2. **Appearance (Panel 2):** Upgrade the theme preview card in `SettingsAppearanceScreen` to show a mini Sanctuary cover + `Resume` preview card alongside the theme mode (`System` / `Light` / `Dark`) and palette selectors.
  3. **Library settings (Panel 3):** Add intro card and group display/update/category preference blocks cleanly.

#### Board 13 — Reader, browse & download preferences (`docs/design/mockups/13-preferences-detail-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsReaderScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsBrowseScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsDownloadScreen.kt`
- **Required Changes:**
  1. Add top Sanctuary intro cards and clean icon-led preference grouping across `SettingsReaderScreen`, `SettingsBrowseScreen`, and `SettingsDownloadScreen`.

#### Board 14 — Performance & maintenance (`docs/design/mockups/14-maintenance-v1.png`)
- **Target Files:**
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/ShinKuSettingsSectionScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/more/settings/screen/SettingsAdvancedScreen.kt`
  - `app/src/main/java/com/shinku/reader/presentation/browse/SourceHealthScreen.kt` (or existing source-health / storage / database maintenance composables)
- **Required Changes (Matching the 4 user-approved refinements in `14-maintenance-v1.png`):**
  1. **Performance & reader enhancement (Panel 1):**
     - Section title: `"Performance mode"` (NOT `"Unified mode selector"`) with segmented choices (`Standard`, `Balanced`, `Battery saver`) and plain-language summary.
     - `"Enhanced scaling"` toggle with subtitle `"Sharper upscaled pages; may be slower on older devices"` (do NOT include `"with low overhead"`).
  2. **Source health & Smart Source Switch (Panel 2):**
     - Place `"Source health"` heading + `"Checked on request"` pill **above** the cards so every source card stands alone in its own `SanctuaryShapes.deckCard` container.
     - Include check timestamps in each card's secondary line (e.g., `"Last check succeeded · 10m ago"`, `"Last check failed · 10m ago"`, `"Not checked yet"`).
  3. **Storage & database tools (Panel 3):**
     - Separate safe cache actions (`Clear chapter cache`, `Clear cover cache`, `Clean up orphaned files`) from database maintenance (`Reindex library`, `Verify database integrity`, `Reset reader flags`).
