package com.shinku.reader.domain.extensionrepo.interactor

import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository

class ReplaceExtensionRepo(
    private val repository: ExtensionRepoRepository,
) {
    suspend fun await(repo: ExtensionRepo) {
        repository.replaceRepo(repo)
    }
}
