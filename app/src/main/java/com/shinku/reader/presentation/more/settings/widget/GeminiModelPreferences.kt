package com.shinku.reader.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.shinku.reader.domain.ai.GeminiModel
import com.shinku.reader.domain.ai.GeminiModels
import com.shinku.reader.i18n.MR
import com.shinku.reader.presentation.core.i18n.stringResource
import com.shinku.reader.presentation.core.util.collectAsState
import com.shinku.reader.presentation.more.settings.Preference
import com.shinku.reader.util.system.toast
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.shinku.reader.core.common.preference.Preference as StoredPreference

@Composable
fun geminiModelPreferences(
    modelPreference: StoredPreference<String>,
    apiKey: String,
    autoLoad: Boolean = true,
    loadModels: suspend (key: String, forceRefresh: Boolean) -> List<GeminiModel>,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected by modelPreference.collectAsState()
    var catalog by remember(apiKey) { mutableStateOf<List<GeminiModel>?>(null) }
    var loading by remember(apiKey) { mutableStateOf(false) }
    var error by remember(apiKey) { mutableStateOf<String?>(null) }

    suspend fun load(forceRefresh: Boolean) {
        if (loading || apiKey.isBlank()) return
        loading = true
        error = null
        try {
            catalog = loadModels(apiKey, forceRefresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            catalog = null
            error = e.message ?: context.getString(MR.strings.gemini_catalog_failed.resourceId)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(apiKey, autoLoad) { if (autoLoad) load(false) }
    val entries = linkedMapOf("auto" to stringResource(MR.strings.gemini_model_automatic))
    catalog.orEmpty().forEach { entries[it.id] = "${it.displayName} (${it.id})" }
    if (selected.isNotBlank() && selected !in entries) {
        entries[selected] = stringResource(MR.strings.gemini_saved_model, selected)
    }
    val catalogStatus = when {
        loading -> stringResource(MR.strings.gemini_catalog_loading)
        error != null -> error!!
        catalog == null -> stringResource(MR.strings.gemini_catalog_key_needed)
        catalog!!.isEmpty() -> stringResource(MR.strings.gemini_catalog_empty)
        else -> stringResource(MR.strings.gemini_catalog_loaded, catalog!!.size)
    }
    return listOf(
        Preference.PreferenceItem.ListPreference(
            preference = modelPreference,
            title = stringResource(MR.strings.pref_gemini_model),
            subtitle = stringResource(MR.strings.gemini_model_summary),
            entries = entries.toImmutableMap(),
        ),
        Preference.PreferenceItem.TextPreference(
            title = stringResource(if (loading) MR.strings.gemini_catalog_loading else MR.strings.gemini_catalog_refresh),
            subtitle = stringResource(MR.strings.gemini_catalog_refresh_summary),
            onClick = {
                if (apiKey.isBlank()) {
                    context.toast(MR.strings.gemini_catalog_key_needed)
                } else {
                    scope.launch { load(true) }
                }
            },
        ),
        Preference.PreferenceItem.InfoPreference(catalogStatus),
        Preference.PreferenceItem.EditTextPreference(
            preference = modelPreference,
            title = stringResource(MR.strings.gemini_custom_model),
            subtitle = stringResource(MR.strings.gemini_custom_model_summary),
            onValueChanged = { value ->
                val valid = runCatching { GeminiModels.resolve(value) }.isSuccess
                if (!valid) context.toast(MR.strings.gemini_model_invalid)
                valid
            },
        ),
    )
}
