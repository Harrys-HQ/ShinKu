package com.shinku.reader.di

import android.app.Application
import com.shinku.reader.exh.pref.DelegateSourcePreferences
import com.shinku.reader.exh.source.ExhPreferences
import com.shinku.reader.exh.source.ShinKuPreferences
import uy.kohesive.injekt.api.InjektRegistrar

class SYPreferenceModule(val application: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory {
            DelegateSourcePreferences(
                preferenceStore = get(),
            )
        }

        addSingletonFactory {
            ExhPreferences(get())
        }

        addSingletonFactory {
            ShinKuPreferences(get())
        }
    }
}
