# Implementation Plan: Fluidity & Refinement 2026

## Overview
This track aims to bridge the gap between "feature-complete" and "project-of-excellence." It prioritizes architectural modernization alongside high-impact, low-effort UI enhancements and AI intelligence features.

## Baseline (Latest Safe Backup)
- **Commit:** `3fb5192f67de28ddafc28642c4814eb70848e524`
- **Revert:** `git checkout 3fb5192f6` (Safe post-Phase-4 state)

## Phases

### Phase 1: Architectural Modernization (DI Pilot)
The current DI system (`Injekt`) is becoming legacy and makes testing/Compose integration harder. We will start a gradual migration.

- [ ] **Koin Integration:** Add Koin to the project alongside Injekt.
- [ ] **Pilot Migration:** Migrate one non-critical domain or presentation module (e.g., `History` or `Stats`) to Koin to verify the pattern.
- [ ] **DI Bridge:** Ensure Injekt and Koin can interoperate during the migration period.

### Phase 2: Immersive UI & Aesthetics (Mood & Haptics)
Building on the dynamic theme system to make the app feel truly alive.

- [x] **Download Migration Progress:** Added progress and completion notifications for the legacy download migration utility.
- [ ] **Mood Lighting:** Implement a reader feature that subtly adjusts screen brightness or color temperature based on manga genres (e.g., warmer for Romance, cooler for Horror).
- [ ] **Dynamic Backdrop Blurs:** Add blurred Cover backgrounds to the Library and Manga Info screens to create depth.
- [ ] **Haptic Profiles:** Integrate `HapticGenerator` for subtle tactile feedback on page turns, cover long-presses, and milestone alerts.

## Verification & Stability
Every task must be followed by:
1.  **Successful `assembleDevRelease` build.**
2.  **R8 Analysis:** Verify no R8/ProGuard regressions, particularly for the new AI/Sync logic.
3.  **UI Verification:** Confirm no flickering or performance drops from new blur effects.
