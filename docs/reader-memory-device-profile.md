# Installed-app memory and frame profile

Captured 2026-11-03 via ADB on CPH2691, installed ShinKu 2.8.0 (142). This is a baseline of the installed app, not validation of the current uninstalled optimization changes.

The automated flow dismissed an existing filter sheet, scrolled Library four times down and four times back, resumed an existing chapter, made five reader swipes (ending at page 2/24), opened Area OCR (ShinKu Lens), closed it, and returned to Library. Normal reading progress/history can change when opening a chapter. No APK was installed and no app data was cleared. Translation inference and full-chapter translation were not run.

| Stage | Reported total PSS KiB | Reported swap PSS KiB | Bitmap malloc + nonmalloc KiB |
|---|---:|---:|---:|
| Library idle | 327569 | 184433 | 274409 |
| Library after scrolling | 357800 | 184349 | 286559 |
| Chapter open | 442761 | 183756 | 354380 |
| Reader after swipes | 428113 | 183604 | 354380 |
| Area OCR selector open | 450151 | 182988 | 364520 |
| Back in Library | 438754 | 182932 | 364520 |
| Library after another 10 seconds | 423754 | 182932 | 354422 |

These are dumpsys snapshots, not peak measurements or a heap ownership trace. Bitmap allocation totals and PSS are distinct accounting measures and must not be added together. This process already had substantial swapped memory and prior app use; it was not restarted for a cold baseline. No forced garbage collection was performed.

After resetting gfxinfo immediately before Library scrolling, eight swipes produced 425 frames, 18 janky (4.24%), and a 14 ms 95th percentile. After resetting before reader swipes, five swipes produced 293 frames, 5 janky (1.71%), and an 8 ms 95th percentile. These are synthetic input runs with dump/UI inspection overhead, not representative user benchmarks or an old/new comparison.

Opening the OCR selector increased recorded bitmap allocation by about 10 MiB; that increase had cleared in the settled snapshot. Approximately 78 MiB more bitmap allocation remained than at the pre-reader Library snapshot, which can be normal reader/image caching. A single cycle does not establish a leak or justify reducing Coil's cache ceiling.

Next measurement: repeated reader return cycles with the same chapter, actual OCR and translation inference, per-image ownership/retention tracing, and comparisons on the newly built app and other memory classes. Current cache budgets remain unchanged pending that evidence. Extreme cover concurrency changes are also provisional until workload timing is measured.

Raw captures and accessibility dumps are under ignored build/device-benchmark/memory. Only anonymous metrics are included here; reading titles and page content are omitted.
