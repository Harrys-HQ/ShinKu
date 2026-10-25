# ShinKu Development Protocols

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

