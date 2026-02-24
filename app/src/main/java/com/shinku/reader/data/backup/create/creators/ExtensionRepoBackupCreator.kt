package com.shinku.reader.data.backup.create.creators

import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.backup.models.backupExtensionReposMapper
import com.shinku.reader.domain.extensionrepo.interactor.GetExtensionRepo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionRepoBackupCreator(
    private val getExtensionRepos: GetExtensionRepo = Injekt.get(),
) {

    suspend operator fun invoke(): List<BackupExtensionRepos> {
        return getExtensionRepos.getAll()
            .map(backupExtensionReposMapper)
    }
}
