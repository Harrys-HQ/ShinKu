Looking to report an issue/bug or make a feature request? Please refer to the [README file](/README.md#issues-feature-requests-and-contributing).

---

Thanks for your interest in contributing to Tachiyomi!


# Code contributions

Pull requests are welcome!

### Build Mandates
ShinKu uses the `devRelease` variant for official previews and development. The legacy `standard` flavor is deprecated.
- **Mandatory Build Check:** All PRs must build successfully with `./gradlew assembleDevRelease`.
- **Primary Flavor:** Always use the `dev` flavor for new features.

## Extension Compatibility (The "Freeze Zone")

ShinKu maintains compatibility with external APK extensions. This requires certain package names and class structures to remain exactly as they were in Tachiyomi/Mihon.

**Do not rename or move files in the following packages:**
- `eu.kanade.tachiyomi.source.*`
- `eu.kanade.tachiyomi.network.*`

For more on this, see [docs/extension-safeguards.md](/docs/extension-safeguards.md).