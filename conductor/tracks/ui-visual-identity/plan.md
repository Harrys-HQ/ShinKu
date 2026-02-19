# Implementation Plan: UI & Visual Identity Overhaul

## Phase 1: Infrastructure (COMPLETED)
- [x] Add `androidx.palette` dependency.
- [x] Create a `ThemeHelper` or update `TachiyomiTheme` to support dynamic seed colors.
- [x] Add `material-kolor` for full scheme generation.

## Phase 2: Dynamic Theme (COMPLETED)
- [x] Implement color extraction from Coil's image cache or bitmap.
- [x] Update `MangaScreen` to wrap its content in a dynamic theme provider.

## Phase 3: Manga Detail Page Redesign (COMPLETED)
- [x] Redesign header with blurred/faded background cover art.
- [x] Improve spacing and typography for description and metadata.

## Phase 4: Drag-and-Drop Library (SKIPPED/DELAYED)
- [ ] Research Compose `Reorderable` library or implement custom reordering logic.
- [ ] Update `LibraryContent` to support drag-and-drop gestures.

## Phase 5: Validation (COMPLETED)
- [x] Build and verify on multiple devices/flavors.
- [x] Ensure theme transitions are smooth.
