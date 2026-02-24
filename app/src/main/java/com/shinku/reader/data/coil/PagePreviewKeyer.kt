package com.shinku.reader.data.coil

import coil3.key.Keyer
import coil3.request.Options
import com.shinku.reader.domain.manga.model.PagePreview

class PagePreviewKeyer : Keyer<PagePreview> {
    override fun key(data: PagePreview, options: Options): String {
        return data.imageUrl
    }
}
