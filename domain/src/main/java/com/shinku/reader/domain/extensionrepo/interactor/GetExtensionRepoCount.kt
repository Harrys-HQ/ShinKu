package com.shinku.reader.domain.extensionrepo.interactor

import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository

class GetExtensionRepoCount(
    private val repository: ExtensionRepoRepository,
) {
    fun subscribe() = repository.getCount()
}
