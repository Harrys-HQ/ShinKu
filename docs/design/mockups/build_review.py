"""Build the local mockup gallery and source-surface checklist. No network calls."""

import csv
import html
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
manifest = json.loads((HERE / 'manifest.json').read_text(encoding='utf-8'))
boards = manifest['boards']
by_id = {b['id']: b for b in boards}
audit = json.loads((HERE / 'implementation-audit.json').read_text(encoding='utf-8'))
audited = {b['id']: b for b in audit['boards']}


def raw_score(panel):
    assert len(panel['scores']) == 4 and all(isinstance(v, int) and 0 <= v <= 4 for v in panel['scores'])
    return sum(v * weight / 4 for v, weight in zip(panel['scores'], audit['weights']))


def nearest_five(value):
    return int(value / 5 + 0.5) * 5


assert sum(audit['weights']) == 100
for entry in audit['boards']:
    assert len(entry['panels']) == 3
    assert entry['percent'] == nearest_five(sum(raw_score(p) for p in entry['panels']) / 3)
    assert by_id[entry['id']]['implementationPercent'] == entry['percent']
for panel in [p for entry in audit['boards'] for p in entry['panels']] + audit['earlierConcepts']:
    assert panel['percent'] == nearest_five(raw_score(panel))
    if panel.get('screenshot'):
        assert (ROOT / panel['screenshot']).is_file(), panel['screenshot']
    if panel.get('largeTextScreenshot'):
        assert (ROOT / panel['largeTextScreenshot']).is_file(), panel['largeTextScreenshot']


def score_tag(entry):
    if entry.get('status', '').startswith('implemented_locally'):
        return f"{entry['percent']}% — visual acceptance pending"
    return f"{entry['percent']}% — partial (provisional)"


def evidence_label(panel):
    if panel.get('largeTextScreenshot'):
        return 'Source + default / actual 1.6×-text runtime'
    return 'Source + partial runtime capture' if panel.get('screenshot') else 'Source only; runtime comparison pending'


audit_report = [
    '# Mockup implementation percentage audit', '', f"Updated {audit['date']}. Baseline: {audit['baseline']}.", '',
    'The previous all-mockups-applied claim is withdrawn. Working controls and retained UI are not proof of a completed redesign. '
    'Generated numbered boards still have visual implementation or acceptance work; none is certified 100%.', '',
    '## How to read the percentages', '', audit['method'], '',
    'These are review estimates anchored to explicit criteria, not exact measured percentages. '
    'Layout includes hierarchy, grouping and placement; styling includes surfaces, icons, typography and spacing. '
    'Controls cover supported actions and navigation; states cover selection/loading/error/confirmation behavior. '
    'Retained UI earns credit where it already meets the design. A 3 means mostly present, not visually accepted. '
    'A missing capture is an evidence limitation, not proof a feature is absent. No score represents phone installation.', '',
    'All 13 numbered images and both earlier concept images were inspected. Current relevant source and available local captures were compared. '
    'Existing captures predate some latest fixes; current source takes precedence. Fixture screenshots validate only their rendered control, '
    'not the surrounding scaffold/theme. The file named dock-browse.png actually shows History and was excluded as Browse evidence. '
    'Theme choice, manga artwork, sample counts and configurable grid density are not fidelity defects by themselves. '
    'Private runtime images remain in ignored build directories; no reading records are reproduced here.', '',
    '## Numbered boards', '', '| Board | Mockup | Implementation tag | Panel tags (in image order) |', '| --- | --- | --- | --- |',
]
for board in boards:
    entry = audited.get(board['id'])
    tag = score_tag(entry) if entry else '0% planned redesign work; visual match unscored'
    panels = '; '.join(f"{p['name']}: {p['percent']}%" for p in entry['panels']) if entry else 'Not started'
    audit_report.append(f"| {board['id']} | {board['title']} | {tag} | {panels} |")
audit_report += ['', audit['plannedNote'], '', '## Panel evidence and remaining work', '']
panel_rows = []
for board in audit['boards']:
    audit_report += [f"### {board['id']} — {by_id[board['id']]['title']} ({board['percent']}%)", '',
                     '| Panel | Tag | L / S / C / States (0–4) | Present | Remaining | Supported-scope adaptation | Evidence |',
                     '| --- | --- | --- | --- | --- | --- | --- |']
    for panel in board['panels']:
        scores = ' / '.join(str(v) for v in panel['scores'])
        evidence = evidence_label(panel)
        if panel.get('screenshot'):
            evidence += f"; local `{panel['screenshot']}`"
        audit_report.append(f"| {panel['name']} | {panel['percent']}% | {scores} | {panel['present']} | {panel['remaining']} | {panel['adaptation']} | {evidence} |")
        panel_rows.append(dict(board=board['id'], panel=panel['name'], percent=panel['percent'],
                               layout=panel['scores'][0], styling=panel['scores'][1], controls=panel['scores'][2], states=panel['scores'][3],
                               evidence=evidence_label(panel), remaining=panel['remaining']))
    audit_report += ['', 'Source evidence:', '']
    for source in board['sources']:
        assert (ROOT / source).is_file(), source
        audit_report.append(f'- [{Path(source).name}](../../{source})')
    if board.get('parts'):
        audit_report += ['', 'Individual part log (functional-part completion is separate from whole-panel fidelity):', '',
                         '| Part | Tag | Evidence / remaining acceptance |', '| --- | --- | --- |']
        for part in board['parts']:
            audit_report.append(f"| {part['name']} | {part['percent']}% | {part['note']} |")
        audit_report += ['', '[Dated progress checkpoints](mockups/05-implementation-progress.md).']
    audit_report.append('')
audit_report += ['## Earlier concept panels', '', '| Panel | Tag | Present | Remaining | Evidence |', '| --- | --- | --- | --- | --- |']
for panel in audit['earlierConcepts']:
    assert (ROOT / panel['source']).is_file(), panel['source']
    audit_report.append(f"| {panel['name']} | {panel['percent']}% partial | {panel['present']} | {panel['remaining']} | {evidence_label(panel)}; [{Path(panel['source']).name}](../../{panel['source']}) |")
audit_report += ['', 'Reader controls overlaps boards 06–08. Earlier concepts are not additional independent delivery units; do not add these percentages into a project-wide total.', '',
                 '## Next implementation order', '',
                 ('1. Board 05: verify the latest artifact and final visual acceptance on the owner’s phone; see the per-part log.'
                  if audited['05'].get('parts') else
                  '1. Board 05: filter heading/badge/icon cards, real category counts and bulk More hierarchy.'),
                 '2. Boards 08 and 12–13: shared sheet headers, icon rows, compact groups and action buttons; apply consistently.',
                 '3. Boards 02–03 and 09–11: finish the page compositions around existing functional flows.',
                 '4. Finish remaining details in 01, 04, 06–07 and earlier concepts; capture each actual screen/state for visual acceptance.', '',
                 'A percentage rises only after the listed gap is implemented and evidence is updated. 100% requires a complete supported-scope comparison, documented adaptations and no known visual gaps.', '',
                 ('The initial audit was documentation-only. The subsequent authorized board-05 implementation is recorded in '
                  '[its progress log](mockups/05-implementation-progress.md), with new code, runtime checks and APK verification. '
                  'Phone installation and final visual acceptance remain separate. Tests are functional validation, not pixel-similarity measurements.'
                  if audited['05'].get('parts') else
                  'This is a documentation audit. No app source, APK, phone setting, reading history or streak was changed. '
                  'No new test run is claimed. Prior 67 JVM passes / one skip and 17 Android passes remain functional validation only.'), '']
(HERE.parent / 'mockup-visual-audit.md').write_text('\n'.join(audit_report), encoding='utf-8')
with (HERE / 'panel-implementation-audit.csv').open('w', newline='', encoding='utf-8-sig') as stream:
    writer = csv.DictWriter(stream, fieldnames=list(panel_rows[0]))
    writer.writeheader()
    writer.writerows(panel_rows)


def family(name, path):
    """Map source entry points to a board; shared primitives remain explicitly templates."""
    if path.startswith('presentation-widget/'):
        return '24', 'platform/widget representative; verify supported sizes'
    checks = [
        ('23', r'BatchAdd|SmartSearch|SourceCategory|SortTag'),
        ('03', r'Migrat|DeadSource|FailedUpdates'),
        ('22', r'Merged|MangaNotes|ExtensionDetails|SourcePreferences'),
        ('21', r'EhLogin|EhScreen|Exh|Mangadex|MangaDex|MetadataView|ConfigureExh'),
        ('14', r'Debug|WorkerInfo|BackupSchema|ClearDatabase|SourceHealth'),
        ('18', r'Crash|About|OpenSource|WhatsNew|NewUpdate'),
        ('10', r'Backup|Restore|SyncTrigger|SyncSettings|SettingsData'),
        ('17', r'WebView|Login|OAuth|ConfirmDialog|WarningDialog'),
        ('11', r'Track|Security|Unlock|Biometric'),
        ('15', r'Stats|Journey|ReaderIdentity|ShareCard'),
        ('09', r'Onboarding|ThemeStep|StorageStep|PermissionStep|GuidesStep'),
        ('02', r'ExtensionRepos|Extensions|ExtensionFilter|SourcesScreen|SourcesTab|SourcesFilter'),
        ('01', r'GlobalSearch|BrowseSource|FeedScreen|FeedTab|SourceFeed|SuggestedTitles|Recommends'),
        ('04', r'Updates|History|DownloadQueue|Upcoming'),
        ('05', r'LibrarySettings|CategoryScreen|ChapterSettings'),
        ('07', r'AreaTranslation|BubbleInspector|BatchTranslation'),
        ('08', r'ReaderSettingsDialog|TranslationOptions|PageActions|OrientationSelect|ReadingModeSelect|ModeSelection'),
        ('06', r'ReaderActivity|ChapterList|PagePreview'),
        ('13', r'SettingsReader|SettingsBrowse|SettingsDownload'),
        ('14', r'SettingsAdvanced'),
        ('12', r'SettingsMain|SettingsAppearance|SettingsLibrary|SettingsSearch|AppLanguage|^SettingsScreen$'),
        ('00B', r'SettingsShinKu'),
        ('00A', r'^MangaScreen$|^MoreScreen$|LibraryTab'),
        ('19', r'HomeScreen|MainActivity'),
        ('20', r'GesturePreview'),
        ('16', r'EmptyScreen'),
    ]
    for board_id, pattern in checks:
        if re.search(pattern, name):
            return board_id, 'family representative; inspect individual behavior'
    logical_path = path.split('/com/shinku/reader/', 1)[-1]
    if logical_path.startswith(('presentation/reader/', 'ui/reader/')):
        return '08', 'shared reader pattern; inspect individual behavior'
    if '/settings/' in path or '/setting/' in path:
        return '12', 'shared settings template; inspect every preference'
    return '17', 'shared surface template; manual review required'


rows = []
source_roots = [ROOT / module / 'src/main/java/com/shinku/reader' for module in ('app', 'presentation-core', 'presentation-widget')]
source_paths = sorted(path for folder in source_roots for path in folder.rglob('*.kt'))
for path in source_paths:
    source = path.read_text(encoding='utf-8')
    names = set(re.findall(r'\bfun\s+([A-Z]\w*(?:Screen|Dialog|Sheet|Step))\s*\(', source))
    if path.stem.endswith(('Screen', 'Dialog', 'Sheet', 'Tab', 'Activity', 'Step')):
        names.add(path.stem)
    if 'presentation-widget' in path.parts and path.stem.endswith('Widget'):
        names.add(path.stem)
    for name in sorted(names):
        relative = path.relative_to(ROOT).as_posix()
        board_id, coverage = family(name, relative)
        if board_id in by_id:
            board = by_id[board_id]
            board_name = board['title']
            visual = score_tag(audited[board_id]) if board_id in audited else 'not_started; visual_match_unscored'
        else:
            board_name = 'Initial main-screen direction' if board_id == '00A' else 'Initial inner-screen direction'
            visual = 'direction_liked; concept only'
        rows.append({
            'surface': name, 'source_file': relative, 'board': board_id,
            'board_name': board_name, 'visual_status': visual,
            'coverage_kind': coverage, 'implementation': 'partial; board_estimate_not_individual_surface_score' if board_id in audited or board_id in {'00A', '00B'} else 'not_started',
            'device_validation': 'pending',
        })

with (HERE / 'source-surface-checklist.csv').open('w', newline='', encoding='utf-8-sig') as stream:
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)

e = html.escape


def card(board):
    ready = (HERE / board['file']).exists()
    status = 'ready' if ready else 'pending'
    preview = (
        f'<a href="{e(board["file"])}" target="_blank"><img src="{e(board["file"])}" '
        f'loading="lazy" alt="{e(board["title"])} concept board"></a>'
        if ready else '<div class="placeholder">Image pending — prompt ready<br>'
        '<small>All three panel briefs are saved below.</small></div>'
    )
    details = ''.join(f'<li><strong>{e(p["name"])}</strong><p>{e(p["brief"])}</p></li>' for p in board['panels'])
    note = f'<p class="note">Review note: {e(board["note"])}</p>' if board.get('note') else ''
    tag = score_tag(audited[board['id']]) if board['id'] in audited else '0% planned work; visual match unscored'
    progress = f'<p><strong>Implementation: {e(tag)}</strong> · <a href="../mockup-visual-audit.md">Panel evidence and remaining work</a></p>'
    return f'''<article data-status="{status}" id="board-{board['id']}">
    <h2>{board['id']}. {e(board['title'])}<span class="badge {status}">{'Generated · review needed' if ready else 'Awaiting generation'}</span></h2>
    {progress}{preview}{note}<details><summary>Screen briefs and behavior scope</summary><ol>{details}</ol></details>
    <p><a href="{board['id']}-prompt.txt">Exact image-generation prompt</a></p></article>'''


ready_count = sum((HERE / b['file']).exists() for b in boards)
cards = ''.join(card(b) for b in boards)
pending_images = [b for b in boards if not (HERE / b['file']).is_file()]
pending_code = [b for b in boards if (HERE / b['file']).is_file() and b.get('implementationStatus') != 'implemented_with_capability_adaptations']
def board_range(items):
    return f"{items[0]['id']}–{items[-1]['id']}" if items else 'none'
next_id = manifest.get('nextBoard')
next_prompt_link = f'[{next_id}-prompt.txt]({next_id}-prompt.txt)' if next_id else 'none; all planned images exist'
generation_note = (
    f"Historical quota observation: image generation reached its usage limit on {manifest.get('lastGenerationDate')}. "
    f"Tool-estimated reset was {manifest['quotaResetLocal']}; quota has not been rechecked during this audit. Resume at board {next_id}."
    if manifest.get('imageGenerationStatus') == 'usage_limit_reached' else
    f"Generation resumed {manifest.get('lastGenerationDate')}; next board {next_id}."
)

prior = '''<article data-status="ready"><h2>Previously reviewed direction</h2>
<p>The owner likes this direction. These are visual references. Provisional panel scores and remaining work are in the <a href="../mockup-visual-audit.md">implementation audit</a>; no panel is certified visually complete.</p>
<div class="prior"><a href="../shinku-phase1-mockup-v1.png"><img src="../shinku-phase1-mockup-v1.png" alt="Library, manga details and More"></a>
<a href="../shinku-inner-experience-mockup-v1.png"><img src="../shinku-inner-experience-mockup-v1.png" alt="Settings, AI setup and reader controls"></a></div></article>'''
gallery = f'''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>ShinKu · UI review gallery</title><style>
:root{{color-scheme:dark;font-family:system-ui,sans-serif;background:#101116;color:#f3f2f7}}
body{{margin:0}}main{{max-width:1400px;margin:auto;padding:28px 22px 70px}}h1{{font-size:32px;margin:0 0 12px}}
p{{line-height:1.6;color:#c0c2d1}}a{{color:#c0c7ff}}nav{{position:sticky;top:0;background:#101116ee;padding:14px 0;display:flex;flex-wrap:wrap;gap:10px;z-index:1}}
button,.navlink{{font:inherit;padding:10px 16px;border-radius:12px;border:1px solid #565b72;background:#252838;color:#eee;text-decoration:none;cursor:pointer}}
button[aria-pressed=true]{{background:#b8c0ff;color:#101116}}article{{border:1px solid #3c4053;border-radius:20px;padding:20px;margin:20px 0;background:#181a24}}
h2{{font-size:23px;margin:0 0 18px}}.badge{{display:inline-block;font-size:12px;margin-left:16px;padding:6px 10px;border-radius:20px;background:#30364c;font-weight:500}}
.pending{{background:#44362c}}img{{width:100%;height:auto;border-radius:12px;display:block}}.placeholder{{padding:70px 18px;text-align:center;border:1px dashed #696477;border-radius:14px;color:#c3bdcd;line-height:1.8}}
.prior{{display:grid;grid-template-columns:1fr 1fr;gap:14px}}details{{margin-top:16px}}summary{{cursor:pointer;color:#d9ddff}}li p{{margin:5px 0 18px}}.note{{border-left:3px solid #d8b477;padding-left:12px}}[hidden]{{display:none!important}}
@media(max-width:700px){{.prior{{grid-template-columns:1fr}}h2{{font-size:19px}}.badge{{margin:8px 0;display:table}}}}
</style><main><h1>ShinKu · UI review gallery</h1>
<p>Sanctuary direction across the app. {ready_count} of {len(boards)} new boards rendered; {(len(boards)-ready_count)*3} new panels await image generation. Six earlier panels are also included.</p>
<p><strong>Rendering is incomplete:</strong> {e(generation_note)} Remaining briefs are saved; placeholders are not finished mockups. Images, implementation and phone installation are tracked separately.</p>
<nav><button data-filter="all" aria-pressed="true">All boards</button><button data-filter="ready" aria-pressed="false">Images ready</button><button data-filter="pending" aria-pressed="false">Pending images</button>
<a class="navlink" href="checklist.md">Mockup checklist</a><a class="navlink" href="../generated-mockups-completion.md">Generated mockup implementation</a><a class="navlink" href="coverage.md">Coverage & implementation rules</a><a class="navlink" href="source-surface-checklist.csv">Source checklist CSV</a></nav>
{prior}{cards}<p>Generated with the built-in image tool. Content and metrics are illustrative. Reuse existing app behavior and tokens; validate actual layouts, accessibility, permissions, extension interfaces and data safety before shipping.</p></main>
<script>document.querySelectorAll('button[data-filter]').forEach(b=>b.addEventListener('click',()=>{{document.querySelectorAll('button[data-filter]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));document.querySelectorAll('article[data-status]').forEach(x=>x.hidden=b.dataset.filter!=='all'&&x.dataset.status!==b.dataset.filter)}}));</script></html>'''
(HERE / 'index.html').write_text(gallery, encoding='utf-8')

coverage = [
    '# ShinKu UI mockup coverage and implementation checklist', '',
    f'Inventory date: 2026-11-03. {len(boards)} new boards / {sum(len(b["panels"]) for b in boards)} planned panels, plus the six earlier panels.', '',
    f'**Incomplete rendering:** {ready_count} new boards are generated; {len(boards)-ready_count} still await generation. '
    f'{generation_note} '
    'See the [mockup checklist](./checklist.md) for separate image, implementation and phone-installation status.', '',
    'Open [the review gallery](./index.html), [the manifest](./manifest.json), or [the source checklist](./source-surface-checklist.csv).', '',
    '## Board coverage', '', '| Board | Screens / states | Image status |', '| --- | --- | --- |',
]
for b in boards:
    ready = (HERE / b['file']).exists()
    title = f'[{b["id"]}. {b["title"]}](./{b["file"]})' if ready else f'{b["id"]}. {b["title"]}'
    coverage.append(f'| {title} | {"; ".join(p["name"] for p in b["panels"])} | {"Generated; review required" if ready else "Prompt ready; not generated"} |')
coverage += [
    '', '## What the source inventory means', '',
    f'The CSV contains {len(rows)} named Kotlin surface entries (file entry points and named Screen/Dialog/Sheet/Step functions). '
    'UI wrappers and presentation functions can appear separately. This is a static inventory, not a count of unique user-visible screens or a runtime navigation audit.', '',
    'Each entry maps to a representative board or shared template. A mapping is not proof that every inherited preference, inline modal, or state is rendered. '
    'Rows marked shared/template/manual review must be inspected before implementation is considered covered. Anonymous dialogs, dynamic extension settings, and system/browser UI require runtime review.', '',
    '## Shared component contract', '',
    '- Reuse existing Sanctuary colors, typography, shapes, spacing and shared components; generated pixels are not authoritative RGB or dp specifications.',
    '- Use one consistent app bar, navigation dock/rail, preference row, section card, search field, manga card, selection bar, dialog and bottom-sheet system.',
    '- Preserve the extension ABI, backup identifiers, deep links, existing preference keys, navigation destinations, source controls and suggestion counts.',
    '- Treat sample names, counts, source labels and status badges as illustrative. Never hard-code generated text as live data or claim checks that were not performed.',
    '- Make states explicit: default, pressed/selected, disabled, focused, loading, partial success, empty, no results, offline, authentication required, failure, retry and completion.',
    '- Keep destructive operations scoped and explicit. Distinguish removing library entries from deleting downloads. Preserve supported cancellation boundaries.',
    '- Reserve layout insets for navigation, gesture/three-button bars, keyboards, bottom actions and reader controls. Verify the final item and its actions can be reached.',
    '- Keep existing features accessible when grouping settings. Search and result highlighting must traverse all subpages. Advanced integration/debug preferences must remain available.',
    '', '## One coordinated implementation pass', '',
    '1. Review all board directions and resolve the generated-content notes below before changing behavior.',
    '2. Establish shared component variants and tokens first; migrate each family using the same primitives rather than styling each page independently.',
    '3. Work through the source CSV, filling implementation and device-validation columns. Inspect every inherited preference and every template-mapped surface.',
    '4. Verify end-to-end paths: onboarding/restore → sources → search → library → reader → progress → download/update → backup/sync/tracking.',
    '5. Validate phone/tablet/landscape, long translated strings, RTL, large text/display scaling, TalkBack/focus, reduced motion, light/dark/high-contrast and E-Ink/low-power profiles.',
    '6. Verify privacy/lock screens, Android permission denial, website authentication, extension trust, migration/cancellation, storage failures, missing covers and corrupt backup/error recovery.',
    '7. Run appropriate dev-flavor checks and the project release protocol only when an actual release is authorized. A gallery or mapped CSV is not a completion certificate.',
    '', 'A single coordinated UI update is the goal; images cannot guarantee a defect-free single coding attempt. Runtime, accessibility and data-safety checks remain required.',
    '', '## Review notes on generated images', '',
]
for b in boards:
    if b.get('note'):
        coverage.append(f'- Board {b["id"]}: {b["note"]}')
coverage += [
    '', '## Resume generation', '',
    'After the tool quota resets, generate only missing PNGs from their numbered prompt files with the two existing reference boards. '
    'Keep filenames/versioning stable; do not overwrite reviewed images without requesting an iteration. Use built-in image generation unless the owner explicitly chooses a different provider/path.', '',
    'Set each board status in manifest.json after inspecting its output, then run `python docs/design/mockups/build_review.py` to refresh the gallery and CSV. '
    'An alternate API-key or credit-based image service requires the owner’s explicit choice; no fallback service has been invoked.', '',
]
(HERE / 'coverage.md').write_text('\n'.join(coverage), encoding='utf-8')
print(f'Gallery built: {ready_count}/{len(boards)} new boards, {len(rows)} source-surface entries.')

# This is board-level progress, not a claim of exhaustive runtime coverage.
coded_boards = [b for b in boards if b['id'] in audited]
validation = manifest.get('validation', {})
checklist = [
    '# ShinKu mockup checklist', '',
    f'Updated: {manifest.get("lastValidationDate", manifest.get("lastImplementationDate", manifest.get("lastGenerationDate", "unknown")))}.', '',
    'Generated = image exists. Code status records implementation work, including retained existing UI. '
    'Visual completion requires comparison of the actual app with the approved image. Tests/builds do not establish visual fidelity. '
    'Latest physical-phone artifact verification is separate.', '',
    f'- [x] {ready_count}/24 numbered boards generated ({ready_count*3}/72 panels).',
    f'- [x] Source-informed percentage audit recorded for {len(coded_boards)} generated boards / 39 panels plus six earlier concepts. Visual acceptance remains below 100%.',
    f'- [x] {validation.get("jvmPassed", "unrecorded")} JVM tests and {validation.get("androidPassed", "unrecorded")} Android emulator tests passed; all five optimized APK signatures verified.',
    '- [ ] Verify the newest artifact on the physical phone; transfers via LocalSend have not been independently verified.',
    '- [ ] Finish the documented visual gaps and obtain complete runtime comparisons for every panel.',
    f'- [ ] Generate remaining boards {board_range(pending_images)}.', '',
    generation_note, '',
    'Percentages are provisional supported-scope estimates, weighted 70% toward layout/styling. '
    'They are not pixel measurements or test coverage. See the [full scoring/evidence report](../mockup-visual-audit.md).', '',
    '| Board | Mockup | Image | Implementation estimate | Evidence / visual status | Phone verification |',
    '| --- | --- | --- | --- | --- | --- |',
]
for board in boards:
    exists = (HERE / board['file']).is_file()
    coded = board['id'] in audited
    phone_status = board.get('phoneInstallationStatus')
    partial = board.get('implementationStatus') == 'partially_implemented'
    phone = 'Latest artifact unverified' if coded or partial else 'Not applicable'
    code_status = score_tag(audited[board['id']]) if coded else '0% planned work; visual match unscored'
    visual = 'Default and actual 1.6× text reviewed; final phone/theme acceptance pending' if board['id'] == '05' and audited['05'].get('parts') else 'Source reviewed; partial captures where available; gaps remain' if coded else 'Draft only; not adopted' if board['id'] == '14' else 'No image target yet'
    image_status = 'Generated' if exists else 'External draft only' if board['id'] == '14' else 'Prompt ready'
    checklist.append(f'| {board["id"]} | {board["title"]} | {image_status} | {code_status} | {visual} | {phone} |')

checklist += [
    '', '*Functional scope previously counted existing UI retained alongside new code changes. '
    'It must not be read as completed visual implementation. See the [visual-audit correction](../mockup-visual-audit.md) and [earlier implementation review](../generated-mockups-completion.md).', '',
    '## Earlier concept boards (separate from 01–24)', '',
    '| Concept panel | Image | Implementation estimate | Visual status |', '| --- | --- | --- | --- |',
    *[f"| {p['name']} | Generated | {p['percent']}% partial (provisional) | {evidence_label(p)}; gaps remain |" for p in audit['earlierConcepts']], '',
    '## Current build and next batch', '',
    'Latest verified local APKs remain version 2.8.1-preview.1 / 143. '
    f'Use {validation.get("artifactRecord", "build/generated-mockups-checksums.json")} to distinguish this build from earlier previews. '
    'Phone installation is tracked separately above.', '',
    f'Generated boards below 100% visual acceptance: {board_range(pending_code)}. '
    'Future boards use built-in image_gen with the two earlier concept boards as style references. '
    'Exact input is the numbered prompt file from Use case onward; only the historical blocked-status header is omitted. '
    'Image review notes are in [manifest.json](manifest.json) and the [gallery](index.html). '
    f'Next generation starts at {next_prompt_link}.', '',
]
(HERE / 'checklist.md').write_text('\n'.join(checklist), encoding='utf-8')
