package com.shinku.reader.exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import com.shinku.reader.exh.md.dto.SimilarMangaDto
import com.shinku.reader.exh.md.utils.MdUtil
import okhttp3.OkHttpClient

class SimilarService(
    private val client: OkHttpClient,
) {
    suspend fun getSimilarManga(mangaId: String): SimilarMangaDto {
        return with(MdUtil.jsonParser) {
            client.newCall(
                GET(
                    "${MdUtil.similarBaseApi}$mangaId.json",
                ),
            ).awaitSuccess().parseAs()
        }
    }
}
