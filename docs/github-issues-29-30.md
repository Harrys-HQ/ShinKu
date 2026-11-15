# GitHub issues 29 and 30 — November 13, 2026

Baseline: `a931760a62` plus the existing uncommitted UI/native work. The open-issue list was fetched from GitHub on November 13. These are local fixes; the issues remain open until the required publication workflow is completed.

## #29: manga details pull-to-refresh

[Issue](https://github.com/Harrys-HQ/ShinKu/issues/29). The manga details screen retained its refresh toolbar callback and loading state, but rendered its lazy list without the shared `PullRefresh` wrapper. Phone and the current tablet fallback both use that layout.

Restored the wrapper around the scrolling content. It invokes the same metadata/chapter refresh action as the toolbar, displays the existing loading state, places the indicator below the toolbar and disables the gesture during chapter selection. No network scheduling, chapter ordering or refresh persistence logic changes.

## #30: animated AVIF covers and reader pages

[Issue](https://github.com/Harrys-HQ/ShinKu/issues/30). The privately downloaded reported cover is a 480×674 AVIF sequence with the `avis` brand. On the actual 16 KB API 35 emulator, the retained native decoder reports `Avif,isAnimated=false` and cannot open it; Android's platform decoder successfully returns an `AnimatedImageDrawable`. This reproduces the failure independently of extensions.

[Coil 3.4.0's animation factory](https://github.com/coil-kt/coil/blob/3.4.0/coil-gif/src/main/java/coil3/gif/AnimatedImageDecoder.kt) recognizes GIF, animated WebP and animated HEIF, and does not select AVIF sequences. ShinKu's earlier custom decoder claims AVIF first, preventing the working platform path from being used.

Added bounded, non-consuming ISO BMFF `ftyp` brand detection for AVIF sequences. On Android 12+ devices that advertise AVIF image decoding, these sequences now use Coil's platform animation decoder before the native still-image or strip paths. Reader animation detection uses the same sequence check, allowing its existing animated image view and playback lifecycle to handle the content. Static AVIF stays on the existing path; GIF and animated WebP retain Coil's existing decoder selection.

The change adds no native library or toolchain dependency. Playback on older Android releases or devices without platform AVIF support is not promised. Existing still-image/native fallback remains; a portable animated AVIF native engine would be a separate migration.

## Validation

- Both dev Kotlin compilers, debug/test assembly, scoped formatting and JVM tests passed: 81 discovered, 80 passed, one existing skip; no failures/errors.
- Final full Android suite: **OK (68 tests)** on the actual 16 KB API 35 emulator, including the private reported cover and real embedding model. `build/issue-review/full-tests.log`.
- The full-screen pull gesture invokes the real manga screen callback on phone/tablet fallback. Animation tests inspect Coil's wrapped `AnimatedImageDrawable` and verify playback starts for generated AVIF/GIF/WebP and the reported cover. Detection preserves the source and rejects static AVIF/truncated data.
- Initial animation assertions expected an unwrapped drawable; corrected to inspect `ScaleDrawable.child`. One repeated refresh-only run completed its assertions then hit an Android ART method-resolution crash during activity pause; logs retained in `build/issue-review/emulator-crash.log`. The earlier targeted refresh run and final full suite pass. This is an observed emulator interruption, not proof that every device lifecycle is certified.
- Required optimized assembly passed; all five APK signatures and 16 KB ELF/RELRO/ZIP alignment checks passed. Record: `build/issue-review/checksums.json`. ARM64 version 2.8.1/144, SHA256 `9C7812BF8300464A8D7235FB9492DD33B94B643F0CED3957A662FE69AF255B7B`.
- Optimized x86-64 APK cold-started successfully on the isolated 16 KB emulator; app process remained alive and the UI contained no Android compatibility dialog (`release-startup.log`, `release-ui.xml`).
- Phone installation: pending; November12 build remains the last verified phone installation. No new phone visual review was performed in this pass.

Fixtures under `app/src/androidTest/assets/animated-images` are generated solid-color images; no reported artwork or signed download URL is committed. The optional private reported-cover test uses `coverPath`. Existing compile SDK37/AGP support warning remains documented in the [native update report](./native-upstream-update-2026-11-12.md).

No version bump, commit, push, release publication, GitHub comment or issue closure occurred. The isolated emulator was stopped after verification. Broader live-source and all-board visual acceptance remains pending.
