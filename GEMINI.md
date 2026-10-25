# ShinKu Project Mandates

## Build Mandates
- **Iteration**: Use fast incremental builds (`./gradlew compileDevDebugKotlin` or `./gradlew assembleDevDebug`).
- **Flavor**: Use the `dev` flavor (`standard` flavor is discontinued).
- **Completion Verification**: Run full `./gradlew assembleDevRelease` before completing feature tracks or publishing releases.

## Release Mandates
- **End-to-End Release Bundle Mandate**: A release is never considered complete until the full bundle pipeline is verified:
  1. **Atomic Version Sync**: Bump `app/build.gradle.kts` (`versionCode` + `versionName`), `docs/version.json`, in-app notice (`app/src/main/res/raw/changelog_release.xml`), and website changelog (`docs/CHANGELOG.md`).
  2. **Production Assembly**: Build and verify full `devRelease` variant (`.\gradlew.bat assembleDevRelease`).
  3. **Repository Push**: Push commits to `origin main` to trigger GitHub Pages documentation deployment.
  4. **GitHub Release Publication**: Publish the release via `gh release create <tag> app/build/outputs/apk/dev/release/*.apk --title "ShinKu <version> (Crimson)" --notes-file ...` with all 5 APK binaries attached.
  5. **Issue Resolution**: Close any associated GitHub issues only after or alongside referencing the live release tag.
- **Changelog**: Include only version-specific notes in release tags/releases.
- **Assets**: Attach compiled `devRelease` APK assets to GitHub releases.
- **In-App Notice**: Update the "What's new" section for every version release.
