# GitHub issues 26 and 27: verified local fixes

**Nov 6 follow-up:** the fixed Gemini list below has been replaced with authenticated catalog discovery and persistent diagnostics. See [Settings and Gemini model discovery](settings-gemini-model-discovery.md) for the current implementation and newest artifact record. The sections below record the Nov 5 fix.

Reviewed 2026-11-05 against the current working tree. Both reports concern published 2.8.0. Local code includes the earlier unreleased mockup and reliability work; this is not a published release or an issue-closure record.

## Issue 27: Gemini HTTP 404 and unavailable model options

Report: [All the current gemini models does not work](https://github.com/Harrys-HQ/ShinKu/issues/27).

Confirmed source defects: the cloud engine, discovery helpers and connection test rewrite every `gemini-3.*` selection to `gemini-2.5-flash`. Their fallback lists also contain retired 1.5/2.0 IDs. Settings offers only older models. Google documents restrictions for older 2.5 models and shutdowns for 2.0, while 3.5 Flash-Lite and 3.6 Flash remain supported. This supports the report's access failure, but does not prove that every 2.5 model has shut down for every account.

Changes:

- `domain/.../ai/GeminiApi.kt` centralizes model normalization, request construction, response parsing and HTTP errors. Cloud AI, discovery text/image requests, translation helpers and the settings connection check share it.
- Automatic defaults to `gemini-3.5-flash-lite`; the dropdown also offers 3.6 Flash and 3.5 Flash. An advanced model-ID editor accepts exact Gemini IDs without requiring another app update. Existing saved IDs remain visible and are not rewritten in preferences.
- Explicit current/custom selections are sent exactly. Automatic and saved legacy selections have bounded current-model fallbacks for unavailable/server-error responses. Authentication, malformed requests and quota errors stop without switching models. Connection testing checks only the resolved selected model, so a different fallback cannot falsely validate it.
- Keys use the `x-goog-api-key` header instead of URL query parameters. HTTP errors do not expose raw response bodies. Multipart answer text is combined while thought parts are excluded; blocked/empty responses fail rather than passing a connection check.
- Existing preference keys, backup/translation-cache formats, provider routing and other providers' model lists remain intact.

Google references: [model lifecycle](https://ai.google.dev/gemini-api/docs/deprecations), [3.6 Flash and 3.5 Flash-Lite migration guidance](https://aistudio.google.com/docs/whats-new-gemini-3.6). Requests retain the stateless generateContent format with user-role contents and no deprecated sampling parameters or prefilled model turn. No Interactions API retention or new account integration is introduced.

## Issue 26: Mood Lighting versus Library color changes

Report: [Mood lighting bug](https://github.com/Harrys-HQ/ShinKu/issues/26). The attached video was downloaded and reviewed locally; personal library content is not copied into this report.

The video shows app-wide accent and surface changes while the old Mood Lighting switch is off. These colors come from the independent Dynamic Library Sanctuary Theme preference, enabled by default. Reader mood lighting controls a genre-based tint/dimming overlay. Its old description incorrectly claimed to follow the current page artwork and did not explain the separate Library setting.

Changes preserve the two independent preferences and their defaults:

- Added **Color effects** under Appearance and linked it from the existing ShinKu Reading/appearance destinations. Search indexes the new page.
- Both actual switches appear together as **Reader mood lighting** and **Cover-based app theme**, with accurate scope and instructions for static app colors.
- Existing ShinKu entries navigate to that page instead of maintaining duplicate switches in distant groups.
- The cover-color synchronization effect checks cancellation and the current enabled preference before publishing an asynchronously extracted color.

Turning reader mood lighting off alone intentionally does not disable the independently enabled cover-based app theme. This is a clarification and discoverability fix for the reported behavior, rather than a claim that the reader overlay was proven to ignore its switch. Reporter confirmation remains pending.

## Validation

- Full JVM suite: 60 discovered, 59 passed, one existing skip, zero failures/errors. Twelve new transport-level regressions use an OkHttp interceptor with synthetic keys and responses; no network or billable provider request is made.
- Cases include preserved 3.x/future IDs, URL credential exclusion, legacy/automatic fallback, exact connection testing, authentication/quota stop behavior, bounded attempts, thought filtering, blocked content, inline image payload and cancellation propagation.
- Android 15/API 35 read-only emulator: **OK (16 tests)**, including the two new regressions plus existing mockup, download-queue and translation-geometry checks. Verified current model choices, custom-ID persistence, settings navigation/search, independent toggles, changing colors when opted in, and stable theme colors when opted out.
- Reviewed issue-specific screenshots locally. Initial emulator System UI ANR was dismissed; subsequent test timing failures were corrected by waiting for rendered/enabled controls. The final complete suite passed.
- Source formatting used single-file Spotless IDE hooks. No broad formatter run. Full source compilation, debug/test APK assembly and focused whitespace checks passed.
- The required optimized build and all-five-APK signature verification passed; records are in `build/github-issues-release.log`, `build/github-issues-signatures.log` and `build/github-issues-checksums.json`. Version stays 2.8.1-preview.1 / 143.

Logs: `build/github-issues-final-tests-build.log`, `build/github-issues-final-android-tests.log`, and `build/github-issues-format.log`. Screenshots and reproduction media remain local under ignored build directories.

## Delivery boundaries

Live Gemini project/key access was not tested; model availability, billing, permissions and quota are server/account-specific. Android 11 behavior from the report has not been exercised on an Android 11 runtime; the controlled color regression ran on API 35. No reading history, streak data, schema, source ABI, backup identifier or deep link was changed. No public comment, commit, push, version bump, release publication or issue closure was performed. Follow AGENTS.md's full release pipeline before closing either issue.
