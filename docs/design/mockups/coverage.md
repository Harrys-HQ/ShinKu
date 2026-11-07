# ShinKu UI mockup coverage and implementation checklist

Inventory date: 2026-11-03. 24 new boards / 72 planned panels, plus the six earlier panels.

**Incomplete rendering:** 13 new boards are generated; 11 still await generation. Historical quota observation: image generation reached its usage limit on 2026-11-05. Tool-estimated reset was 2026-11-06 09:45:35 Asia/Shanghai (tool estimate); quota has not been rechecked during this audit. Resume at board 14. See the [mockup checklist](./checklist.md) for separate image, implementation and phone-installation status.

Open [the review gallery](./index.html), [the manifest](./manifest.json), or [the source checklist](./source-surface-checklist.csv).

## Board coverage

| Board | Screens / states | Image status |
| --- | --- | --- |
| [01. Discovery & search](./01-discovery-v1.png) | Discover; Global search; Source catalogue | Generated; review required |
| [02. Sources, extensions & repositories](./02-sources-v1.png) | Sources; Extensions; Repositories | Generated; review required |
| [03. Source migration](./03-migration-v1.png) | Find affected titles; Migration options; Review & progress | Generated; review required |
| [04. Updates, history & downloads](./04-daily-v1.png) | Updates; History; Downloads | Generated; review required |
| [05. Library organization](./05-library-tools-v1.png) | Library filters; Categories; Bulk selection | Generated; review required |
| [06. Reading & chapter navigation](./06-reading-v1.png) | Paged reader; Webtoon reader; Chapters & pages | Generated; review required |
| [07. Translation in the reader](./07-translation-v1.png) | Area selection; Inspect bubbles; Chapter translation | Generated; review required |
| [08. Reader settings & page actions](./08-reader-options-v1.png) | Display controls; Translation actions; Page actions | Generated; review required |
| [09. Getting started](./09-onboarding-v1.png) | Welcome; Storage & permissions; Theme & finish | Generated; review required |
| [10. Backups, restore & sync](./10-backup-sync-v1.png) | Create backup; Restore preview; Sync settings | Generated; review required |
| [11. Tracking & privacy](./11-tracking-security-v1.png) | Trackers; Link a title; Security & privacy | Generated; review required |
| [12. Core settings & appearance](./12-general-settings-v1.png) | Settings; Appearance; Library settings | Generated; review required |
| [13. Reader, browse & download preferences](./13-core-preferences-v1.png) | Reader preferences; Browse preferences; Download preferences | Generated; review required |
| 14. Performance & maintenance | Performance; Maintenance & source health; Advanced tools | Prompt ready; not generated |
| 15. Reading journey & identity | Journey overview; Genres & patterns; Milestones & profile | Prompt ready; not generated |
| 16. Empty, loading & offline states | Empty library; Partial loading; Offline & no results | Prompt ready; not generated |
| 17. Dialogs, permissions & authentication | Confirm an action; App lock; Website sign-in | Prompt ready; not generated |
| 18. Recovery, diagnostics & release information | Restore issue; Crash recovery; About & changes | Prompt ready; not generated |
| 19. Tablet & landscape layouts | Tablet library; Landscape reader; Large-text phone | Prompt ready; not generated |
| 20. Light, high-contrast & E-Ink | Light theme; Accessible controls; E-Ink profile | Prompt ready; not generated |
| 21. Specialist source integrations | MangaDex follows; Source integration settings; Metadata editor | Prompt ready; not generated |
| 22. Extension details & merged manga | Extension details; Source preferences; Merged manga & notes | Prompt ready; not generated |
| 23. Batch tools & advanced organization | Batch add; Smart search; Advanced categories | Prompt ready; not generated |
| 24. Widgets & system surfaces | Updates widget; Widget size variants; Notifications & shortcuts | Prompt ready; not generated |

## What the source inventory means

The CSV contains 232 named Kotlin surface entries (file entry points and named Screen/Dialog/Sheet/Step functions). UI wrappers and presentation functions can appear separately. This is a static inventory, not a count of unique user-visible screens or a runtime navigation audit.

Each entry maps to a representative board or shared template. A mapping is not proof that every inherited preference, inline modal, or state is rendered. Rows marked shared/template/manual review must be inspected before implementation is considered covered. Anonymous dialogs, dynamic extension settings, and system/browser UI require runtime review.

## Shared component contract

- Reuse existing Sanctuary colors, typography, shapes, spacing and shared components; generated pixels are not authoritative RGB or dp specifications.
- Use one consistent app bar, navigation dock/rail, preference row, section card, search field, manga card, selection bar, dialog and bottom-sheet system.
- Preserve the extension ABI, backup identifiers, deep links, existing preference keys, navigation destinations, source controls and suggestion counts.
- Treat sample names, counts, source labels and status badges as illustrative. Never hard-code generated text as live data or claim checks that were not performed.
- Make states explicit: default, pressed/selected, disabled, focused, loading, partial success, empty, no results, offline, authentication required, failure, retry and completion.
- Keep destructive operations scoped and explicit. Distinguish removing library entries from deleting downloads. Preserve supported cancellation boundaries.
- Reserve layout insets for navigation, gesture/three-button bars, keyboards, bottom actions and reader controls. Verify the final item and its actions can be reached.
- Keep existing features accessible when grouping settings. Search and result highlighting must traverse all subpages. Advanced integration/debug preferences must remain available.

## One coordinated implementation pass

1. Review all board directions and resolve the generated-content notes below before changing behavior.
2. Establish shared component variants and tokens first; migrate each family using the same primitives rather than styling each page independently.
3. Work through the source CSV, filling implementation and device-validation columns. Inspect every inherited preference and every template-mapped surface.
4. Verify end-to-end paths: onboarding/restore → sources → search → library → reader → progress → download/update → backup/sync/tracking.
5. Validate phone/tablet/landscape, long translated strings, RTL, large text/display scaling, TalkBack/focus, reduced motion, light/dark/high-contrast and E-Ink/low-power profiles.
6. Verify privacy/lock screens, Android permission denial, website authentication, extension trust, migration/cancellation, storage failures, missing covers and corrupt backup/error recovery.
7. Run appropriate dev-flavor checks and the project release protocol only when an actual release is authorized. A gallery or mapped CSV is not a completion certificate.

A single coordinated UI update is the goal; images cannot guarantee a defect-free single coding attempt. Runtime, accessibility and data-safety checks remain required.

## Review notes on generated images

- Board 01: Preserve genre filters and full-title access; generated counts and truncated captions require review.
- Board 02: Preserve Feed / Sources / Extensions / Migrate routes; the generated Latest updates tab must not replace an existing route.
- Board 03: Sample source names and matching claims are illustrative; show only data supported by actual sources.
- Board 04: Rendered 2026-11-04. Downloads shows Pause all and Resume all as active together; implement only the action appropriate to queue state. Sample counts/progress are illustrative.
- Board 05: Rendered 2026-11-04. Filter badge says 3 active but only two rows have non-Any selections. Tri-state controls need checkmarks or text beyond color. Dialog and bulk-selection layouts are representative; verify safe final-row scrolling.
- Board 06: Rendered 2026-11-04. Webtoon loading row also shows Retry; separate loading and failure states. Generated chapter bookmark controls require capability verification; do not introduce unsupported actions.
- Board 07: Rendered 2026-11-04. Translation text, counts and completion states are illustrative. Verify existing pause/cancel support and chapter scope before implementing controls; avoid treating generated state labels as confirmed behavior.
- Board 08: Rendered 2026-11-04. Brightness slider appears enabled while system brightness is on; align enabled states. Cache wording incorrectly says downloaded translation data and scopes removal to a title; use actual saved-cache semantics. Verify page bookmark/orientation actions against existing capabilities.
- Board 09: Rendered 2026-11-05 with built-in image_gen and the two approved references. Review Next-button gating: no selected storage folder is shown, so its enabled appearance is illustrative. Theme/finish combines two steps; verify actual onboarding flow. AI setup remains optional.
- Board 10: Rendered 2026-11-05. Review required: Chapters wording implies downloaded image files are backed up/synced; use actual metadata/progress scope. Restore counts/time and progress-preservation promise are illustrative. Cancel during active restore must not be implemented unless safely supported; require explicit confirmation before starting. Folder paths are examples, not Android storage APIs.
- Board 11: Rendered 2026-11-05. Connected status, biometric availability, chapter progress and matches are illustrative. Tracker search should be scoped to the selected service rather than merging providers into one result list unless supported. Verify sync toggles and hide-content behavior. Incognito wording must not imply stopping library updates; describe only actual history/tracking behavior. Source: Local library is illustrative, not real source metadata.
- Board 12: Rendered 2026-11-05. General Settings root is mislabeled ShinKu settings and needs a separate ShinKu hub entry. Font, density, page layout, navigation style and scheduling controls are illustrative; verify actual preference capabilities and preserve OS settings/defaults. Preview values are samples.
- Board 13: Rendered 2026-11-05. Language count matches the two selected example rows. Default search target, page transitions, extension-security grouping and download-ahead switch are illustrative; retain actual supported controls, values and inherited adult-content preferences. Clear-cache versus manga-file removal must match the real storage scopes; the all-downloads deletion confirmation is a design example, not an authorized deletion or new maintenance capability.
- Board 14: Generation attempted 2026-11-05; tool returned usage_limit_reached and produced no image. Resume at this board after the tool-estimated reset.
- Board 24: Respect Android/OEM-controlled layouts and supported widget sizes.

## Resume generation

After the tool quota resets, generate only missing PNGs from their numbered prompt files with the two existing reference boards. Keep filenames/versioning stable; do not overwrite reviewed images without requesting an iteration. Use built-in image generation unless the owner explicitly chooses a different provider/path.

Set each board status in manifest.json after inspecting its output, then run `python docs/design/mockups/build_review.py` to refresh the gallery and CSV. An alternate API-key or credit-based image service requires the owner’s explicit choice; no fallback service has been invoked.
