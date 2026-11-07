# ShinKu Development Protocols

## Project Memory
- Read `PROJECT_MEMORY.md` at the start of work on this repository. It contains the project map, compatibility constraints, and review context.
- Consult `docs/project-review.md` for the prioritized optimization and UI/UX backlog. Findings are observations at the recorded commit, not proof that later code still has the same behavior.
- Start with `git status --short` and changes since the memory's reviewed commit; inspect the files relevant to the task instead of repeating a whole-project audit.
- Update memory when architecture, workflows, or confirmed findings materially change. Keep credentials and personal reading data out of memory. A review recommendation does not authorize its implementation or a release.

## 1. Build Protocol
- **Fast & Secure Assembly**: Run `.\gradlew.bat assembleDevRelease --build-cache --parallel --quiet --console=plain`.
- **Active Iteration**: Use fast incremental builds (`./gradlew compileDevDebugKotlin` or `./gradlew assembleDevDebug`).
- **Flavor Restrictions**: Use the `dev` flavor only (`devRelease`).
- **Security & Signing**: Keystore properties read strictly from untracked `local.properties` or environment variables; never commit credentials. R8 optimization enabled on release-tier variants.
- **Build Failure Recovery**: Read full log, fix errors modularly, verify constructors before updating DI, and avoid cyclic fix attempts.

## 2. Architecture & Compatibility
- **Package Stability**: Keep `source-api` and `network` in `eu.kanade.tachiyomi` for extension API binary compatibility.
- **Backups**: Preserve legacy `@SerialName("eu.kanade.tachiyomi...")` annotations on backup models.
- **Deep Links**: Retain `tachiyomi://` and `mihon://` intent filters in `AndroidManifest.xml`.
- **UI & Grouping**: Group new settings under "ShinKu"; reuse components from `eu.kanade.tachiyomi.presentation.core` and `com.shinku.reader.presentation`.

## 3. Database & Versioning
- **DB Migrations**: Number `.sq` and `.sqm` migration files sequentially.
- **Atomic Versioning**: Synchronize version bumps across `app/build.gradle.kts`, `docs/version.json`, in-app notice (`app/src/main/res/raw/changelog_release.xml`), and website changelog (`docs/CHANGELOG.md`).
- **Complete Release & Push Mandate**: Never close an issue or declare a release done without executing the full bundle pipeline:
  1. Synchronize version numbers and changelogs across app, docs, and in-app notices.
  2. Assemble verified production APKs (`assembleDevRelease`).
  3. Push commits to `origin main` to trigger automated GitHub Pages website deployment.
  4. Publish GitHub release with all 5 APK architecture binaries attached (`gh release create`).
  5. Close GitHub issues with comments linking to the published release tag.

