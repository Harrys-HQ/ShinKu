package com.shinku.reader.domain.source.interactor

import com.shinku.reader.domain.base.BasePreferences
import com.shinku.reader.domain.source.service.SourcePreferences
import com.shinku.reader.extension.ExtensionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetIncognitoState(
    private val basePreferences: BasePreferences,
    private val sourcePreferences: SourcePreferences,
    private val extensionManager: ExtensionManager,
) {
    fun await(sourceId: Long?): Boolean {
        if (basePreferences.incognitoMode().get()) return true
        if (sourceId == null) return false
        val extensionPackage = extensionManager.getExtensionPackage(sourceId) ?: return false

        return extensionPackage in sourcePreferences.incognitoExtensions().get()
    }

    fun subscribe(sourceId: Long?): Flow<Boolean> {
        if (sourceId == null) return basePreferences.incognitoMode().changes()

        return combine(
            basePreferences.incognitoMode().changes(),
            sourcePreferences.incognitoExtensions().changes(),
            extensionManager.getExtensionPackageAsFlow(sourceId),
        ) { incognito, incognitoExtensions, extensionPackage ->
            incognito || (extensionPackage in incognitoExtensions)
        }
            .distinctUntilChanged()
    }
}
