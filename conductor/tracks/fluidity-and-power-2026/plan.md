# Implementation Plan: Fluidity & Power 2026

## Phases

### Phase 1: Visual Immersion (The "Komikku" Layer)
- [x] **Task 1.1: Per-Manga Dynamic Theming**
  - [x] Research: Identify color extraction points and theme application targets.
  - [x] Implementation: Add Palette extraction to cover loading.
  - [x] UI: Apply extracted colors to `ReaderActivity` and `MangaDetailsScreen`.
- [x] **Task 1.2: Smart Webtoon Detection**
  - [x] Implementation: Logic to sample page dimensions in `ReaderViewModel`.
  - [x] Integration: Auto-toggle `ReadingMode.WEBTOON`.
- [x] **Task 1.3: Gesture Preview Activity**
  - [x] UI: Create a mock reader screen with interactive overlays.

### Phase 2: Extreme Performance (The "AniZen" Layer)
- [x] **Task 2.2: Source Health Dashboard**
  - [x] Backend: `SourceHealthWorker` (implemented as `RepoHealthScanJob`).
  - [x] UI: Status indicators (colored dots) in `SourcesScreen`.

### Phase 3: On-Device Intelligence
- [x] **Task 3.1: Offline Embedding Engine**
  - [x] Backend: `MangaEmbeddingJob` using MediaPipe Text Embedder.
  - [x] Database: `manga_embeddings` table for local vector storage.
  - [x] UX: Progress notifications for library analysis.
- [x] **Task 3.2: AI Clustering for Categories**
  - [x] Backend: `AiClusteringJob` with K-Means logic.
  - [x] UI: Manual trigger in ShinKu Settings.
  - [x] UI: "Similar Vibes" carousel on Manga Details screen.

### Phase 4: Core Infrastructure Rewrite
- [x] **Task 4.1: Multi-threaded Download Engine (Moved from Phase 2)**
  - [x] Architecture: Refactor `Downloader` to use chunked coroutine workers.
  - [x] Database: Add `download_chunk_progress` table for granular chunk tracking.
  - [x] Logic: Implemented byte-range resume support.
  - [x] UI: Added "Download threads per page" slider in Settings.

## Verification
- Build: `./gradlew assembleDevRelease` (Verified - APK Generated 2026-04-20 11:11 AM)
- Stability: Resolved `SecurityException` in notification channels.
- Maintenance: Fixed compiler warnings in `MangaScreen.kt`.
- Performance: Verified resumable parallel downloads with granular chunk tracking.
