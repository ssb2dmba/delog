package `in`.delog.service.ssb

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate

/**
 * [SyncStatusMonitor] backed by [WorkInfo] from [WorkManager]
 */
class WorkManagerSyncStatusMonitor(
    context: Context
) : SyncStatusMonitor {
    override val isSyncing: Flow<Boolean> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(SyncWorkName)
            .map { it.anyRunning }
            .asFlow()
            .conflate()
}

private val List<WorkInfo>.anyRunning get() = any { it.state == State.RUNNING }