package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * An interceptor that repairs Next.js React Server Component (RSC) flight data streams where
 * earlier layout/context chunks contain a summary "chapter" object lacking a "pages" array,
 * which erroneously preempts the actual reader component when extensions extract ChapterDetailDto.
 */
class NextJsRscInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            return response
        }

        val contentType = response.header("Content-Type") ?: ""
        val isRsc = "text/x-component" in contentType || request.header("rsc") == "1"

        if (!isRsc) {
            return response
        }

        val body = response.body ?: return response
        val rawBody = try {
            body.string()
        } catch (_: Throwable) {
            return response
        }

        val fixedBody = fixRscChapterPages(rawBody)
        val mediaType = response.header("Content-Type")?.toMediaTypeOrNull() ?: body.contentType()

        return response.newBuilder()
            .body(fixedBody.toResponseBody(mediaType))
            .build()
    }

    private fun fixRscChapterPages(body: String): String {
        val pagesRegex = Regex("""\"pages\"\s*:\s*\[""")
        val pagesMatch = pagesRegex.find(body) ?: return body
        val pagesIdx = pagesMatch.range.first

        val chapterRegex = Regex("""\"chapter\"\s*:\s*\{""")
        val prefixMatches = chapterRegex.findAll(body.substring(0, pagesIdx)).toList()
        if (prefixMatches.size <= 1) {
            // If there's 0 or only 1 chapter before pages, the last one before pages is the real reader chapter
            return body
        }

        val realChapterMatch = prefixMatches.last()
        val realChapterIdx = realChapterMatch.range.first

        val prefix = body.substring(0, realChapterIdx)
        val fixedPrefix = prefix.replace(chapterRegex, "\"chapterMeta\":{")
        return fixedPrefix + body.substring(realChapterIdx)
    }
}
