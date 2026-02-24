package com.shinku.reader.exh.md.handlers

import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.SManga
import com.shinku.reader.exh.md.dto.RelationListDto
import com.shinku.reader.exh.md.dto.SimilarMangaDto
import com.shinku.reader.exh.md.service.MangaDexService
import com.shinku.reader.exh.md.service.SimilarService
import com.shinku.reader.exh.md.utils.MangaDexRelation
import com.shinku.reader.exh.md.utils.MdUtil
import com.shinku.reader.exh.metadata.metadata.MangaDexSearchMetadata
import com.shinku.reader.core.common.util.lang.withIOContext

class SimilarHandler(
    private val lang: String,
    private val service: MangaDexService,
    private val similarService: SimilarService,
) {

    suspend fun getSimilar(manga: SManga): MetadataMangasPage {
        val similarDto = withIOContext { similarService.getSimilarManga(MdUtil.getMangaId(manga.url)) }
        return similarDtoToMangaListPage(similarDto)
    }

    private suspend fun similarDtoToMangaListPage(
        similarMangaDto: SimilarMangaDto,
    ): MetadataMangasPage {
        val ids = similarMangaDto.matches.map {
            it.id
        }

        val mangaList = service.viewMangas(ids).data.map {
            MdUtil.createMangaEntry(it, lang)
        }

        return MetadataMangasPage(
            mangaList, false,
            List(mangaList.size) {
                MangaDexSearchMetadata().also { it.relation = MangaDexRelation.SIMILAR }
            },
        )
    }

    suspend fun getRelated(manga: SManga): MetadataMangasPage {
        val relatedListDto = withIOContext { service.relatedManga(MdUtil.getMangaId(manga.url)) }
        return relatedDtoToMangaListPage(relatedListDto)
    }

    private suspend fun relatedDtoToMangaListPage(
        relatedListDto: RelationListDto,
    ): MetadataMangasPage {
        val ids = relatedListDto.data
            .mapNotNull { it.relationships.firstOrNull() }
            .map { it.id }

        val mangaList = service.viewMangas(ids).data.map {
            MdUtil.createMangaEntry(it, lang)
        }

        return MetadataMangasPage(
            mangas = mangaList,
            hasNextPage = false,
            mangasMetadata = mangaList.map { manga ->
                MangaDexSearchMetadata().also {
                    it.relation = relatedListDto.data
                        .firstOrNull { it.relationships.any { it.id == MdUtil.getMangaId(manga.url) } }
                        ?.attributes?.relation?.let(MangaDexRelation::fromDex)
                }
            },
        )
    }
}
