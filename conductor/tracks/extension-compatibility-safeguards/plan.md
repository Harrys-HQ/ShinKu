# Implementation Plan: Extension Compatibility & Architectural Safeguards

## Phase 1: Documentation & Source Annotation
- [ ] **Annotate Frozen Files:** Add "DANGER: DO NOT RENAME" headers to `SManga`, `SChapter`, `Source`, and `NetworkHelper`.
- [ ] **Create Freeze Zone Doc:** Create `docs/extension-safeguards.md` listing all protected paths.
- [ ] **Update CONTRIBUTING.md:** Add a section about extension compatibility and package stability.

## Phase 2: Automated Safeguards
- [ ] **Package Integrity Test:** Create a unit test in `app` module that uses reflection to verify the existence and location of critical classes (e.g., `Class.forName("eu.kanade.tachiyomi.source.model.SChapter")`).
- [ ] **Lint Rule (Optional):** Research a custom ArchUnit or Lint rule to prevent package moves for the frozen zones.

## Phase 3: Redirect & Intent Stability
- [ ] **Intent Filter Audit:** Verify all intent filters in `AndroidManifest.xml` against the expected `tachiyomi://` and `mihon://` schemes.
- [ ] **Document Deep Links:** Add a table of supported deep links to `docs/extension-safeguards.md`.

## Phase 4: Verification
- [ ] **Run Safeguard Tests:** `./gradlew test`.
- [ ] **Build Check:** `./gradlew assembleDevRelease`.
