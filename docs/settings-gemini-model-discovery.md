# Settings info rows and Gemini model discovery

Updated 2026-11-06. Follow-up to the owner's on-device report that the settings footer stacks its icon above its text and the previously offered Gemini 3.5 Flash-Lite does not work. The exact HTTP status from the affected account was requested but has not been supplied; no account-specific failure cause is claimed.

## Settings layout

The shared InfoWidget used a Column. It now uses a full-width Row with a 20 dp decorative information icon, 12 dp spacing, and weighted wrapping text. The ShinKu footer displays its icon and “Changes save automatically” together. Longer notes wrap within the text area. Existing typography, display scale and theme controls are retained. The same correction applies consistently to settings information rows.

## Gemini catalog instead of a fixed availability assumption

The earlier fix corrected the 3.x-to-2.5 rewrite but replaced older choices with another fixed list. Published model IDs are not proof that a particular key/project can successfully generate content.

- GeminiApi now reads the authenticated `GET /v1beta/models` catalog, follows pagination, deduplicates IDs, checks generateContent support and filters known specialized image/audio/live/embedding model names out of the text-model picker.
- The AI settings page loads that catalog for the configured key and offers explicit Refresh available models. Metadata loading is gated to the visible AI page; building the settings-search index does not trigger it. Other AI providers retain their existing configuration.
- The picker shows Automatic, fetched choices, and a retained saved/custom ID where necessary. It no longer advertises a fixed set of models as available for every account. Catalog errors and counts remain visible and explicitly distinguish listing from successful inference.
- Automatic ranks and attempts only catalog-listed candidates, with a maximum of three generation attempts. The old Flash-Lite constant is a ranking/normalization hint, not a request target when the catalog omits it. Explicit current/custom IDs remain exact; saved legacy IDs can discover current fallbacks after an unavailable/server-error response.
- Successful catalogs are cached in memory for ten minutes in a bounded eight-entry LRU, scoped to client, endpoint and SHA-256 credential digest. The new LRU retains credential digests rather than raw keys and adds no persistent catalog or credential preference. Existing API-key preference storage is unchanged. Refresh bypasses and invalidates stale cached results, including when refresh fails.
- API errors retain HTTP status and validated Google status/reason enums, without echoing response bodies or credentials. Connection-test details remain visible until configuration changes or a new test starts. Explicit model tests stay exact; Automatic tests use the same catalog/fallback policy as runtime requests and report the model that actually answered. Authentication and quota errors do not cause model cycling.

Primary references: [Google model-list API](https://aistudio.google.com/api-reference/models/list-models), [Google troubleshooting](https://aistudio.google.com/docs/troubleshooting). Catalog visibility does not establish billing, quota, region/project eligibility, current serving capacity or a successful generateContent call. These require the affected account's response. No production key was requested or used by the transport tests.

## Validation and delivery

- Full JVM suite: 68 discovered, 67 passed, one existing skip, zero failures/errors. Twenty Gemini transport tests cover key-scoped discovery/cache/refresh, paginated filtering, missing default model, empty/failed catalogs, safe diagnostics and runtime/connection fallback, alongside the prior request, image and cancellation regressions.
- Android 15/API 35 read-only emulator: OK (17 tests). Updated picker fixture supplies a catalog omitting Flash-Lite, checks fetched choices and custom IDs, and verifies key changes/refresh errors. Existing navigation/search, independent colors, reader/library controls, queue and geometry tests remain passing.
- Reviewed final hub and picker screenshots. The footer is one horizontal row; model names/IDs wrap legibly. Picker screenshots use synthetic catalog data, not the owner's account. Local screenshots stay under ignored build/.
- Single-file Spotless hooks, source compilation, debug/test APK assembly and focused whitespace checks passed. Logs: build/settings-gemini-verified-debug.log, settings-gemini-android-tests.log and settings-gemini-format.log.
- The required optimized build passed and all five APK signatures verified. Final records: build/settings-gemini-release.log, settings-gemini-signatures.log and settings-gemini-checksums.json. Version remains 2.8.1-preview.1 / 143; distinguish previews by hash and build timestamp.

No reading data, preference keys/default values, schema, extension ABI, backup identifiers or deep links were changed. No commit, push, public release, GitHub comment or issue closure is included. Transfer the new ARM64 preview through the owner's preferred LocalSend workflow. Real-key inference and physical-phone installation remain unverified until the owner tries it.
