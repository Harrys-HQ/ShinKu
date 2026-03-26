# Implementation Plan: ShinKu Enhancements 2026

## Overview
This plan prioritizes features based on their impact on the core value proposition of ShinKu: "immersive, intelligent, and highly personalized."

## Phases

### Phase 1: Reading Journey (Depth & Insights)
The Reading Journey Card is a unique selling point of ShinKu. Enhancing its depth will immediately improve user engagement.

- [x]  **Deep Stats Migration:** Add support for tracking read count and duration per genre and author in the database.
- [x]  **Heatmap Visuals:** Implement a simple genre/frequency heatmap in the statistics screen.
- [x]  **Time Analysis:** Track and display reading "Time Patterns" (e.g., Identifying the user as a "Morning Reader").
- [x]  **Milestone Engine:** Build a system to trigger and display badges (achievements) based on statistics.
- [x]  **ShinKu Wrapped:** Added a text-based shareable summary of reading statistics.

### Phase 2: AI-Powered Discovery & Enrichment
Enhancing the existing Gemini integration to make the app feel "smarter."

- [ ]  **"For You" AI Feed:** Add a new tab or section that uses reading history as a prompt for Gemini to suggest titles.
- [ ]  **AI Metadata Enrichment:** Improve the "enrichMetadata" tool to automatically fix missing or poor descriptions/tags in the library.
- [ ]  **Multimodal Vibe Search:** Research and prototype using Gemini Vision for image-based search.

### Phase 3: Immersive Reader & Performance
Focused on the core reading experience.

- [ ]  **Atmospheric Audio:** Implement a system to play background ambient sounds that match the manga's genre or tags.
- [ ]  **Predictive Loading:** Analyze user reading speed and pre-load next chapters more intelligently.
- [ ]  **AI Image Upscaling:** Investigate lightweight on-device upscaling models (e.g., SRCNN or Waifu2x-Lite).

### Phase 4: Smarter Organization & Sync
Refining library management and connectivity.

- [ ]  **Dynamic Smart Categories:** Enable real-time updates for smart categories like "Hot" (last read 24h) and "Update Soon."
- [ ]  **Self-Hosted Sync:** Add WebDAV and Nextcloud providers to the synchronization settings.

## Verification & Stability
Each phase must be followed by:
1.  **Successful `assembleDevDebug` build.**
2.  **Manual Verification** of UI components and performance impacts.
3.  **Regression Testing** of core Mihon/SY features.
