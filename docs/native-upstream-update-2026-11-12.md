# Native compatibility and selected upstream updates — November 12, 2026

This pass addresses the confirmed native-library alignment issue and ports focused upstream fixes into the existing UI work. Local baseline is `a931760a62` plus the previously recorded, uncommitted UI changes. No upstream branch was merged wholesale.

## Upstream review

Fetched Mihon `main` at [`134e5de3d672`](https://github.com/mihonapp/mihon/commit/134e5de3d672) and TachiyomiSY `master` at [`c1eb1e3cbfd7`](https://github.com/jobobby04/TachiyomiSY/commit/c1eb1e3cbfd7). Reviewed their recent commits and compared relevant fixes with ShinKu's current source.

| Change | Integration decision |
| --- | --- |
| SY [stored stub sources](https://github.com/jobobby04/TachiyomiSY/commit/2bea5c9765a5efa82e8d7abe35da82aa855268fc) | Ported: populate the real concurrent cache instead of a discarded copy; source metadata gains value equality so unchanged rows do not trigger false changes or renames |
| Mihon [observable calendar locale](https://github.com/mihonapp/mihon/commit/629033c804d019535390759d14be9aabc24999f8) | Ported the calendar portion using the existing Compose version's observable `LocalConfiguration`; recompute weekday order when locale changes, and format the month with that same locale |
| Download-index startup ordering and atomic writes | Already present: `initJob.join()`, `AtomicFile` and snapshot locking in ShinKu's `DownloadCache` |
| Sui/Shizuku detection | Already present: Shizuku permission check with Sui fallback |
| MyAnimeList token expiry | Already correct: the expiry comparison includes the one-minute margin |
| Unreadable local archive metadata | Already isolated per chapter, with cancellation rethrown |
| New merged-manga schema and migration fixes | Deferred: these depend on upstream's newer schema and merge representation; isolated cherry-picks would not preserve ShinKu's existing data model |
| Reader renderer/Compose rewrite, Injekt removal, Gradle/AGP 9.x, beta Compose updates | Deferred as broad migrations with dependencies outside this focused pass |
| Source-preference Compose/custom-button portion of the lint fix | Not applicable to ShinKu's retained native preference-fragment integration |

Source API/network namespaces, backup identifiers, deep links, database schema and runtime settings retain their existing contracts. No version bump or publication is part of this work.

## Native replacements

| Component | Before | Selected replacement |
| --- | --- | --- |
| Image decoder | `com.github.tachiyomiorg:image-decoder:41c059e540` | Maintained Mihon fork pinned to [`e03b81e18a`](https://github.com/mihonapp/image-decoder/commit/e03b81e18a), preserving the `tachiyomi.decoder` API and all four existing CPU architectures |
| MediaPipe text | `0.10.14`, with a 4 KB-only text JNI library | Official `0.10.35`, preserving the used text-embedding API and all four CPU architectures; the JNI runtime is now shared through `tasks-core` |
| ML Kit OCR | Bundled `16.0.1` | Unchanged; the inspected OCR ELF already has 16 KB alignment |

Candidate binaries were inspected before selection. Both 64-bit decoder and MediaPipe binaries have 16 KB LOAD alignment and pass the RELRO check. A new standalone [alignment checker](../scripts/check-native-alignment.py) checks uncompressed ZIP offsets, ELF LOAD alignment and RELRO boundaries in APKs. It reproduces the old installed APK's decoder/MediaPipe failures and passes the rebuilt ARM64, x86-64 and universal debug APKs.

The maintained decoder AAR declares `minCompileSdk=37`. The shared Android configuration now compiles all consuming modules against the installed SDK 37. Runtime `targetSdk=36`, `minSdk=26`, Gradle and AGP versions are unchanged. **AGP 8.13.2 warns that it was tested through compile SDK 36.1.** That warning is not suppressed. Compilation passes, but this build-tool support limitation remains explicit; the change does not imply an AGP 9.x migration has been completed. An initial app-only SDK change compiled but failed seven shared-module release metadata checks; the shared configuration correction resolves that incomplete update. The initial failure log is retained as `build/native-upstream-release-sdk-metadata-before.log`.

## Validation

Scoped formatting, both dev Kotlin compilers, debug/test assembly and JVM tests passed. JVM: 81 discovered, 80 passed, one existing skip, no failures/errors. New source tests cover metadata hydration, updates without extra database reads, and equality distinguishing real changes.

Google's official API 35 experimental 16 KB x86-64 image was downloaded and verified against its repository checksum, then booted in a fresh workspace-owned headless AVD. Guest `getconf PAGE_SIZE` reports **16384**; native fixtures independently assert that page size with `Os.sysconf`. No existing AVD or phone setting was altered.

The first 63-case Android run passed 61. The text model fixture was shell-owned in scoped external storage and unreadable by the app; transferring the official model into the isolated app's private directory with `run-as` resolved that fixture failure. SHA256 of the transferred model: `89ad3c74175dd8caa398cc22b657296d94302d20c525c12b58b29420f7249749`. The native decoder and real sentence-encoder cases then passed on the actual 16 KB runtime. The other initial failure was a download queue menu interaction; the fixture now refreshes Android 14+ accessibility nodes before reading bounds. Production download logic is unchanged. Initial log: `build/native-16k-full-tests.log`; targeted native result: `build/native-16k-runtime-tests.log`.

## Final gates and artifact

- Consistent shared compile SDK: both dev compilers, final JVM suite, and debug/test assembly pass (`build/native-upstream-final-debug.log`). JVM 81 discovered/80 passed/one existing skip, no errors/failures.
- Native-only gate: three actual JNI cases (PNG/JPEG/WebP full/region/sample decoding, bundled local Latin OCR, real sentence embeddings) pass on the 16 KB runtime. Each case independently asserts `Os.sysconf(_SC_PAGESIZE)==16384`.
- **Final clean full Android run: OK (64 tests)** on the same actual 16 KB runtime after the shared-SDK correction. Log: `build/native-16k-sdk37-final-full-tests.log`. This supersedes the initial 63-case fixture failures and earlier 64-case run before the shared SDK correction. No compatibility-warning acknowledgment or global page-size mode change was used.
- **Physical phone: OK (12 tests)**, including those three native cases, Reader dialogs/crop/results/actions, native overlay mapping/containment, and observable calendar locale. The app's native compatibility flag changed from 4 in the original investigation to **0**. No warning dismissal option was supplied; default UI visibility assertions passed. Private screenshots reviewed under `build/phone-native-review/final-fixtures`. Temporary debug/test packages and their model/capture data were removed, and the production app was restored to main navigation. Display settings and reading progress were preserved.
- Required optimized `assembleDevRelease --build-cache --parallel --quiet --console=plain` passed. All five APK signatures, official 16 KB `zipalign`, native LOAD/RELRO/ZIP checks and expected CPU-specific decoder/MediaPipe/OCR libraries verified. Artifact record: `build/native-upstream-checksums.json`; logs: `build/native-upstream-release.log`, `native-upstream-artifact-verification.log`, `native-upstream-signatures.log`, `native-upstream-zipalign.log`; machine-readable native report: `build/native-upstream-native-alignment.json`.
- Final ARM64 version 2.8.1/144, packaged 2026-11-12T22:10:55.6358208+08:00, bytes 84466217, SHA256 `4F584413D9ED639F6E77B9B20A6C362E1C646CAB31D388F08DA9755D12D3742B`. **Installed in place** at 2026-11-12T22:15:07.7958506+08:00; independently pulled installed base APK checksum matched and production `pageSizeCompat=0` was verified. No screen unlock was required for this final installation.
- The optimized x86-64 APK also installed and cold-started successfully on the isolated 16 KB emulator. Its process remained running and the UI showed ShinKu's release notes, with no Android compatibility dialog (`build/native-release-review.xml`). The emulator was then stopped.
- Native runtime and UI instrumentation exercise devDebug; the optimized artifacts are independently built, signed and structurally verified, with startup checked as described above. These checks do not certify every image encoding, model, live provider/account, extension, restore or OEM widget state.

No version bump, commit, push, GitHub release/publication, issue closure, image mockup or paid-provider request occurred. The source/network extension ABI, legacy backup identifiers and deep-link contracts remain unchanged. No schema migration or default-preference change was introduced. The isolated 16 KB emulator was stopped after validation; its workspace-only image/AVD can be reused for future regressions.
