# Implementation Plan: Library Update Speed Toggle

## Phase 1: Preparation (COMPLETED)
- [x] Research `LibraryUpdateJob` concurrency logic.
- [x] Identify preference storage in `LibraryPreferences`.

## Phase 2: Implementation (COMPLETED)
- [x] Add preference definition for update speed.
- [x] Add string resources for speed settings.
- [x] Update `LibraryUpdateJob` logic for chapters and covers (Concurrency).
- [x] Add UI toggle in `SettingsLibraryScreen`.
- [x] Create SQLDelight migration (38.sqm) for `last_metadata_update`.
- [x] Update `Manga` model and mapper for new field.
- [x] Implement 7-day cooldown logic in `LibraryUpdateJob`.
- [x] Update `MangaRestorer` and `LibraryQuery` to support new field.
- [x] Create `SmartCategorizer` interactor with priority logic.
- [x] Create `SmartCategorizerJob` with foreground notification.
- [x] Add string resources for Smart Categorizer.
- [x] Add trigger button in `SettingsLibraryScreen`.

## Phase 3: Validation (COMPLETED)
- [x] Verify successful build (Standard and Dev flavors).
- [x] Ensure database integrity with migration.
- [ ] Manual testing of update speed and cooldown logic (User verification).
- [ ] Manual testing of Smart Categorizer (User verification).
