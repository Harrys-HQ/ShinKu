package com.shinku.reader.exh.md

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.shinku.reader.ui.setting.track.BaseOAuthLoginActivity
import com.shinku.reader.exh.md.utils.MdUtil
import kotlinx.coroutines.flow.first
import com.shinku.reader.core.common.util.lang.launchIO
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaDexLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code != null) {
            lifecycleScope.launchIO {
                val sourceManager = Injekt.get<SourceManager>()
                sourceManager.isInitialized.first { it }
                MdUtil.getEnabledMangaDex(sourceManager = sourceManager)?.login(code)
                returnToSettings()
            }
        } else {
            lifecycleScope.launchIO {
                val sourceManager = Injekt.get<SourceManager>()
                sourceManager.isInitialized.first { it }
                MdUtil.getEnabledMangaDex(sourceManager = sourceManager)?.logout()
                returnToSettings()
            }
        }
    }
}
