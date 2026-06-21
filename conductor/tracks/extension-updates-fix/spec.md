# Specification: Extension Updates Fix

## Background
Users report that when they update an extension, the list temporarily refreshes indicating that the extension was updated, but then it goes back to the updates pending list.

## Investigation & Root Cause
1. **Lazy State Flow Issue:** `availableExtensionsFlow` in `ExtensionManager` is mapped using `stateIn(scope, SharingStarted.Lazily, ...)`:
   ```kotlin
   val availableExtensionsFlow = availableExtensionMapFlow.map { it.filterNotBlacklisted().values.toList() }
       .stateIn(scope, SharingStarted.Lazily, availableExtensionMapFlow.value.values.toList())
   ```
   Under `SharingStarted.Lazily`, the flow's value is only kept updated when there are active collectors. When accessed via `.value` outside of active subscription scopes (e.g., in `ExtensionInstallReceiver` background thread or tasks), it returns stale or empty data.
2. **Missing Package Update Wait Loop:** In `ExtensionInstallReceiver.onReceive`, the logic to wait for the package manager to register the updated version relies on:
   ```kotlin
   val expectedVersionCode = try {
       Injekt.get<ExtensionManager>().availableExtensionsFlow.value.find { it.pkgName == pkgName }?.versionCode
   } catch (e: Exception) {
       null
   }
   ```
   Because `availableExtensionsFlow.value` returns empty or stale data, `expectedVersionCode` is `null`. Consequently, the loop that waits for the new version code to be loaded is skipped.
3. **PackageManager Cache Race:** Without the loop waiting for the updated version, `ExtensionLoader.loadExtensionFromPkgName(context, pkgName)` is called immediately after a fixed delay of 2 seconds. In many instances (especially on slower devices or newer Android versions), the PackageManager cache is not yet updated, causing the old package info (and version code) to be read.
4. **Stale Registration:** The extension is registered back into the map with its old version code, which makes `updateExists()` return `true` when available extensions are refreshed, returning the extension back to the updates pending list.

## Proposed Solution
1. **Promote Sharing to Eager:** Change `SharingStarted.Lazily` to `SharingStarted.Eagerly` for `availableExtensionsFlow` and the `mapExtensions` extension method in `ExtensionManager.kt`.
2. **Direct Available Extension Lookup:** Implement a public helper function `getAvailableExtension(pkgName: String): Extension.Available?` in `ExtensionManager.kt` to bypass the flow mapping entirely and look up available extensions directly from the underlying `availableExtensionMapFlow.value`.
3. **Use Helper in Receiver:** Update `ExtensionInstallReceiver.kt` to use the new `getAvailableExtension(pkgName)` helper to reliably retrieve the expected version code and ensure the wait loop triggers correctly.
