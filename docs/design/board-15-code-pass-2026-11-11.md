# Board 15: reading journey and identity

This direct code pass follows the [boards 01–14 checkpoint](boards-01-14-code-pass-2026-11-11.md), using the existing Sanctuary direction without another mockup. It extends the journey overview, calendar activity and truthful genre/time summaries from the November 10 pass.

## Changes

- Share-card statistics are opt-in per dialog session. Level/XP, time, chapters, streak, badges and genres/persona start unchecked. The scope note explains that name, avatar and chosen title are included. No preference, reading-data or backup-format change.
- The preview and PNG exporter receive the same filtered payload. Hidden statistics never reach either rendering path. Unchecking a statistic removes its values; all-unchecked produces an identity-only card.
- The preview uses wrapping identity text and stacked metric values. Theme choices have 48dp targets and selected checkmarks; avatar/share/close controls form an explicit vertical footer while the options scroll. Default Obsidian uses a muted lavender accent for readable small text.
- Export text is measured with Android multiline layouts. Image height follows actual identity, metrics and genre content; metrics use two measured columns instead of four fixed positions. Zero progress renders an empty track. Default avatar uses the existing crest. Each explicit export creates a separate cached file, preserving an earlier chooser's image.
- Avatar decoding samples large images and uses a center crop; revoked/unreadable avatar access falls back to the crest. Export failures produce a visible retry message, duplicate share taps are disabled while preparing, and coroutine cancellation propagates.
- Milestone columns adapt to available width and font scale. Names/descriptions wrap fully; progress text flows rather than colliding with the percentage. Filters have 48dp targets and badge details scroll. No badge/XP/equipped-title semantics changed.

## Validation

Final file-scoped formatting and both dev compilations passed. Combined boards15–16 JVM suite:76 discovered /75 passed /one existing skip, including the two share-selection regressions. The four new journey Android cases pass independently at normal/1.6× text and again in the complete39-case suite plus nine affected large-text cases. They cover opt-in/removal, actual non-overlapping dialog controls without sharing, exported long-text images/separate URIs/sampled avatar crop, and milestone cards/details. Final combined logs: `build/states16-android-tests.log`, `build/states16-large-font-tests.log`, `build/states16-final-debug.log`.

Reviewed final normal/large-text captures under `build/states16-review/default` and `large-font`, including selected/deselected previews, vertical footer, adaptive milestone grid/details and identity-only/selected-stat PNG exports. These are controlled synthetic fixtures; physical-device, whole-screen theme, RTL and broader accessibility acceptance remain separate. Final optimized artifact verification is recorded in the [board16 report](board-16-code-pass-2026-11-11.md).

The first normal run exposed an immediate assertion before the deselected preview redrew. The test now waits for the visible preview to update and brings it into view before checking removal. Normal and large-text reruns then passed, but visual review rejected the tinted-into-a-block default logo and overlapping large-text Close action. The existing crest now keeps its artwork; a vertical action footer and explicit bounds assertions address the overlap. Earlier passing click tests are not treated as final acceptance of that rejected composition.

The combined boards15–16 APK was installed in place on the phone at20:38:37 on November11 and its installed hash verified. Targeted phone review confirms Journey totals, identity-only share preview, initially unchecked statistics, scrolling remaining options, separate Avatar/Share/Close actions and readable milestone cards. No option, avatar or equipped title was changed and Share was not invoked. Broader states remain pending. No new image service, automatic sharing, live reading session, preference edit, version bump, commit, push or public release.
