# ShinKu Development Protocols

## 1. Build Protocol
- **Active Iteration**: Use fast incremental builds (`./gradlew compileDevDebugKotlin` or `./gradlew assembleDevDebug`).
- **Feature Completion / Release**: Run full `./gradlew assembleDevRelease`.
- **Flavor Restrictions**: Use the `dev` flavor only (`standard` flavor is discontinued).
- **Build Failure Recovery**: Read full log, fix errors modularly, verify constructors before updating DI, and avoid cyclic fix attempts.

## 2. Architecture & Compatibility
- **Package Stability**: Keep `source-api` and `network` in `eu.kanade.tachiyomi` for extension API binary compatibility.
- **Backups**: Preserve legacy `@SerialName("eu.kanade.tachiyomi...")` annotations on backup models.
- **Deep Links**: Retain `tachiyomi://` and `mihon://` intent filters in `AndroidManifest.xml`.
- **UI & Grouping**: Group new settings under "ShinKu"; reuse components from `eu.kanade.tachiyomi.presentation.core` and `com.shinku.reader.presentation`.

## 3. Database & Versioning
- **DB Migrations**: Number `.sq` and `.sqm` migration files sequentially.
- **Atomic Versioning**: Synchronize version bumps across `build.gradle.kts`, `version.json`, `CHANGELOG.md`, and `README.md` prior to release tags.

