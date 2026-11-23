package com.shinku.reader.data.library

import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibraryUpdateJobTest {
    @Test
    fun `manual refresh stays active through queued and running work and clears on every terminal state`() = runTest {
        val blocked = workInfo(WorkInfo.State.BLOCKED)
        val enqueued = workInfo(WorkInfo.State.ENQUEUED)
        val running = workInfo(WorkInfo.State.RUNNING)
        val succeeded = workInfo(WorkInfo.State.SUCCEEDED)
        val failed = workInfo(WorkInfo.State.FAILED)
        val cancelled = workInfo(WorkInfo.State.CANCELLED)
        val workManager = mockk<WorkManager>()
        every { workManager.getWorkInfosByTagFlow("LibraryUpdate-manual") } returns flowOf(
            emptyList(),
            listOf(blocked),
            listOf(enqueued),
            listOf(running),
            listOf(succeeded),
            listOf(succeeded, enqueued),
            listOf(succeeded, failed),
            listOf(succeeded, failed, running),
            listOf(succeeded, failed, cancelled),
        )

        assertEquals(
            listOf(false, true, false, true, false, true, false),
            LibraryUpdateJob.observeManualUpdate(workManager).toList(),
        )
    }

    private fun workInfo(status: WorkInfo.State): WorkInfo = mockk {
        every { state } returns status
    }
}
