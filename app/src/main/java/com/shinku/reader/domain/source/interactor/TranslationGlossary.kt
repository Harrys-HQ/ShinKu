package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.source.interactor.GeminiVibeSearch.TranslationBlockInput
import com.shinku.reader.domain.source.interactor.GeminiVibeSearch.TranslationBlockOutput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.Locale

@Serializable
data class TranslationGlossary(
    val entries: List<Entry> = emptyList(),
    val preserveHonorifics: Boolean = false,
) {
    @Serializable
    data class Entry(val source: String = "", val preferred: String = "")

    fun normalized(): TranslationGlossary = copy(
        entries = entries.map { Entry(it.source.trim(), it.preferred.trim()) }
            .filter { it.source.isNotEmpty() && it.preferred.isNotEmpty() }
            .distinctBy { it.source.lowercase(Locale.ROOT) },
    )
}

internal fun translationCacheKey(sourceLanguage: String, targetLanguage: String, provider: String, glossary: TranslationGlossary): String {
    val identity = Json.encodeToString(listOf("reader-v1", sourceLanguage, targetLanguage, provider, Json.encodeToString(glossary)))
    val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
    return "reader" + digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

internal fun TranslationGlossary.cloudInstructions(blocks: List<TranslationBlockInput>): String = buildString {
    val matching = entries.filter { entry ->
        val term = glossaryTermPattern(entry.source).toRegex(RegexOption.IGNORE_CASE)
        blocks.any { term.containsMatchIn(it.text) }
    }
        .map { TranslationGlossary.Entry(sanitizeReaderTranslationText(it.source), sanitizeReaderTranslationText(it.preferred)) }
    if (matching.isNotEmpty()) {
        appendLine("Use these preferred translations for matching terms: ${Json.encodeToString(matching)}.")
    }
    if (preserveHonorifics) appendLine("Preserve honorific suffixes in the translated dialogue.")
}.trimEnd()

private val honorifics = mapOf(
    "-san" to "-san", "-sama" to "-sama", "-kun" to "-kun", "-chan" to "-chan",
    "-senpai" to "-senpai", "-sensei" to "-sensei",
    "さん" to "-san", "さま" to "-sama", "様" to "-sama", "くん" to "-kun", "君" to "-kun",
    "ちゃん" to "-chan", "先輩" to "-senpai", "先生" to "-sensei",
)

private data class GlossaryPart(val literal: String, val translationId: Int? = null)

private fun glossaryTermPattern(term: String): String {
    val escaped = Regex.escape(term)
    return when {
        term.first().isLetter() && term.first().code < 128 -> "(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])"
        term.startsWith("-") -> "(?<=[\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])"
        else -> escaped
    }
}

internal suspend fun TranslationGlossary.translateLocally(
    blocks: List<TranslationBlockInput>,
    translate: suspend (List<TranslationBlockInput>) -> List<TranslationBlockOutput>,
): List<TranslationBlockOutput> {
    val replacements = buildMap {
        if (preserveHonorifics) putAll(honorifics)
        this@translateLocally.entries.forEach { put(it.source.lowercase(Locale.ROOT), it.preferred) }
    }
    if (replacements.isEmpty()) return translate(blocks)

    val terms = replacements.keys.sortedByDescending { it.length }.joinToString("|", transform = ::glossaryTermPattern)
        .toRegex(RegexOption.IGNORE_CASE)
    val inputs = mutableListOf<TranslationBlockInput>()
    fun splitText(value: String): List<GlossaryPart> {
        if (value.isBlank()) return listOf(GlossaryPart(value))
        val leading = value.takeWhile(Char::isWhitespace)
        val trailing = value.takeLastWhile(Char::isWhitespace)
        val id = inputs.size
        inputs += TranslationBlockInput(id, value.trim())
        return listOf(GlossaryPart(leading), GlossaryPart(value.trim(), id), GlossaryPart(trailing))
    }
    val parts = blocks.map { block ->
        buildList {
            var offset = 0
            for (match in terms.findAll(block.text)) {
                addAll(splitText(block.text.substring(offset, match.range.first)))
                add(GlossaryPart(replacements[match.value.lowercase(Locale.ROOT)] ?: match.value))
                offset = match.range.last + 1
            }
            addAll(splitText(block.text.substring(offset)))
        }
    }
    val translated = if (inputs.isEmpty()) emptyMap() else translate(inputs).associate { it.id to it.text }
    return blocks.zip(parts) { block, blockParts ->
        TranslationBlockOutput(block.id, blockParts.joinToString("") { translated[it.translationId] ?: it.literal })
    }
}
