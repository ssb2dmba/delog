package `in`.delog.service.ssb


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import `in`.delog.db.repository.IdentRepository
import `in`.delog.service.ssb.SsbService.Companion.TAG
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Syncs the data layer by delegating to the appropriate repository instances with
 * sync functionality.
 */
class SyncWorker (
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher,
    private val ssbService: SsbService,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        appContext.syncForegroundInfo()

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        Log.i(TAG, "do work!")
        val promise = ssbService.synchronize2()
        var returnValue = false
        promise.thenAccept { result ->
            println("Async operation completed with result: $result")
            returnValue = result
        }
        Log.i(TAG, "done work! $returnValue")
        if (returnValue) {
            return@withContext Result.success()
        } else {
            return@withContext Result.retry()
        }

    }


    companion object {
        /**
         * Expedited one time work to sync data on app startup
         */
        fun startUpSyncWork() = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(SyncConstraints)
            .setInputData(SyncWorker::class.delegatedData())
            .build()

        fun periodicSyncWork() = PeriodicWorkRequestBuilder<SyncWorker>(15L, TimeUnit.MINUTES)
        .setConstraints(SyncConstraints)
        .setInputData(SyncWorker::class.delegatedData())
        .build()
    }
}