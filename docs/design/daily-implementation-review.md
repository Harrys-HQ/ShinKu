# Daily screens mockup implementation

Implementation date: 2026-11-04. Reference: [board 04](mockups/04-daily-v1.png), with the existing Sanctuary theme and shared components.

This is the first implementation pass for Updates, History and Downloads. The owner authorized beginning work on areas with completed mockups. Library/glass-dock work from the earlier pass remains in place. Other completed boards remain queued.

## Changes

- Updates cards use portrait covers, up to three title lines and two chapter-name lines. Reading position has its own line. A labeled Read/Resume action appears outside selection mode beside the existing state-aware download indicator, retaining its progress, cancellation, delete and immediate-download behavior. Selection mode replaces those actions with a checkbox and retains long-press/range and bulk actions.
- History cards allow three title lines and include an explicit Resume button. The cover still opens details and the card still resumes reading. Add to library and removal are in the overflow menu; removal still uses the existing confirmation dialog. Read timestamps derive from the current item rather than an unkeyed remembered value.
- Updates and History use the measured home dock inset at the end of their scroll content. Updates selection mode instead uses its action-bar padding. Content can continue behind the glass dock.
- Downloads replaces the collapsing floating button with a persistent Sanctuary header and exactly one Pause all/Resume all action driven by the existing downloader flow. The source-grouped RecyclerView, chapter/page progress, drag ordering, sort options and cancellation menus are retained. Queue rows have rounded, theme-based surfaces and longer title/chapter labels.
- New action labels use base localized resources, allowing the existing resource fallback for untranslated locales.

## Deliberate differences from generated pixels

History's current model supplies chapter number and reading time, without page totals. No sample progress bars or page counts were added. The downloads queue retains its existing source grouping and removal of completed entries; no fabricated Finished group, speeds or aggregate completion percentage was introduced. Its original chapter actions and reorder controls remain available. Cover thumbnails and broader queue restructuring are outside this pass.

## Validation

- `compileDevDebugKotlin` passed. Log: `build/daily-mockup-compile.log`.
- Existing `:app:testDevDebugUnitTest` passed: 44 discovered, 43 passed, one existing skip, zero failures/errors. Log: `build/daily-mockup-tests.log`.
- Scoped formatter applied to the five changed Kotlin files. Logs: `build/daily-mockup-format.log` and `build/daily-mockup-format-hook.log`; the normalized-path `:app:spotlessApply` hook reported clean. No repository-wide formatting claim.
- Required `assembleDevRelease --build-cache --parallel --quiet --console=plain` passed, exit 0. Quiet log: `build/daily-mockup-release.log`.
- All five newly generated APK signatures verified. Build times and SHA-256 values: `build/daily-mockup-checksums.json`. APKs remain version `2.8.1-preview.1` / 143; distinguish them from earlier same-version local builds by checksum.

The verified ARM64 APK was subsequently installed and sampled on the phone. Updates selection, History confirmation/cancel, final-row clearance, a landscape layout and real download progress were checked; both pending Android geometry tests passed after fixing the missing runner dependency. See [device results and remaining checks](daily-device-checks.md), including a download-header status discrepancy that still needs controlled reproduction. Original text size and portrait orientation were restored at the owner's request. Full large-text, theme, accessibility and queue interaction validation is not claimed.

These changes are a local preview, with no version bump or publication.
