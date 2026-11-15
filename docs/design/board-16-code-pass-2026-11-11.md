# Board 16: empty, loading and unavailable-source states

Direct code continuation from [board 15](board-15-code-pass-2026-11-11.md), using existing Sanctuary components and briefs without image generation.

## Changes

- Empty Library has one scrollable card with an existing book icon, “Your library starts here,” primary Browse sources, secondary Restore backup and the retained getting-started guide. AI setup is optional. The caller reserves the existing phone dock clearance; large text can scroll. Restore retains the real data/storage route and its safeguards.
- Browse requests explicitly select Sources or Extensions using the current visible tab ordering. This also corrects the old fixed Extensions index when Feed is hidden. No source/display preference is edited by the Browse-sources action.
- Shared empty screens use a calm inbox icon and full-width, wrapping 48dp action buttons rather than random faces and compressed action columns. Existing messages/callbacks remain; card content colors are explicit.
- Search shows No matching titles only after actual source requests complete with no results and no failures. Empty source selection is recorded after the selected-source list is resolved and gets its own explanation. Neither state is inferred from a still-pending request.
- Search failures have a separate notice based on the real unfiltered error count, even when the existing “only sources with results” setting hides failed rows. Available results remain actionable. Show all sources uses the existing result/source-filter callbacks and preserves the query. Query summaries and source headings wrap.
- Discover reports catalogue/saved-feed/genre request exceptions and limiter timeouts, retains loaded content and per-feed loading placeholders, and exposes the existing refresh action plus actual source routes. Failure sources are deduplicated and reset by refresh. Empty responses remain successes; cancellation does not publish source failures. The six/three permits, timeout and scheduling are unchanged. Nullable AI title matching retains its prior path; a missing title is not labelled a source outage.
- Discover with no usable content offers retry and an actual failed-source route when available. Related source/feed actions use 48dp targets.

## Supported adaptations

The current search flow does not provide a durable offline result cache. These screens therefore describe actual source failures and retained loaded results; they do not label them “Offline” or “Showing saved results” without evidence. Discover retains its existing library fallback. No new persistence, network monitor, source API, automatic retry loop or AI setup requirement is introduced.

## Validation

File-scoped formatting, both dev compilations, debug/test APK assembly and `git diff --check` pass. JVM76 discovered /75 passed /one existing skip; zero failures/errors. Two new request-feedback regressions cover empty success, timeout, exceptions, cancellation and permit reuse. An initial fixture compared exception identity across a coroutine boundary; it now verifies preserved class/message instead. Final build log: `build/states16-final-debug.log`.

Five new Android cases cover empty-library actions, empty/loading/failure search states, retained results with hidden failures, partial Discover plus a pending feed and no-content recovery, and empty source selection. Complete suite: **OK(39 tests)**. Nine affected board15/16 cases repeated at actual1.6× text: **OK(9 tests)**. After neutral count wording replaced grammatically incorrect singular counts, three affected cases repeated at both scales: **OK(3 tests)** each. No assertion was removed. Logs: `build/states16-android-tests.log`, `build/states16-large-font-tests.log`, `build/states16-final-wording-1.0.log`, `build/states16-final-wording-1.6.log`.

Reviewed the sample captures in `build/states16-review/default` and `large-font`: empty-library actions remain reachable with dock clearance; query/source labels wrap; failed sources remain distinct from no matches; retained results remain actionable; loaded Discover content and pending-feed skeletons coexist; recovery buttons stack without collisions. Actual app source/reader data were not used in these fixtures. Restored emulator font1.0/Play Store and stopped its read-only instance after checks. Full phone/theme/RTL/accessibility acceptance remains pending.

Required `assembleDevRelease --build-cache --parallel --quiet --console=plain` completed successfully. All five APK signatures verify. Logs: `build/states16-release.log`, `build/states16-signatures.log`; exact artifact record: `build/states16-checksums.json`.

ARM64 remains version **2.8.1 /144**, built **November11, 17:54 Shanghai**, SHA256 `BE2F496F62A6F9422EF678FC9C02C9CF26A1288F95C6DF8BFB9455AD1B40C6D1`. File: `app/build/outputs/apk/dev/release/ShinKu-dev-arm64-v8a-release.apk`. This supersedes intermediate board15 APKs with the same version. ADB no longer listed the phone at the final checkpoint; this artifact has not been installed there.

The owner reconnected the phone and this exact build was installed in place at20:38:37 on November11. The installed APK's SHA256 was independently checked on-device and matches the record above. Targeted Journey/share/milestone review passed; board16 empty/loading/error states retain their synthetic-fixture coverage. See the [phone checkpoint](phone-ui-audit-2026-11-11.md). No live phone reading, progress/preference edit, restore/download/cleanup, generated mockup, version bump or external publication.
