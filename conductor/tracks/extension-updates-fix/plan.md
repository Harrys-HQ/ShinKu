# Implementation Plan: Extension Updates Fix

## Phase 1: Modify ExtensionManager
- [x] Update `ExtensionManager.kt` to use `SharingStarted.Eagerly` for `availableExtensionsFlow` and the `mapExtensions` helper.
- [x] Add the `getAvailableExtension(pkgName: String): Extension.Available?` helper method in `ExtensionManager.kt`.

## Phase 2: Modify ExtensionInstallReceiver
- [x] Update `ExtensionInstallReceiver.kt` to use `getAvailableExtension(pkgName)` instead of `availableExtensionsFlow.value.find { ... }`.

## Phase 2.5: Deduplicate Repositories
- [x] Deduplicate available extensions by package name in [ExtensionApi.kt](file:///C:/Users/squal/OneDrive/Documents/App-Dev/ShinKu/app/src/main/java/com/shinku/reader/extension/api/ExtensionApi.kt) to resolve version code mismatches between multiple repositories.

## Phase 3: Verification
- [x] Run the project compilation to verify build success: `./gradlew assembleDevRelease`.


