package com.shinku.reader.domain.storage.service

import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.storage.FolderProvider

class StoragePreferences(
    private val folderProvider: FolderProvider,
    private val preferenceStore: PreferenceStore,
) {

    fun baseStorageDirectory() = preferenceStore.getString(Preference.appStateKey("storage_dir"), folderProvider.path())
}
