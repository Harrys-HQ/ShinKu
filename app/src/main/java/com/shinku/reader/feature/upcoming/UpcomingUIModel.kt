package com.shinku.reader.feature.upcoming

import com.shinku.reader.domain.manga.model.Manga
import java.time.LocalDate

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val mangaCount: Int) : UpcomingUIModel
    data class Item(val manga: Manga) : UpcomingUIModel
}
