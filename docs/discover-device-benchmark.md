# Discover source-loading comparison

Tested on 2026-11-03 with the owner's connected CPH2691 phone running Android 17 and installed ShinKu 2.8.0 / 142. The installed app was not upgraded, downgraded, cleared, or restored. The temporary instrumentation APK was removed after testing.

## Finding

The four-overall/two-per-source request limits increased median completion time for the tested source workload. A six-overall/three-per-source candidate restored comparable speed while retaining finite concurrency. Prefer evaluating that candidate as the next adjustment rather than assuming four/two is a performance improvement.

These are source-operation timings on one phone and network session. They are **not end-to-end refresh, first visible suggestion, frame-rate, or battery measurements**. After reviewing these results, the owner authorized adopting six/three, and the production limiter defaults and regression expectations were updated. The phone measurements below were taken in the harness, not in an installed updated production APK.

## Main comparison

Three trials per mode, fixed page one, ten-second pauses after each trial. The order was old, four/two, six/three, four/two, six/three, old, six/three, old, four/two, balancing each mode's average position in the sequence. Sources, genres, request counts, and timeout values were held constant within the session.

| Mode | First nonempty response, median | Top-section source operations, median | All tested source operations, median | Failed operations |
| --- | --- | --- | --- | --- |
| Old dispatcher-only behavior | 251 ms | 1,044 ms | 1,561 ms | 0 / 36 |
| Four/two limits | 272 ms | 1,008 ms | 2,001 ms | 3 / 36 |
| Candidate six/three limits | 212 ms | 792 ms | 1,316 ms | 0 / 36 |

For this workload, four/two's median overall source completion was 440 ms, approximately 28%, longer than the old behavior. Six/three was 685 ms, approximately 34%, shorter than four/two. Small sample sizes and network variation prevent concluding that six/three is universally faster than the old implementation.

The three recorded failures were HTTP 429 responses in the four/two trials. This does not establish that four/two caused rate limiting, or that six/three prevents it: server quotas, prior requests, cache state, and other traffic were not controlled. Concurrency bounds alone are not a requests-per-second policy. The observed failures provide no evidence of a reliability gain from the tighter caps in this session.

| Trial | Mode | First response | Top source stage | All source stages | Failures | Raw records returned |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Old | 454 ms | 1,158 ms | 1,679 ms | 0 | 242 |
| 2 | Four/two | 272 ms | 1,667 ms | 2,547 ms | 0 | 242 |
| 3 | Six/three | 212 ms | 792 ms | 1,307 ms | 0 | 242 |
| 4 | Four/two | 278 ms | 906 ms | 1,876 ms | 1 | 218 |
| 5 | Six/three | 251 ms | 907 ms | 1,470 ms | 0 | 242 |
| 6 | Old | 251 ms | 1,044 ms | 1,561 ms | 0 | 242 |
| 7 | Six/three | 171 ms | 722 ms | 1,316 ms | 0 | 242 |
| 8 | Old | 175 ms | 663 ms | 1,169 ms | 0 | 242 |
| 9 | Four/two | 162 ms | 1,008 ms | 2,001 ms | 2 | 194 |

Raw records include duplicates across source queries and shelves. They are not unique suggestions or the number of cards the production UI would show.

## Method and scope

- Built a separate release-signed instrumentation APK and verified its signing certificate matches the installed app. Used Android's [instrumentation support](https://developer.android.com/studio/test/advanced-test-setup) to execute the harness within the existing app process. Only the test APK was installed.
- Reused the installed app's source manager, preferences, source health data, reading-stat genre preferences, network clients, and extension implementations. No API keys or reading titles were copied into the report or results.
- Selected two sources from seven eligible sources using the existing eligibility/health rules and a fixed shuffle seed. Kept the selected sources and genre workload identical for every mode in a session. This deliberately freezes the production feed's randomized choices; it does not replay every possible source combination.
- Each trial executed six popular/latest/behavior operations for the top section, followed by six genre operations. Each operation invoked the actual extension catalogue method. Library fallback data, local manga mapping/database writes, image downloads, AI generation/search, custom saved feeds, Compose rendering, and cancellation/reselection behavior were outside this harness.
- The old mode used `Dispatchers.IO.limitedParallelism(4)` with the old eight-second response timeout. The current limiter implementation was copied unchanged into the test APK under a distinct package and executed with four/two or six/three constructor limits. Queue time is excluded from the response timeout for both bounded modes, as in the implementation.
- Measured elapsed wall-clock times, successful/nonempty responses, failures, raw returned-record counts, and active-operation peaks. Observed peaks were six overall/three per source in the old mode, four/two in the current mode, and six/three in the candidate. Old mode's observed peak is specific to this workload, not its maximum possible concurrency.
- The same installed binary hosted all three modes, avoiding changes in app version, R8 configuration, or library data between modes. This also means the full new APK and its refresh UI were not exercised. Existing unit tests separately cover request cancellation and late-result rejection.
- No caches were cleared and no unmeasured warm-up phase was used. First-trial initialization, source-side caches, JIT warming, network variability, and shared server quotas remain possible confounders. Medians and balanced ordering reduce some effects but do not remove them.

## Preliminary rapid series

Before the main comparison, eight trials alternated old and four/two modes with two-second gaps and mixed pages one/two. Median overall source completion was 1,268.5 ms versus 1,842 ms. There were 10/48 and 11/48 failed operations respectively; HTTP status codes were not captured in that first series. Because pagination and transient failures could affect the comparison, the main series fixed page one, increased spacing, and recorded HTTP codes. Both series are preserved in the [raw anonymous results](./benchmarks/discover-2026-11-03.json).

In the raw JSON, `old` means dispatcher-only scheduling, `new` means the four/two configuration tested at that time, and `six_three` means the six/three candidate subsequently adopted as the default.

## Reproduction artifacts and next step

The local harness source, copied limiter, Gradle init script, build log, installed-APK certificate reference, and raw output remain under ignored `build/device-benchmark/`. These are local artifacts and can be removed by a clean build. Permanent raw results are linked above. The custom runner was selected through a Gradle init script; the app's tracked build configuration and production source were not changed for this test.

The owner approved six/three as the default; cancellation, shared permits, source timeouts, and guarded publication are retained. Next, verify the updated APK's actual feed UI on-device, including saved feeds and configured AI paths, before claiming complete refresh-speed improvement. Do not extrapolate three trials per mode into a general source-reliability guarantee.
