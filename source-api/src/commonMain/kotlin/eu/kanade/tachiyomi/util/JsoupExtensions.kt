package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.shinku.reader.util.asJsoup as asJsoupNew
import com.shinku.reader.util.attrOrText as attrOrTextNew
import com.shinku.reader.util.selectInt as selectIntNew
import com.shinku.reader.util.selectText as selectTextNew

fun Element.selectText(css: String, defaultValue: String? = null): String? = selectTextNew(css, defaultValue)

fun Element.selectInt(css: String, defaultValue: Int = 0): Int = selectIntNew(css, defaultValue)

fun Element.attrOrText(css: String): String = attrOrTextNew(css)

fun Response.asJsoup(html: String? = null): Document = asJsoupNew(html)
