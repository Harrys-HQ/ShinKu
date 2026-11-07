# ShinKu mockup checklist

Updated: 2026-11-06.

Generated = image exists. Code status records implementation work, including retained existing UI. Visual completion requires comparison of the actual app with the approved image. Tests/builds do not establish visual fidelity. Latest physical-phone artifact verification is separate.

- [x] 14/24 numbered boards generated (42/72 panels).
- [x] Source-informed percentage audit recorded for 14 generated boards / 39 panels plus six earlier concepts. Visual acceptance remains below 100%.
- [x] 67 JVM tests and 22 Android emulator tests passed; all five optimized APK signatures verified.
- [ ] Verify the newest artifact on the physical phone; transfers via LocalSend have not been independently verified.
- [ ] Finish the documented visual gaps and obtain complete runtime comparisons for every panel.
- [ ] Generate remaining boards 15–24.

Historical quota observation: image generation reached its usage limit on 2026-11-05. Tool-estimated reset was 2026-11-06 09:45:35 Asia/Shanghai (tool estimate); quota has not been rechecked during this audit. Resume at board 15.

Percentages are provisional supported-scope estimates, weighted 70% toward layout/styling. They are not pixel measurements or test coverage. See the [full scoring/evidence report](../mockup-visual-audit.md).

| Board | Mockup | Image | Implementation estimate | Evidence / visual status | Phone verification |
| --- | --- | --- | --- | --- | --- |
| 01 | Discovery & search | Generated | 95% — visual acceptance pending | UI code applied across Discover, Global search and Source catalogue; final phone/theme acceptance pending | Latest artifact unverified |
| 02 | Sources, extensions & repositories | Generated | 95% — visual acceptance pending | UI code applied across Sources, Extensions and Repositories; final phone/theme acceptance pending | Latest artifact unverified |
| 03 | Source migration | Generated | 90% — visual acceptance pending | UI code applied across Migrate sources, Migration options sheet and Migration review list; final phone/theme acceptance pending | Latest artifact unverified |
| 04 | Updates, history & downloads | Generated | 95% — visual acceptance pending | UI code applied across Updates, History and Download queue; final phone/theme acceptance pending | Latest artifact unverified |
| 05 | Library organization | Generated | 95% — visual acceptance pending | Default and actual 1.6× text reviewed; final phone/theme acceptance pending | Latest artifact unverified |
| 06 | Reading & chapter navigation | Generated | 95% — visual acceptance pending | UI code applied across Paged reader, Webtoon reader, and Navigate sheet; final phone/theme acceptance pending | Latest artifact unverified |
| 07 | Translation in the reader | Generated | 95% — visual acceptance pending | UI code applied across Area selection, Inspect bubbles, and Chapter translation; final phone/theme acceptance pending | Latest artifact unverified |
| 08 | Reader settings & page actions | Generated | 95% — visual acceptance pending | UI code applied across Reader settings, Translation options, and Page actions; final phone/theme acceptance pending | Latest artifact unverified |
| 09 | Getting started | Generated | 95% — visual acceptance pending | UI code applied across Welcome hero, Storage & permissions, and Guides; final phone/theme acceptance pending | Latest artifact unverified |
| 10 | Backups, restore & sync | Generated | 95% — visual acceptance pending | UI code applied across Create backup, Restore preview, and Cloud sync; final phone/theme acceptance pending | Latest artifact unverified |
| 11 | Tracking & privacy | Generated | 95% — visual acceptance pending | UI code applied across Trackers, Link a title, and Security & privacy; final phone/theme acceptance pending | Latest artifact unverified |
| 12 | Core settings & appearance | Generated | 95% — visual acceptance pending | UI code applied across Settings root, Appearance cover preview and Library settings; final phone/theme acceptance pending | Latest artifact unverified |
| 13 | Reader, browse & download preferences | Generated | 95% — visual acceptance pending | UI code applied across Reader preferences, Browse preferences and Download preferences; final phone/theme acceptance pending | Latest artifact unverified |
| 14 | Performance & maintenance | Generated | 95% — visual acceptance pending | UI code applied across Performance mode, Maintenance tools and Source health; final phone/theme acceptance pending | Latest artifact unverified |
| 15 | Reading journey & identity | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 16 | Empty, loading & offline states | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 17 | Dialogs, permissions & authentication | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 18 | Recovery, diagnostics & release information | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 19 | Tablet & landscape layouts | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 20 | Light, high-contrast & E-Ink | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 21 | Specialist source integrations | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 22 | Extension details & merged manga | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 23 | Batch tools & advanced organization | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |
| 24 | Widgets & system surfaces | Prompt ready | 0% planned work; visual match unscored | No image target yet | Not applicable |

*Functional scope previously counted existing UI retained alongside new code changes. It must not be read as completed visual implementation. See the [visual-audit correction](../mockup-visual-audit.md) and [earlier implementation review](../generated-mockups-completion.md).

## Earlier concept boards (separate from 01–24)

| Concept panel | Image | Implementation estimate | Visual status |
| --- | --- | --- | --- |
| Library / Continue Reading | Generated | 75% partial (provisional) | Source + partial runtime capture; gaps remain |
| Manga details | Generated | 55% partial (provisional) | Source only; runtime comparison pending; gaps remain |
| More / complete statistics | Generated | 75% partial (provisional) | Source + partial runtime capture; gaps remain |
| ShinKu settings hub | Generated | 80% partial (provisional) | Source + partial runtime capture; gaps remain |
| AI / translation setup | Generated | 60% partial (provisional) | Source + partial runtime capture; gaps remain |
| Reader controls | Generated | 60% partial (provisional) | Source + partial runtime capture; gaps remain |

## Current build and next batch

Latest verified local APKs remain version 2.8.1-preview.1 / 143. Use build/library05-checksums.json to distinguish this build from earlier previews. Phone installation is tracked separately above.

Generated boards below 100% visual acceptance: 01–13. Future boards use built-in image_gen with the two earlier concept boards as style references. Exact input is the numbered prompt file from Use case onward; only the historical blocked-status header is omitted. Image review notes are in [manifest.json](manifest.json) and the [gallery](index.html). Next generation starts at [14-prompt.txt](14-prompt.txt).
