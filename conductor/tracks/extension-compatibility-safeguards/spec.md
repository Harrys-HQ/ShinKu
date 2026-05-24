# Specification: Extension Compatibility & Architectural Safeguards

## Problem
ShinKu relies on a shared interface with Tachiyomi/Mihon extensions. Renaming certain backend packages (e.g., `eu.kanade.tachiyomi.source`) or moving critical models (e.g., `SChapter`, `SManga`) will break existing extension compatibility. Additionally, app-wide deep links (like `tachiyomi://add-repo`) must remain consistent.

## Goals
1. **Define the "Freeze Zone":** Document all packages and classes that are strictly "Do Not Touch" regarding naming and structure.
2. **Automated Verification:** Implement unit tests or lint rules that fail if critical package paths are modified.
3. **Deep Link Stability:** Ensure `MainActivity` and `DeepLinkActivity` intent filters are protected and documented.
4. **Developer Guidance:** Update `CONTRIBUTING.md` to explain *why* these packages are frozen.

## Requirements
- No changes to `eu.kanade.tachiyomi.source.*` or `eu.kanade.tachiyomi.network.*`.
- Explicit warnings in the source code for frozen files.
- Documentation mapping the "Bridge" between ShinKu's internal domain and the legacy extension interfaces.
