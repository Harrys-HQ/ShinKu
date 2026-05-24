# Specification: Tracker Synchronization

## Problem
Currently, the application manages tracking services (AniList, MAL, Bangumi, etc.) as independent entities. Logic for updating chapters, scores, and statuses is often triggered for one service at a time or handled inconsistently across different ViewModels and Dialogs. This leads to:
1. **Sync Drift:** One service is updated while another falls behind.
2. **Logic Duplication:** Similar "check and update" logic is repeated in `TrackChapter`, `BaseTracker`, and `AddTracks`.
3. **Friction for Multi-Tracking:** Users linking multiple services must manually ensure they are in sync.

## Goals
1. **Unified Interactor:** Create a central `SyncTrack` interactor to handle all tracking state changes.
2. **Parallel Broadcasting:** Every tracking update (progress, status, score) is broadcast to all logged-in services for that manga.
3. **Progress Inheritance:** New tracking links automatically inherit metadata and progress from existing links.
4. **Architectural Purity:** Move business logic out of `BaseTracker` and into the domain layer.

## Requirements
- Maintain support for all existing services (AniList, MAL, Kitsu, etc.).
- Ensure `DelayedTrackingStore` (offline sync) works for all services.
- The UI should not block while broadcasting to multiple services; use non-blocking coroutines.
- Preserve service-specific features (like private tracking on AniList) through the unified interactor.
