package com.shinku.reader.domain.extensionrepo.interactor

import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository

class DeleteExtensionRepo(
    private val repository: ExtensionRepoRepository,
) {
    suspend fun await(baseUrl: String) {
        repository.deleteRepo(baseUrl)
    }
}
