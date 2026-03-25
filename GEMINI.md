# ShinKu Project Mandates

## Build Mandates
- **MANDATE:** All build verifications and final development checks MUST now use the **'devRelease' (Preview)** variant (e.g., `./gradlew assembleDevRelease`). This ensures that the codebase is always ready for the official Preview release channel.
- **MANDATE:** The 'standard' build flavor remains discontinued due to R8-related instability. Use the 'dev' flavor instead.
- **MANDATE:** For active development and debugging, the **'devDebug'** variant (e.g., `./gradlew assembleDevDebug`) remains the standard to allow for faster builds and full debugging symbols.
- **MANDATE:** A successful `./gradlew assembleDevRelease` build is required before proceeding to any new development phase or completing a track.
