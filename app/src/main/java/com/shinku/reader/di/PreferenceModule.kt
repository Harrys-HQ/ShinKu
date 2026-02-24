package com.shinku.reader.di

import android.app.Application
import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.domain.sync.SyncPreferences
import com.shinku.reader.domain.track.service.TrackPreferences
import com.shinku.reader.domain.ui.UiPreferences
import com.shinku.reader.core.security.PrivacyPreferences
import com.shinku.reader.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import com.shinku.reader.ui.reader.setting.ReaderPreferences
import com.shinku.reader.util.system.isDevFlavor
import com.shinku.reader.core.common.preference.AndroidPreferenceStore
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.storage.AndroidStorageFolderProvider
import com.shinku.reader.domain.backup.service.BackupPreferences
import com.shinku.reader.domain.download.service.DownloadPreferences
import com.shinku.reader.domain.library.service.LibraryPreferences
import com.shinku.reader.domain.storage.service.StoragePreferences
import uy.kohesive.injekt.api.InjektRegistrar

class PreferenceModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> {
            AndroidPreferenceStore(app)
        }
        addSingletonFactory {
            NetworkPreferences(
                preferenceStore = get(),
                verboseLogging = isDevFlavor,
            )
        }
        addSingletonFactory {
            SourcePreferences(get())
        }
        addSingletonFactory {
            SecurityPreferences(get())
        }
        addSingletonFactory {
            PrivacyPreferences(get())
        }
        addSingletonFactory {
            LibraryPreferences(get())
        }
        addSingletonFactory {
            ReaderPreferences(get())
        }
        addSingletonFactory {
            TrackPreferences(get())
        }
        addSingletonFactory {
            DownloadPreferences(get())
        }
        addSingletonFactory {
            BackupPreferences(get())
        }
        addSingletonFactory {
            StoragePreferences(
                folderProvider = get<AndroidStorageFolderProvider>(),
                preferenceStore = get(),
            )
        }
        addSingletonFactory {
            UiPreferences(get())
        }
        addSingletonFactory {
            BasePreferences(app, get())
        }

        addSingletonFactory {
            SyncPreferences(get())
        }
    }
}
