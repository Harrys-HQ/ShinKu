package com.shinku.reader.data.backup.create.creators

import com.shinku.reader.data.backup.models.BackupManga
import com.shinku.reader.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.Source
import com.shinku.reader.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesBackupCreator(
    private val sourceManager: SourceManager = Injekt.get(),
) {

    operator fun invoke(mangas: List<BackupManga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map(BackupManga::source)
            .distinct()
            .map(sourceManager::getOrStub)
            .map { it.toBackupSource() }
            .toList()
    }
}

private fun Source.toBackupSource() =
    BackupSource(
        name = this.name,
        sourceId = this.id,
    )
