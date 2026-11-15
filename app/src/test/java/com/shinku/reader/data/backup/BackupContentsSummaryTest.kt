package com.shinku.reader.data.backup

import com.shinku.reader.data.backup.models.Backup
import com.shinku.reader.data.backup.models.BackupCategory
import com.shinku.reader.data.backup.models.BackupChapter
import com.shinku.reader.data.backup.models.BackupHistory
import com.shinku.reader.data.backup.models.BackupManga
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupContentsSummaryTest {
    @Test
    fun previewCountsComeFromTheFileIncludingEmptySections() {
        val backup = Backup(
            backupManga = listOf(
                BackupManga(1L, "/one", chapters = listOf(BackupChapter("/one/1", "One")), history = listOf(BackupHistory("/one/1", 0L))),
                BackupManga(2L, "/two", chapters = listOf(BackupChapter("/two/1", "One"), BackupChapter("/two/2", "Two"))),
            ),
            backupCategories = listOf(BackupCategory(name = "Fixture")),
        )
        assertEquals(BackupContentsSummary(2, 3L, 1, 1L, 0), BackupContentsSummary.from(backup))
        assertEquals(BackupContentsSummary(0, 0L, 0, 0L, 0), BackupContentsSummary.from(Backup(emptyList())))
    }
}
