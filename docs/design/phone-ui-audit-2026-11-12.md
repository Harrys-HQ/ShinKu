# Phone UI review — November 12, 2026

This is targeted visual acceptance, not certification of all 24 boards or live workflows. Private captures remain in ignored `build/phone-ui-review/boards24-*`; no reading titles, account values or personal statistics are reproduced here.

## Installed artifact and observations

The reviewed ARM64 APK was version2.8.1/144, SHA256 `704B85FB7E6059F6CA138600818FB84E6E242EDA721AC7484FDA4C8EFEB297AB`. It was installed in place at02:18:59 and its installed base APK checksum independently verified. The owner unlocked the phone and extended its timeout. Existing font1.0 and1080×2376/density480 overrides were retained.

- Removal confirmation: temporary single-title selection opened the real dialog. Count/scope text wraps, library/download choices begin unchecked, Remove is disabled, and Cancel is reachable. Cancelled without selecting a choice or confirming, then cleared temporary selection.
- Release information: the real bundled2.8.1 entries load in the bounded scrolling dialog and Close is visible. Review caught an obsolete95% completion claim, contradicted by the current audit. In-app and website release-note wording now describes supported changes and ongoing acceptance instead of certifying every panel/device. No version bump or public release is involved.
- Extension details: bordered header, extension identity/version/language, stacked Uninstall/App info and source settings route are readable. No trust/install/update/uninstall/incognito/source-enable action was taken.
- Native source preferences: source-only scope text and wrapping extension-provided title/summary render within the native preferences container. No preference value was edited.
- Library, More and Browse navigation remains available in the installed build. Other boards' earlier targeted phone evidence is recorded in [the November11 audit](phone-ui-audit-2026-11-11.md); it is not a full rerun against this build.

## Integration follow-up

Source review found the existing gallery batch screen and source-category manager had no production navigation entry. They are now linked under **ShinKu Settings → Advanced tools → Advanced Options**, using their existing screen models and validation/mutation boundaries. Batch add still accepts only supported gallery links/visited-gallery ID lists and requires explicit confirmation. Merely opening either screen does not start imports, create categories or modify the library. Existing tag-sort management remains in Library settings.

The owner asked whether phone testing was finished and was told they could lock/disconnect it. The phone disconnected before final navigation cleanup and removal of temporary `/sdcard/shinku-ui-audit.xml`; remove that exact audit file on the next authorized connection. Do not claim that cleanup completed. Remaining integration checks use a disposable emulator.

File-scoped formatting, both dev compilations, debug/test assembly and JVM77 discovered/76 passed/one existing skip passed after the route changes. The production settings-to-source-tools test and existing hub-navigation test passed at both normal and actual1.6× system font: **two tests per configuration**. The new test opens empty batch input, verifies disabled Add, returns, opens source categories and returns without imports or edits. Screenshots were inspected under ignored `build/phone-audit-route-review/1.0` and `1.6`. Large-text batch input scrolls; Add can require scrolling beyond the initial viewport. This is targeted route evidence, not a complete import or every-layout certification. Logs: `build/phone-audit-debug.log`, `build/phone-audit-format.log`, `build/phone-audit-routes-1.0.log` and `build/phone-audit-routes-1.6.log`. Across the documented cumulative runs,52 unique Android cases have passing evidence; there was no single final52-case run. Existing broader checks are described in [the boards17–24 report](boards-17-24-code-pass-2026-11-11.md).

Required optimized `assembleDevRelease --build-cache --parallel --quiet --console=plain` passed; all five APK signatures verified. Latest record: `build/phone-audit-checksums.json`; logs: `build/phone-audit-release.log` and `build/phone-audit-signatures.log`. The intermediate copy-only build is superseded and was not installed.

Final ARM64 APK: `app/build/outputs/apk/dev/release/ShinKu-dev-arm64-v8a-release.apk`, version **2.8.1 / 144**, packaged **2026-11-12 08:50:00 +08:00**, SHA256 **730CE7D26DDB2A0B0E9C0B5273D68B26D66E1703E3AD8BF656417211CE12F829**. This revised artifact was **installed in place**, recorded **08:54:09 +08:00**, and its installed base APK SHA256 independently verified. No new mockup, version bump, commit, push, publication or issue closure occurred.

Latest-artifact phone follow-up passed: Advanced tools opens actual Batch add with empty input/disabled Add, Source categories opens its manager, and the real bundled release notes show the corrected acceptance wording with reachable Close. No input, import, category creation/edit/deletion or reading action was initiated. An initial stale coordinate opened title details/a text menu; backed out without opening a chapter or applying an action, then verified each screen before further taps. Private `followup-*` captures remain ignored. This is targeted acceptance of these states only.

Closed the reviewed screens, returned to main navigation and removed `/sdcard/shinku-ui-audit.xml` successfully. Font1.0 and the existing1080×2376/density480 overrides remain unchanged. This supersedes the earlier disconnected/cleanup-pending checkpoint above.

## Remaining acceptance

Actual device authentication/website sign-in, live reader/translation, authenticated source integrations, merge operations, successful/error restore flows and Android/OEM widget hosts remain pending. Recovery, source metadata, source-tool confirmations and alternate configurations have controlled emulator evidence only where documented. No reading/progress/preference/restore/download/cleanup job or external sharing was initiated on the phone.

## Reader installation checkpoint — 2026-11-12T20:43:34.0251567+08:00

The subsequent Reader audit build supersedes the730CE7 artifact above. Installed base SHA256 `E31F3CF6D8009750A63F4A6C5912E92ABF2A0924BB576B764DB200BDA46E5170` matches `build/reader-audit-checksums.json`. Production main/Settings/Reader settings checked without edits; eight isolated Reader/native fixtures passed after acknowledging the debug native-compatibility warning; startup wait alone did not resolve the failures. Temporary test packages and audit files removed, production main navigation restored, display settings unchanged. Details and remaining live-flow limits: [Reader and translation audit](./reader-translation-audit-2026-11-12.md). No real chapter or provider action initiated; phone no longer needed for this stage.
