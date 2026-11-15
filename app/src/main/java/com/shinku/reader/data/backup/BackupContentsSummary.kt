package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.Backup

/** Counts from the decoded file, not the installed library or illustrative reference artwork. */
data class BackupContentsSummary(
    val titles: Int,
    val chapters: Long,
    val categories: Int,
    val historyEntries: Long,
    val settings: Int,
) {
    companion object {
        fun from(backup: Backup) = BackupContentsSummary(
            titles = backup.backupManga.size,
            chapters = backup.backupManga.sumOf { it.chapters.size.toLong() },
            categories = backup.backupCategories.size,
            historyEntries = backup.backupManga.sumOf { it.history.size.toLong() },
            settings = backup.backupPreferences.size,
        )
    }
}
