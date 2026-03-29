# Implementation Plan: Fluidity & Refinement 2026

## Overview
This track aims to bridge the gap between "feature-complete" and "project-of-excellence." It prioritizes architectural modernization alongside high-impact, low-effort UI enhancements and AI intelligence features.

## Baseline (Latest Safe Backup)
- **Commit:** `9f57f040a30c9dd3aa0d8977172d23a3ef0a01c2`
- **Revert:** `git checkout 9f57f040a` (Safe post-Enhancements 2026 state)

## Phases

### Phase 1: Architectural Modernization (DI Pilot)
The current DI system (`Injekt`) is becoming legacy and makes testing/Compose integration harder. We will start a gradual migration.

- [x] **Koin Integration:** Added Koin to the project alongside Injekt.
- [x] **Pilot Migration:** Migrated `StatsScreenModel` to Koin using `voyager-koin` and constructor injection.
- [x] **DI Bridge:** Established `InjektKoinBridge` to allow interop during migration.

### Phase 2: Immersive UI & Aesthetics (Mood & Haptics)
Building on the dynamic theme system to make the app feel truly alive.

- [x] **Download Migration Progress:** Added progress and completion notifications for the legacy download migration utility.
- [x] **Mood Lighting:** Implement a reader feature that subtly adjusts screen brightness or color temperature based on manga genres (e.g., warmer for Romance, cooler for Horror).
- [x] **Dynamic Backdrop Blurs:** Add blurred Cover backgrounds to the Library and Manga Info screens to create depth.
- [x] **Haptic Profiles:** Integrate `HapticGenerator` for subtle tactile feedback on page turns, cover long-presses, and milestone alerts.

## Verification & Stability
Every task must be followed by:
1.  **Successful `assembleDevRelease` build.**
2.  **R8 Analysis:** Verify no R8/ProGuard regressions, particularly for the new AI/Sync logic.
3.  **UI Verification:** Confirm no flickering or performance drops from new blur effects.
