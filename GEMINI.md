# ShinKu Project Mandates

## Build Mandates
- **MANDATE:** All build verifications and final development checks MUST now use the **'devRelease' (Preview)** variant (e.g., `./gradlew assembleDevRelease`). This ensures that the codebase is always ready for the official Preview release channel.
- **MANDATE:** The 'standard' build flavor remains discontinued due to R8-related instability. Use the 'dev' flavor instead.
- **MANDATE:** For active development and debugging, the **'devRelease'** variant (e.g., `./gradlew assembleDevRelease`) must be used instead of 'devDebug'.
- **MANDATE:** A successful `./gradlew assembleDevRelease` build is required before proceeding to any new development phase or completing a track.

## Release Mandates
- **MANDATE:** Every GitHub release MUST include only the changelog notes specific to that version (do not include the full historical changelog).
- **MANDATE:** Every GitHub release MUST include the compiled APK files (e.g., from the `devRelease` build) as release assets.
