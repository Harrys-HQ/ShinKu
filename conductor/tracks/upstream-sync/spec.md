# Track Specification: Upstream Synchronization & Core Evolution

## Objective
Keep ShinKu updated with core fixes and features from Mihon (upstream) while evolving the project into an independent and robust platform.

## Requirements
1.  Identify critical fixes in Mihon that are missing in SY.
2.  Maintain build stability during merges.
3.  **Modernize Storage:** Replace restricted sync services (Google Drive) with accessible alternatives (Dropbox).
4.  **Fix Core Regressions:** Address issues in library management and notification logic inherited or introduced during rebranding.

## Scope
- Merge/Cherry-pick from `mihon/main`.
- Resolve conflicts between Mihon core and SY features.
- Build verification after each major sync.
- Implementation of independent features (Dropbox Sync, AI Vibe Search).
