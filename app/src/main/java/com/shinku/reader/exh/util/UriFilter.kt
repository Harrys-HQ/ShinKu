package com.shinku.reader.exh.util

import android.net.Uri

/**
 * Uri filter
 */
interface UriFilter {
    fun addToUri(builder: Uri.Builder)
}
