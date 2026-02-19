# Dropbox Integration Specification

## Goal
Replace Google Drive and SyncYomi with Dropbox as the primary data synchronization solution, eliminating reliance on restricted Google Cloud secrets and providing a more accessible sync alternative.

## Scope
### Removal
- Removed `GoogleDriveSyncService` and `GoogleDriveService` dependencies.
- Removed `SyncYomiSyncService` and related configurations.
- Removed `sylibs.google.api.services.drive` and `sylibs.google.api.client.oauth` from `app/build.gradle.kts`.
- Removed `GoogleDriveLoginActivity` from `AndroidManifest.xml` and `SettingsDataScreen.kt`.

### Implementation
- **Dependency:** Added Dropbox Core and Android SDKs (`7.0.0`) via Gradle.
- **Service:** Created `DropboxSyncService` implementing the `SyncService` interface.
- **Authentication:** 
    - Used Dropbox SDK's `com.dropbox.core.android.AuthActivity` for the standard OAuth flow.
    - Added a **Manual Access Token** entry field in settings for direct token pasting.
    - Token capture handled automatically in `SettingsDataScreen` on return from login.
- **Configuration:** Updated `SyncManager` to support `DROPBOX` as the primary `SyncService` type.
- **UI:** Added Dropbox login, manual token entry, and data purge options to `SettingsDataScreen.kt`.
- **Security:** Access tokens are stored in `SyncPreferences`.

## Technical Details
- **Dropbox API:** Uses Dropbox API v2 via the official Java/Kotlin SDK.
- **App Key:** `6h2m965uod1itio` (Hardcoded in `DropboxSyncService` and manifest).
- **File Structure:**
    - Stores sync data at `/ShinKu/sync_data.proto.gz` in the user's Dropbox.
- **Permissions:** 
    - App requires `files.content.write` and `files.content.read` permissions.
    - Redirect URI: `db-6h2m965uod1itio://1/connect`.

## Verification
- Build verified (no Google Drive remnants).
- Dropbox login flow tested (AuthActivity handoff).
- Manual token entry allows immediate sync without OAuth flow.
