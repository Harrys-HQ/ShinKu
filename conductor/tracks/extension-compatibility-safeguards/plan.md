# Implementation Plan: Extension Compatibility & Architectural Safeguards

## Phase 1: Documentation & Source Annotation
- [x] **Annotate Frozen Files:** Add "DANGER: DO NOT RENAME" headers to `SManga`, `SChapter`, `Source`, and `NetworkHelper`.
- [x] **Create Freeze Zone Doc:** Create `docs/extension-safeguards.md` listing all protected paths.
- [x] **Update CONTRIBUTING.md:** Add a section about extension compatibility and package stability.

## Phase 2: Automated Safeguards
- [x] **Package Integrity Test:** Create a unit test in `app` module that uses reflection to verify the existence and location of critical classes (e.g., `Class.forName("eu.kanade.tachiyomi.source.model.SChapter")`).
- [x] **Lint Rule (Optional):** Implemented an architectural package consistency scanner in `ExtensionCompatibilityTest` that recursively verifies that no Kotlin/Java source files declare package mismatches or are moved out of the frozen zone.

## Phase 3: Redirect & Intent Stability
- [x] **Intent Filter Audit:** Verify all intent filters in `AndroidManifest.xml` against the expected `tachiyomi://` and `mihon://` schemes.
- [x] **Document Deep Links:** Add a table of supported deep links to `docs/extension-safeguards.md`.

## Phase 4: Verification
- [x] **Run Safeguard Tests:** `./gradlew test`.
- [x] **Build Check:** `./gradlew assembleDevRelease`.
