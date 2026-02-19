# Implementation Plan: Dropbox Integration

## Phase 1: Preparation
- [x] Research existing `SyncService` architecture (Google Drive/SyncYomi).
- [x] Identify dependencies for removal (`build.gradle.kts`, `sy.versions.toml`).
- [ ] Add Dropbox SDK dependency (`com.dropbox.core:dropbox-core-sdk`) to `sy.versions.toml` and `app/build.gradle.kts`.

## Phase 2: Removal (COMPLETED)
- [x] Remove `GoogleDriveSyncService` and `GoogleDriveService`.
- [x] Remove `SyncYomiSyncService` and its related code.
- [x] Clean up `AndroidManifest.xml` (Remove `GoogleDriveLoginActivity`).
- [x] Update `SyncManager.kt` to remove `GOOGLE_DRIVE` and `SYNCYOMI`.
- [x] Clean up `SettingsDataScreen.kt` (Remove obsolete sync options).

## Phase 3: Implementation (COMPLETED)
- [x] Create `DropboxSyncService.kt` implementing `SyncService`.
- [x] Implement `DropboxLoginActivity.kt` for OAuth flow.
- [x] Register `DropboxLoginActivity` in `AndroidManifest.xml`.
- [x] Update `SyncManager.kt` to include `DROPBOX` sync service.
- [x] Add Dropbox login and sync options to `SettingsDataScreen.kt`.
- [x] Implement `DropboxSyncService` logic.
- [x] Implement `DropboxLoginActivity` logic.

## Phase 4: Validation
- [ ] Verify successful build.
- [ ] Test Dropbox login flow (Requires valid App Key).
- [ ] Verify sync upload (creates file in Dropbox).
- [ ] Verify sync download (retrieves file from Dropbox).
