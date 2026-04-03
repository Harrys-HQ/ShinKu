# Specification: Anime Support Integration

## Goal
Transform ShinKu from a dedicated manga reader into a hybrid "ShinKu Media Hub" that supports both Manga and Anime through a modular, extension-based system.

## Architectural Changes

### 1. Model Expansion (`domain` & `source-api`)
- **SAnime:** Similar to `SManga`, containing titles, descriptions, status, and genres.
- **SEpisode:** Replacing `SChapter` for Anime, including episode numbers and titles.
- **Video:** A new model representing a stream URL, quality (1080p, 720p, etc.), and subtitle tracks.

### 2. Extension API (`source-api`)
- **AnimeSource:** A new interface for extensions to fetch anime lists, metadata, episodes, and video streams.
- **Video Extractors:** A library of modular extractors (e.g., GogoPlay, MyCloud) to resolve direct video URLs from host sites.

### 3. Media Player (`app`)
- **Engine:** Integrate **mpv-android** (preferred for its codec support and subtitle handling) or **ExoPlayer/Media3** (native Android).
- **UI:** A dedicated full-screen video player with controls for:
  - Playback speed.
  - Subtitle selection.
  - Quality switching.
  - Gesture controls (brightness, volume, seeking).
  - Picture-in-Picture (PiP) support.

### 4. Database Schema (`data`)
- New tables for `anime`, `episodes`, `anime_categories`, and `anime_history`.
- Migration logic to handle version jumps for the database.

### 5. UI/UX Evolution
- **Unified Library:** A toggle or tab system to switch between Manga and Anime libraries.
- **Media Info Screen:** Adapt the existing Manga Info screen to display Episode lists with playback progress indicators.
- **Search:** Global search results split into Manga and Anime categories.

## Technical Impacts
- **APK Size:** Expect an increase of ~5-10MB due to video player libraries.
- **Complexity:** Maintenance increases significantly due to video host protections (DMCA, captchas, obfuscated URLs).
- **Synchronization:** Support for Anime trackers (AniList, MyAnimeList) must be expanded to handle episode-level tracking.
