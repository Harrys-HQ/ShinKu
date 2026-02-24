package com.shinku.reader.ui.browse.migration.advanced.design

import eu.davidea.flexibleadapter.FlexibleAdapter
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy

class MigrationSourceAdapter(
    listener: FlexibleAdapter.OnItemClickListener,
) : FlexibleAdapter<MigrationSourceItem>(
    null,
    listener,
    true,
) {
    val sourceManager: SourceManager by injectLazy()

    // SY _->
    val sourcePreferences: SourcePreferences by injectLazy()
    // SY <--
}
