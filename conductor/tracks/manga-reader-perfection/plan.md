# Track: Manga Reader Perfection & Polish 2026

**Track ID:** `manga-reader-perfection`

## Description
This track covers the phase-by-phase implementation of top-of-the-line features to elevate the ShinKu manga reader to absolute perfection. 

## Roadmap Phases

### Phase 1: Immersive Reading Experience
- [x] **Ambient Audio Integration**: Add actual low-overhead looping background audio assets for genres (Horror, Slice of Life/Romance, Action/Adventure/Fantasy) to `res/raw`. Refine `AtmosphericAudioManager` to support smooth fading transitions between chapters/volumes.
- [x] **Adaptive Palette Transitions**: Improve the adaptiveness of details and reader screen by using palette extraction with hardware-accelerated crossfades.
- [x] **Ambient Light Adaptive Night-Read**: Automatically optimize screen contrast and warm-light filters in the reader when ambient brightness drops, using the device's light sensor.

### Phase 2: On-Device AI Intelligence & Enrichment
- [ ] **Lightweight AI Super-Resolution**: Research/integrate a TFLite model or Glide/Coil interceptor to upscale low-res manga scans on the fly.
- [ ] **"Story So Far" AI Recaps**: Integrate local/API-based automated contextual recaps of previous chapters when resuming an inactive manga.
- [ ] **Deep Vibe Natural Language Search**: Leverage our MediaPipe embeds to allow semantic description searches in global search.

### Phase 3: Deep Reading Analytics & Gamification
- [ ] **"ShinKu Wrapped" Dashboard**: A stunning, full-featured statistics screen showcasing genre heatmaps, page-velocity graphs, and reading time-patterns.
- [ ] **Reader Badges & Achievements**: Implement gamified reader milestones.

### Phase 4: Extreme Performance & Adaptability
- [ ] **E-Ink Hardware Optimization Profile**: Auto-detect E-ink devices and optimize UI contrast, disable transition animations, and map physical hardware volume keys for page turns.
