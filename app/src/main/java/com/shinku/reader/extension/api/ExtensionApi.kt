package com.shinku.reader.extension.api

import android.content.Context
import com.shinku.reader.core.common.preference.Preference
import com.shinku.reader.core.common.preference.PreferenceStore
import com.shinku.reader.core.common.util.lang.withIOContext
import com.shinku.reader.core.common.util.system.logcat
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepo
import com.shinku.reader.domain.extensionrepo.interactor.UpdateExtensionRepo
import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.exh.source.BlacklistedSources
import com.shinku.reader.extension.ExtensionManager
import com.shinku.reader.extension.model.Extension
import com.shinku.reader.extension.model.LoadResult
import com.shinku.reader.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import logcat.LogPriority
import okio.BufferedSource
import okio.buffer
import okio.gzip
import uy.kohesive.injekt.injectLazy
import java.time.Instant
import kotlin.time.Duration.Companion.days

internal class ExtensionApi {

    private val networkService: NetworkHelper by injectLazy()
    private val preferenceStore: PreferenceStore by injectLazy()
    private val getExtensionRepo: GetExtensionRepo by injectLazy()
    private val updateExtensionRepo: UpdateExtensionRepo by injectLazy()
    private val extensionManager: ExtensionManager by injectLazy()
    private val protoBuf: ProtoBuf by injectLazy()

    // SY -->
    private val sourcePreferences: SourcePreferences by injectLazy()

    // SY <--
    private val json: Json by injectLazy()

    private val lastExtCheck: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_ext_check"), 0)
    }

    suspend fun findExtensions(): List<Extension.Available> {
        return withIOContext {
            val extensions = getExtensionRepo.getAll()
                .map { async { getExtensions(it) } }
                .awaitAll()
                .flatten()

            extensions
                .groupBy { it.pkgName }
                .mapValues { (_, list) ->
                    list.maxWith(
                        compareBy<Extension.Available> { it.versionCode }
                            .thenBy { it.libVersion }
                    )
                }
                .values
                .toList()
        }
    }

    private suspend fun getExtensions(extRepo: ExtensionRepo): List<Extension.Available> {
        val repoBaseUrl = extRepo.baseUrl
        return try {
            // Prefer protobuf index (Keiyoushi and modern repos stub/deprecate index.min.json)
            fetchProtobufExtensions(repoBaseUrl)
                ?.takeIf { it.isNotEmpty() }
                ?: fetchJsonExtensions(repoBaseUrl)
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Failed to get extensions from $repoBaseUrl" }
            emptyList()
        }
    }

    private suspend fun fetchProtobufExtensions(repoBaseUrl: String): List<Extension.Available>? {
        return try {
            val response = networkService.client
                .newCall(GET("$repoBaseUrl/index.pb"))
                .awaitSuccess()

            val indexDto = response.body.source().decompressIfGzipped().use { source ->
                protoBuf.decodeFromByteArray<ExtensionIndexDto>(source.readByteArray())
            }

            val extensions = if (indexDto.extensionList != null && indexDto.extensionList.extensions.isNotEmpty()) {
                indexDto.extensionList.extensions
            } else if (!indexDto.extensionListUrl.isNullOrEmpty()) {
                val listUrl = when {
                    indexDto.extensionListUrl.startsWith("http://") || indexDto.extensionListUrl.startsWith("https://") -> indexDto.extensionListUrl
                    indexDto.extensionListUrl.startsWith("/") -> "$repoBaseUrl${indexDto.extensionListUrl}"
                    else -> "$repoBaseUrl/${indexDto.extensionListUrl}"
                }
                val listResponse = networkService.client.newCall(GET(listUrl)).awaitSuccess()
                listResponse.body.source().decompressIfGzipped().use { listSource ->
                    protoBuf.decodeFromByteArray<ExtensionIndexDto.ExtensionList>(listSource.readByteArray()).extensions
                }
            } else {
                emptyList()
            }

            extensions.toAvailableExtensions(repoBaseUrl).takeIf { it.isNotEmpty() }
        } catch (e: Throwable) {
            logcat(LogPriority.DEBUG, e) { "Failed to get protobuf extensions from $repoBaseUrl" }
            null
        }
    }

    private suspend fun fetchJsonExtensions(repoBaseUrl: String): List<Extension.Available> {
        val response = try {
            networkService.client
                .newCall(GET("$repoBaseUrl/index.min.json"))
                .awaitSuccess()
        } catch (e: Throwable) {
            networkService.client
                .newCall(GET("$repoBaseUrl/index.json"))
                .awaitSuccess()
        }

        return with(json) {
            response
                .parseAs<List<ExtensionJsonObject>>()
                .toExtensions(repoBaseUrl)
        }
    }

    suspend fun checkForUpdates(
        context: Context,
        fromAvailableExtensionList: Boolean = false,
    ): List<Extension.Installed>? {
        // Limit checks to once a day at most
        if (!fromAvailableExtensionList &&
            Instant.now().toEpochMilli() < lastExtCheck.get() + 1.days.inWholeMilliseconds
        ) {
            return null
        }

        // Update extension repo details
        updateExtensionRepo.awaitAll()

        val extensions = if (fromAvailableExtensionList) {
            extensionManager.availableExtensionsFlow.value
        } else {
            findExtensions().also { lastExtCheck.set(Instant.now().toEpochMilli()) }
        }

        // SY -->
        val blacklistEnabled = sourcePreferences.enableSourceBlacklist().get()
        // SY <--

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }
            // SY -->
            .filterNot { it.isBlacklisted(blacklistEnabled) }
        // SY <--

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue
            val hasUpdatedVer = availableExt.versionCode > installedExt.versionCode
            val hasUpdatedLib = availableExt.libVersion > installedExt.libVersion
            val hasUpdate = hasUpdatedVer || hasUpdatedLib
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        if (extensionsWithUpdate.isNotEmpty()) {
            ExtensionUpdateNotifier(context).promptUpdates(extensionsWithUpdate.map { it.name })
        }

        return extensionsWithUpdate
    }

    private fun List<ExtensionIndexDto.Extension>.toAvailableExtensions(repoUrl: String): List<Extension.Available> {
        return this
            .mapNotNull { ext ->
                val libVersion = ext.extensionLib.toDoubleOrNull()
                    ?: ext.versionName.substringBeforeLast('.').toDoubleOrNull()
                    ?: return@mapNotNull null
                if (libVersion < ExtensionLoader.LIB_VERSION_MIN || libVersion > ExtensionLoader.LIB_VERSION_MAX) {
                    return@mapNotNull null
                }
                val resources = ext.resources ?: return@mapNotNull null
                val apkName = resources.apkUrl.substringAfterLast('/').ifEmpty { "${ext.packageName}-v${ext.versionName}.apk" }

                val languages = ext.sources.map { it.language }.toSet()
                val apkUrl = when {
                    resources.apkUrl.startsWith("http://") || resources.apkUrl.startsWith("https://") -> resources.apkUrl
                    resources.apkUrl.startsWith("/") -> "$repoUrl${resources.apkUrl}"
                    resources.apkUrl.isNotEmpty() -> "$repoUrl/${resources.apkUrl}"
                    else -> "$repoUrl/apk/$apkName"
                }
                val iconUrl = when {
                    resources.iconUrl.startsWith("http://") || resources.iconUrl.startsWith("https://") -> resources.iconUrl
                    resources.iconUrl.startsWith("/") -> "$repoUrl${resources.iconUrl}"
                    resources.iconUrl.isNotEmpty() -> "$repoUrl/${resources.iconUrl}"
                    else -> "$repoUrl/icon/${ext.packageName}.png"
                }

                val label = ext.name
                val extName = label
                    .substringAfter("ShinKu: ")
                    .substringAfter("Mihon: ")
                    .substringAfter("Tachiyomi: ")
                    .substringAfter("tachiyomi:")
                    .substringAfter("extension_app")
                    .trim()

                Extension.Available(
                    name = extName,
                    pkgName = ext.packageName,
                    versionName = ext.versionName,
                    versionCode = ext.versionCode,
                    libVersion = libVersion,
                    lang = if (languages.size == 1) languages.first() else "all",
                    isNsfw = ext.contentWarning == ExtensionIndexDto.ContentWarning.NSFW ||
                        ext.contentWarning == ExtensionIndexDto.ContentWarning.MIXED,
                    sources = ext.sources.map { source ->
                        Extension.Available.Source(
                            id = source.id,
                            lang = source.language,
                            name = source.name,
                            baseUrl = source.homeUrl,
                        )
                    },
                    apkName = apkName,
                    iconUrl = iconUrl,
                    repoUrl = repoUrl,
                    apkUrl = apkUrl,
                )
            }
    }

    private fun List<ExtensionJsonObject>.toExtensions(repoUrl: String): List<Extension.Available> {
        return this
            .filter {
                val libVersion = it.extractLibVersion()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map {
                val label = it.name
                val extName = label
                    .substringAfter("ShinKu: ")
                    .substringAfter("Mihon: ")
                    .substringAfter("Tachiyomi: ")
                    .substringAfter("tachiyomi:")
                    .substringAfter("extension_app")
                    .trim()
                Extension.Available(
                    name = extName,
                    pkgName = it.pkg,
                    versionName = it.version,
                    versionCode = it.code,
                    libVersion = it.extractLibVersion(),
                    lang = it.lang,
                    isNsfw = it.nsfw == 1,
                    sources = it.sources?.map(extensionSourceMapper).orEmpty(),
                    apkName = it.apk,
                    iconUrl = "$repoUrl/icon/${it.pkg}.png",
                    repoUrl = repoUrl,
                )
            }
    }

    fun getApkUrl(extension: Extension.Available): String {
        return extension.apkUrl?.takeIf { it.isNotEmpty() } ?: "${extension.repoUrl}/apk/${extension.apkName}"
    }

    private fun ExtensionJsonObject.extractLibVersion(): Double {
        return version.substringBeforeLast('.').toDoubleOrNull() ?: 0.0
    }

    private fun BufferedSource.decompressIfGzipped(): BufferedSource {
        val isGzip = peek().use { peeked ->
            try {
                peeked.readShort().toInt() and 0xFFFF == 0x1f8b
            } catch (_: Exception) {
                false
            }
        }
        return if (isGzip) gzip().buffer() else this
    }

    // SY -->
    private fun Extension.isBlacklisted(
        blacklistEnabled: Boolean = sourcePreferences.enableSourceBlacklist().get(),
    ): Boolean {
        return pkgName in BlacklistedSources.BLACKLISTED_EXTENSIONS && blacklistEnabled
    }
    // SY <--
}

@Serializable
private data class ExtensionJsonObject(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int = 0,
    val sources: List<ExtensionSourceJsonObject>? = null,
)

@Serializable
private data class ExtensionSourceJsonObject(
    val id: Long,
    val lang: String,
    val name: String,
    val baseUrl: String = "",
)

private val extensionSourceMapper: (ExtensionSourceJsonObject) -> Extension.Available.Source = {
    Extension.Available.Source(
        id = it.id,
        lang = it.lang,
        name = it.name,
        baseUrl = it.baseUrl,
    )
}
