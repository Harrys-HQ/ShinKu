# Feature Specification: Library Update Speed Toggle

## Objective
Allow users to control the speed and resource usage of library updates by adjusting the number of concurrent sources and manga being processed.

## Requirements
1.  **Standard Mode**: 5 concurrent sources, 1 manga per source (Default).
2.  **Boost Mode**: 10 concurrent sources, 2 manga per source.
3.  **Extreme Mode**: No limit on concurrent sources or manga.
4.  **Metadata Refresh Cooldown**: Automatic metadata updates (artist, author, description, etc.) should only occur if the last successful refresh was at least 7 days ago.
5.  Persistence of setting across app restarts.
6.  User-friendly toggle in the Library Settings (for speed).
7.  **Smart Categorizer**:
    *   Automatically organize library into 4 smart categories:
        *   **Dropped**: Status is `CANCELLED` or `ON_HIATUS`.
        *   **Finished**: Status is `COMPLETED` or `PUBLISHING_FINISHED`.
        *   **Reading**: Read chapters >= 5.
        *   **Queue**: Read chapters < 5.
    *   Priority-based assignment (Dropped > Finished > Reading > Queue).
    *   Manual trigger button in Settings.
    *   Progress notification during execution.

## Implementation Details
- Added `library_update_speed` to `LibraryPreferences`.
- Added `last_metadata_update` column to `mangas` table via SQLDelight migration (38.sqm).
- Updated `Manga` domain model and `MangaMapper` to handle the new field.
- Updated `LibraryUpdateJob` to use `Semaphore` based on the selected speed and enforced 7-day cooldown.
- Created `SmartCategorizer` interactor for the categorization logic.
- Created `SmartCategorizerJob` for background execution with notifications.
- Added UI in `SettingsLibraryScreen` for speed toggle and smart categorizer trigger.
