package com.shinku.reader.domain.extensionrepo.interactor

import kotlinx.coroutines.flow.Flow
import com.shinku.reader.domain.extensionrepo.model.ExtensionRepo
import com.shinku.reader.domain.extensionrepo.repository.ExtensionRepoRepository

class GetExtensionRepo(
    private val repository: ExtensionRepoRepository,
) {
    fun subscribeAll(): Flow<List<ExtensionRepo>> = repository.subscribeAll()

    suspend fun getAll(): List<ExtensionRepo> = repository.getAll()
}
