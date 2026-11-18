package com.shinku.reader.exh.source

object BlacklistedSources {
    val EHENTAI_EXT_SOURCES = longArrayOf(
        8100626124886895451,
        57122881048805941,
        4678440076103929247,
        1876021963378735852,
        3955189842350477641,
        4348288691341764259,
        773611868725221145,
        5759417018342755550,
        825187715438990384,
        6116711405602166104,
        7151438547982231541,
        2171445159732592630,
        3032959619549451093,
        5980349886941016589,
        6073266008352078708,
        5499077866612745456,
        6140480779421365791,
    )

    // ShinKu does not register built-in E-Hentai/ExHentai sources, so external E-Hentai
    // extensions do not conflict and should not be hidden from users.
    val BLACKLISTED_EXT_SOURCES = longArrayOf()

    val BLACKLISTED_EXTENSIONS = emptyArray<String>()

    var HIDDEN_SOURCES = setOf(
        MERGED_SOURCE_ID,
        EH_SOURCE_ID,
        EXH_SOURCE_ID,
    )
}
