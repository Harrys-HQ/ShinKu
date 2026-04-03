# Implementation Plan: Anime Support Integration

## Overview
This plan describes the phased rollout of Anime support, prioritizing architectural stability before UI implementation.

## Phases

### Phase 1: Core API & Data Models (`source-api` & `domain`)
Define the fundamental interfaces and models required for Anime.
- [ ] Create `SAnime`, `SEpisode`, and `Video` interfaces in `source-api`.
- [ ] Implement `AnimeSource` and `CatalogueAnimeSource` interfaces.
- [ ] Implement `AnimeSourceFactory` for extension loading.

### Phase 2: Database & Domain Logic (`data` & `domain`)
Extend the persistence and business logic layers.
- [ ] Add `anime.sq` and `episodes.sq` to `data/src/main/sqldelight`.
- [ ] Implement `AnimeRepository` and `EpisodeRepository`.
- [ ] Extend trackers (AniList, MAL) to support Anime entry types and episode progress.

### Phase 3: Video Engine & Player Implementation (`app`)
Integrate the media playback system.
- [ ] Add `mpv-android` or `Media3` dependencies to `app/build.gradle.kts`.
- [ ] Create `VideoPlayerActivity` with basic playback controls.
- [ ] Implement `VideoExtractor` logic to handle stream resolution from extensions.
- [ ] Add support for "External Player" in settings.

### Phase 4: UI/UX & Unified Experience (`app` & `presentation`)
Bring the feature to the user.
- [ ] Implement `AnimeLibraryTab` and `AnimeInfoScreen`.
- [ ] Add "Media Toggle" (Manga/Anime) in the Library and Browse tabs.
- [ ] Update Global Search to include Anime results.
- [ ] Add Anime-specific settings (auto-skip intro, preferred quality, etc.).

## Verification & Stability
1. **API Compatibility:** Ensure that existing Manga extensions continue to work without modification.
2. **Build Success:** Maintain a successful `assembleDevRelease` build throughout all phases.
3. **Resource Check:** Monitor APK size and memory usage during video playback.
