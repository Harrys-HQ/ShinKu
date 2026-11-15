# Android native-library compatibility investigation — November 12, 2026

The physical phone displayed an **Android App Compatibility** dialog while running the temporary debuggable Reader fixture app. It reported an ELF alignment failure for `lib/arm64-v8a/libmlkit_google_ocr_pipeline.so` with an `Unknown error` detail. The system dialog covered the fixtures and caused their initial visibility assertions to time out. Waiting for app startup did not resolve those failures; the earlier startup-race diagnosis did not explain the observed blocker.

The production ARM64 APK installed for this review is version 2.8.1/144, SHA256 `E31F3CF6D8009750A63F4A6C5912E92ABF2A0924BB576B764DB200BDA46E5170`. The temporary debug APK and production APK contain identical copies of the three libraries below.

| ARM64 library | Dependency in this repository | LOAD alignment | Additional finding |
| --- | --- | --- | --- |
| `libimagedecoder.so` | `com.github.tachiyomiorg:image-decoder:41c059e540` | 4 KB | Fails the 16 KB ELF alignment requirement |
| `libmediapipe_tasks_text_jni.so` | `com.google.mediapipe:tasks-text:0.10.14` | 4 KB | Also has a RELRO boundary sharing a LOAD segment with later writable data |
| `libmlkit_google_ocr_pipeline.so` | Bundled ML Kit text recognition, version 16.0.1 | 16 KB | Passes the inspected LOAD/RELRO checks despite the phone's `Unknown error` message |

All twelve ARM64 native libraries in each APK were inspected. The other ten, including the OCR library, pass the recorded LOAD alignment checks. Both ARM64 APKs also pass the official `zipalign -c -P 16 -v 4` check. ZIP alignment and ELF segment alignment are separate checks; successful APK packaging does not repair an incompatible prebuilt library. Android documents both requirements and the additional RELRO check in its [16 KB compatibility guide](https://developer.android.com/guide/practices/page-sizes).

The OCR library was also inspected with the installed NDK's `llvm-readelf -Wl`. Its three LOAD segments have alignment `0x4000`, and the RELRO segment ends with its containing LOAD segment. These observations do not establish why this device reports an unknown error for that library. A platform checker/ELF-reading error is a possibility, not a confirmed diagnosis. Android's [native library checker source](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/master/core/jni/com_android_internal_content_NativeLibraryHelper.cpp) distinguishes failures to inspect an ELF from detected 4 KB LOAD segments; the phone's exact build implementation has not been established.

The phone currently reports a 4 KB runtime page size (`getconf PAGE_SIZE` = 4096) and a maximum page-size capability property of 16384. No page-size mode or warning preference was changed. This phone session cannot certify runtime behavior on a device actually using 16 KB pages.

Private evidence and reproduction:

- `build/phone-reader-review/crop-during-wait.png`: system warning covering the synthetic crop controls.
- `build/16kb-review/native-alignment-before.json`: parsed ELF LOAD/RELRO results for both ARM64 APKs.
- `build/16kb-review/debug-zipalign.log` and `release-zipalign.log`: successful APK alignment checks.
- `build/check-native-alignment.py`: local diagnostic parser. The OCR ELF copies are retained under `build/16kb-review/` for independent NDK inspection.

The next compatibility change should update or rebuild the image decoder and MediaPipe dependencies with 16 KB support while preserving their APIs and functionality. Candidate artifacts must be inspected before adoption, followed by image-format/region decoding, native text-embedding/model-loading checks, both 64-bit ABIs, and runtime testing on a genuine 16 KB environment. ML Kit remains a separate investigation until the phone's unknown-error result is explained. The [ML Kit release notes](https://developers.google.com/ml-kit/release-notes) and [text-recognition setup](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) currently list the project's bundled text-recognition version 16.0.1; inventing a version bump is not a fix.

No dependency, native binary, production manifest or packaging setting was changed during this investigation. The optional phone fixture harness acknowledges only the identified debug compatibility dialog with **OK**, logs the acknowledgment, and leaves **Don't Show Again** untouched. That allows component UI testing to proceed; it is not compatibility acceptance. Production compatibility remains open.

## Resolution checkpoint

The subsequent [native/upstream update](./native-upstream-update-2026-11-12.md) replaces the image decoder and MediaPipe binaries. All five optimized APKs pass native/ZIP alignment checks; a clean 64-case suite passes on an actual 16 KB runtime, including local OCR, real sentence embeddings and region decoding. The current phone's 12-case native/UI/locale review passes without acknowledging a compatibility dialog; its package compatibility flag is 0. Final installed ARM64 SHA256 `4F584413D9ED639F6E77B9B20A6C362E1C646CAB31D388F08DA9755D12D3742B`. ML Kit remains 16.0.1 and successfully performs local OCR at 16 KB; no ML Kit version bump or warning suppression was required. The earlier scanner's precise Unknown error implementation remains unexplained, but the observed warning is absent with the corrected dependencies. The compileSDK 37/AGP support warning is separately documented and is not an Android native-compatibility popup.
