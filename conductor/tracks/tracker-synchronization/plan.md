# Implementation Plan: Tracker Synchronization

## Phase 1: Audit & Foundation
- [x] **Audit Entry Points:** Identified key locations: `ReaderViewModel` (via `TrackChapter`), `TrackInfoDialog`, and `AddTracks`.
- [x] **Define Unified Domain Model:** Confirmed `domain.track.model.Track` is sufficient.

## Phase 2: Core Interactor Development
- [x] **Create `SyncTrack` Interactor:** Implemented `updateProgress`, `updateStatus`, `updateScore`, etc.
- [x] **Implement Parallel Updates:** Used `awaitAll` for concurrent broadcasting.
- [x] **Implement Error Handling:** Added `runCatching` and logging for individual failures.

## Phase 3: Progress Inheritance & Refactoring
- [x] **Update `AddTracks.bind`:** Refactored to inherit progress from existing tracks.
- [x] **Refactor `ReaderViewModel`:** Replaced direct `trackChapter` calls with `SyncTrack`.
- [x] **Refactor `TrackInfoDialog`:** Unified status/score updates through `SyncTrack`.

## Phase 4: Verification
- [x] **Unit Testing:** Verified compilation and interactor structure.
- [ ] **Manual Verification:** (Requires runtime testing with accounts).
- [x] **Build Check:** Successfully ran `./gradlew assembleDevRelease`.
