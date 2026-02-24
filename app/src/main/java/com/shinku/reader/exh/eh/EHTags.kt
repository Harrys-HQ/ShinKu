package com.shinku.reader.exh.eh

import com.shinku.reader.exh.eh.tags.Artist
import com.shinku.reader.exh.eh.tags.Artist2
import com.shinku.reader.exh.eh.tags.Character
import com.shinku.reader.exh.eh.tags.Cosplayer
import com.shinku.reader.exh.eh.tags.Female
import com.shinku.reader.exh.eh.tags.Group
import com.shinku.reader.exh.eh.tags.Group2
import com.shinku.reader.exh.eh.tags.Language
import com.shinku.reader.exh.eh.tags.Male
import com.shinku.reader.exh.eh.tags.Mixed
import com.shinku.reader.exh.eh.tags.Other
import com.shinku.reader.exh.eh.tags.Parody
import com.shinku.reader.exh.eh.tags.Reclass

object EHTags {
    fun getAllTags(): List<String> = listOf(
        Female.getTags(),
        Male.getTags(),
        Language.getTags(),
        Reclass.getTags(),
        Mixed.getTags(),
        Other.getTags(),
        Cosplayer.getTags(),
        Parody.getTags(),
        Character.getTags(),
        Group.getTags(),
        Group2.getTags(),
        Artist.getTags(),
        Artist2.getTags(),
    ).flatten().flatten()

    fun getNamespaces(): List<String> = listOf(
        "reclass",
        "language",
        "parody",
        "character",
        "group",
        "artist",
        "cosplayer",
        "male",
        "female",
        "mixed",
        "other",
    )
}
