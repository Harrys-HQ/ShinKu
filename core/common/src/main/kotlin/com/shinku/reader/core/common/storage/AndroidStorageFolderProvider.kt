package com.shinku.reader.core.common.storage

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR
import java.io.File

class AndroidStorageFolderProvider(
    private val context: Context,
) : FolderProvider {

    override fun directory(): File {
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                context.stringResource(MR.strings.app_name),
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}
