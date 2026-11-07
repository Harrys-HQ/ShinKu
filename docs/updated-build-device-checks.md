# Updated-build device smoke checks

2026-11-03, CPH2691 over ADB. Installed verified ARM64 devRelease as an update: 2.8.0 (142) → 2.8.1-preview.1 (143). The interrupted install had completed; this was checked before any retry. Existing Library and chapter resume state were present after the update. No app data was cleared.

Observed on the updated build:

- Library and resume UI rendered. One synthetic Library scroll capture recorded zero frames; discard it as a timing sample. It does not demonstrate faster rendering.
- Browse/Discover displayed recommendations. Performed a pull gesture, waited, opened Titles For You via See All, and scrolled the grid. This is navigation/content smoke coverage, not proof of request timing, new result counts, or each pagination failure/end condition. Those branches passed the local model tests.
- Resumed the existing chapter at page 2/24 and scrolled its long image. Opened Area OCR, submitted one translation, observed processing, and visually confirmed an overlay. The default crop clipped dialogue; this validates pipeline/result delivery, not OCR or translation accuracy. No language setting was changed; source and target in this sample were English.
- Translation Options initially showed Inspect Page Bubbles (1). Selected Re-Translate Current Page and observed the OCR processing dialog rather than an immediate old-cache restoration. Processing finished and the inspector showed 5 blocks, replacing the earlier cropped result.
- Returned out of the reader, force-stopped and relaunched the app without clearing data, then reopened the same resumed chapter. Translation Options still showed 5 blocks without submitting translation again. This confirms persistent restoration for the newly written page record.
- A navigation detour after restart opened another title and selected a chapter; selection was cancelled using the app UI without invoking chapter actions. Testing can update normal reading progress/history. Restored the intended chapter to confirm persistence.

Limits: no timing comparison with the old warm process; no repeated long-chapter retention/peak-heap study; no target-language switch, legacy-cache migration on this phone, induced source error/retry, full-chapter translation, large cover-update workload, or native embedding/model recovery exercise. Those remain explicit follow-ups. Avoid claims that every optimization is device-validated or that performance has improved from these smoke checks.

Raw UI evidence, local screenshots, and metric files are in ignored build/device-benchmark/updated; personal reading titles, image content and provider details are omitted here. All 26 focused local regression tests and the signed release build had already passed before this device pass. This is local validation, not a published release.


## Follow-up correctness and retention checks

Language UI: switched the target from English to French. Translation Options offered Translate Current Page and showed no English block inspector. Restored English and observed Inspect Page Bubbles (5) without submitting another translation. English was restored before leaving the test; no French translation was purchased/generated. This checks language selection isolation and English restoration, not the quality of French output.

Isolated recovery checks: extracted the existing staging/validation/atomic-publication sequence into ModelFilePublisher, used by MangaEmbeddingJob. New tests simulate interrupted writes, failed validation, cancellation, and successful retry using temporary files. The previous target survives failures; validation sees the staged replacement before publication; the temporary file is removed. All 29 focused local tests passed. The validator in production still uses the native MediaPipe loader. Native model loading/download integration and corrupt-model recovery on the phone remain untested; no existing phone model files were altered.

Source failure/retry, duplicate-page continuation and final-page exhaustion were checked by the existing model tests, rerun successfully. Legacy chapter migration, invalidation across restart, corrupt page isolation and concurrent saves were checked by the existing disk-store tests, rerun successfully. These are isolated deterministic tests, not induced source/network faults or legacy-cache corruption on the phone.

A six-cycle device retention run confirmed Library was visible before each sample. The earlier unconfirmed sample and failed navigation attempts are excluded. Reported bitmap allocation (malloced plus nonmalloced) grew across the six valid samples: 299395, 374381, 436827, 511813, 592927, 661785 KiB. After another 20 seconds it remained 656055 KiB. Activity object counts also increased, reaching 10; these are heap object counts, not proof of ten live activity stack entries. A controlled RUNNING_CRITICAL trim signal did not materially reduce allocations. No forced GC or heap ownership trace was taken, so a specific retained-object root has not been established.

Source inspection found viewer destroy methods cancelled scopes but did not detach adapters. A targeted teardown change now detaches RecyclerView/ViewPager adapters (to invoke holder recycling), clears the webtoon recycled pool/listeners, and clears recycled AppCompatImageView drawables after cancelling image requests. Cache budgets remain unchanged. The follow-up build and repeated device result will be recorded below when complete.


### Reader teardown result

The optimized devRelease build with explicit reader adapter/drawable teardown succeeded, all five APK signatures verified, and the ARM64 build was installed as a data-preserving update. Build log: build/reader-teardown-release.log; checksums: build/reader-teardown-checksums.json. Version remains 2.8.1-preview.1/143.

Repeated the six confirmed resume/return cycles after installation, in the same chapter. Each measurement was recorded only after Library Resume was visible. Bitmap allocation totals after successive returns were 18170, 23940, 23982, 24023, 30193, 24106 KiB; after a further 20 seconds, 18374 KiB. The corresponding final reported PSS was 131287 KiB. The earlier run reached 661785 KiB bitmap allocation after six cycles and settled at 656055 KiB.

This supports explicit holder/image cleanup addressing the observed large bitmap accumulation in this flow. The new installation started a fresh process whereas the previous process had prior OCR activity; do not turn these numbers into a precise overall memory-saving percentage or a speed claim. The within-run growth patterns are the useful observation. Managed Activity object counts still rose (one to seven); without a forced-GC/heap ownership trace this does not prove either collection or a persistent leak. Longer sessions, other chapters, pager mode and retained-object tracing remain follow-ups. No cache limits were reduced.

Returned the phone to Library, with English restored. No existing model files were damaged or deleted for these tests, and no phone-wide connectivity changes were made. Cover workload benchmarking and native model inference/download recovery on-device remain unverified. Work is local and no release was published.

Final post-teardown regression rerun: 29 tests, zero failures/errors. The final phone process had no fatal exception markers in the inspected AndroidRuntime log.
