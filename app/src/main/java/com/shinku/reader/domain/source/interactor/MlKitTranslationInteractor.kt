package com.shinku.reader.domain.source.interactor

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MlKitTranslationInteractor {

    private val translatorCache = ConcurrentHashMap<String, Translator>()

    suspend fun translateBlocks(
        blocks: List<GeminiVibeSearch.TranslationBlockInput>,
        targetLanguageName: String,
        sourceLanguageName: String = "Auto-Detect",
        onModelDownloadStarting: suspend () -> Unit = {},
    ): List<GeminiVibeSearch.TranslationBlockOutput> = withContext(Dispatchers.IO) {
        if (blocks.isEmpty()) return@withContext emptyList()

        val targetLangCode = languageNameToCode(targetLanguageName)
        val combinedText = blocks.joinToString(" ") { it.text }
        val sourceLangCode = if (sourceLanguageName.equals("Auto-Detect", ignoreCase = true)) {
            detectLanguageFromText(combinedText, fallback = TranslateLanguage.JAPANESE)
        } else {
            languageNameToCode(sourceLanguageName)
        }

        if (sourceLangCode == targetLangCode) {
            // Already in target language: return blocks without error
            return@withContext blocks.map { GeminiVibeSearch.TranslationBlockOutput(it.id, it.text) }
        }

        val cacheKey = "$sourceLangCode->$targetLangCode"
        val translator = translatorCache.computeIfAbsent(cacheKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()
            Translation.getClient(options)
        }

        try {
            onModelDownloadStarting()
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions).await()
        } catch (e: Exception) {
            throw Exception("Failed to download on-device translation model: ${e.localizedMessage ?: e.message}")
        }

        var failureCount = 0
        var lastError: Exception? = null

        val outputs = coroutineScope {
            blocks.map { block ->
                async {
                    try {
                        val translated = translator.translate(block.text).await()
                        if (translated.isNotBlank()) {
                            GeminiVibeSearch.TranslationBlockOutput(block.id, translated.trim())
                        } else {
                            GeminiVibeSearch.TranslationBlockOutput(block.id, block.text)
                        }
                    } catch (e: Exception) {
                        failureCount++
                        lastError = e
                        GeminiVibeSearch.TranslationBlockOutput(block.id, block.text)
                    }
                }
            }.awaitAll()
        }

        if (failureCount == blocks.size && lastError != null) {
            throw Exception("On-device translation failed: ${lastError?.localizedMessage ?: lastError?.message}")
        }

        outputs
    }

    private fun detectLanguageFromText(text: String, fallback: String): String {
        for (char in text) {
            val code = char.code
            // Hangul syllables & jamo (Korean)
            if (code in 0xAC00..0xD7AF || code in 0x1100..0x11FF || code in 0x3130..0x318F) {
                return TranslateLanguage.KOREAN
            }
            // Hiragana & Katakana (Japanese)
            if (code in 0x3040..0x309F || code in 0x30A0..0x30FF) {
                return TranslateLanguage.JAPANESE
            }
        }
        // Hanzi / Kanji (Chinese)
        for (char in text) {
            val code = char.code
            if (code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF) {
                return TranslateLanguage.CHINESE
            }
        }
        // Latin letters (English / Spanish / etc.)
        for (char in text) {
            if (char in 'a'..'z' || char in 'A'..'Z') {
                return TranslateLanguage.ENGLISH
            }
        }
        return fallback
    }

    private fun languageNameToCode(name: String): String {
        return when (name.lowercase().trim()) {
            "japanese", "ja" -> TranslateLanguage.JAPANESE
            "chinese", "zh" -> TranslateLanguage.CHINESE
            "korean", "ko" -> TranslateLanguage.KOREAN
            "english", "en" -> TranslateLanguage.ENGLISH
            "spanish", "es" -> TranslateLanguage.SPANISH
            "french", "fr" -> TranslateLanguage.FRENCH
            "german", "de" -> TranslateLanguage.GERMAN
            "portuguese", "pt" -> TranslateLanguage.PORTUGUESE
            "italian", "it" -> TranslateLanguage.ITALIAN
            "russian", "ru" -> TranslateLanguage.RUSSIAN
            "indonesian", "id" -> TranslateLanguage.INDONESIAN
            "tagalog", "tl", "filipino" -> TranslateLanguage.TAGALOG
            "arabic", "ar" -> TranslateLanguage.ARABIC
            else -> TranslateLanguage.ENGLISH
        }
    }

    private fun languageCodeToName(code: String): String {
        return when (code) {
            TranslateLanguage.ENGLISH -> "English"
            TranslateLanguage.JAPANESE -> "Japanese"
            TranslateLanguage.CHINESE -> "Chinese"
            TranslateLanguage.KOREAN -> "Korean"
            TranslateLanguage.SPANISH -> "Spanish"
            TranslateLanguage.FRENCH -> "French"
            TranslateLanguage.GERMAN -> "German"
            TranslateLanguage.PORTUGUESE -> "Portuguese"
            TranslateLanguage.ITALIAN -> "Italian"
            TranslateLanguage.RUSSIAN -> "Russian"
            TranslateLanguage.INDONESIAN -> "Indonesian"
            TranslateLanguage.TAGALOG -> "Tagalog"
            TranslateLanguage.ARABIC -> "Arabic"
            else -> code
        }
    }
}
