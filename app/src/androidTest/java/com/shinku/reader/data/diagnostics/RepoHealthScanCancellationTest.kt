package com.shinku.reader.data.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import com.shinku.reader.data.library.RepoHealthScanJob
import com.shinku.reader.domain.source.model.DiagnosticPhase
import com.shinku.reader.util.system.workManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RepoHealthScanCancellationTest {
    @Test
    fun cancellingTargetedScanKeepsThePeriodicRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".debug"))
        val workManager = context.workManager
        RepoHealthScanJob.setupTask(context)
        val periodic = workManager.getWorkInfosForUniqueWork("RepoHealthScan")
            .get(10, TimeUnit.SECONDS)
            .single { !it.state.isFinished }

        val request = OneTimeWorkRequestBuilder<RepoHealthScanJob>()
            .setInitialDelay(1, TimeUnit.DAYS)
            .addTag("RepoHealthScan")
            .build()
        val store = Injekt.get<DiagnosticsStore>()
        val correlationId = store.newCorrelationId()
        workManager.enqueue(request).result.get(10, TimeUnit.SECONDS)
        try {
            assertTrue(store.startScan(correlationId, totalCount = 0, workId = request.id))
            RepoHealthScanJob.stop(context)
            assertEquals(
                WorkInfo.State.CANCELLED,
                workManager.getWorkInfoById(request.id).get(10, TimeUnit.SECONDS)?.state,
            )
            assertEquals(
                periodic.id,
                workManager.getWorkInfosForUniqueWork("RepoHealthScan")
                    .get(10, TimeUnit.SECONDS)
                    .single { !it.state.isFinished }
                    .id,
            )
        } finally {
            store.finishScan(correlationId, DiagnosticPhase.CANCELLED)
            workManager.cancelWorkById(request.id).result.get(10, TimeUnit.SECONDS)
        }
    }
}
