# ShinKu Project Mandates

## Build Mandates
- **Iteration**: Use fast incremental builds (`./gradlew compileDevDebugKotlin` or `./gradlew assembleDevDebug`).
- **Flavor**: Use the `dev` flavor (`standard` flavor is discontinued).
- **Completion Verification**: Run full `./gradlew assembleDevRelease` before completing feature tracks or publishing releases.

## Release Mandates
- **Changelog**: Include only version-specific notes in release tags/releases.
- **Assets**: Attach compiled `devRelease` APK assets to GitHub releases.
- **In-App Notice**: Update the "What's new" section for every version release.
