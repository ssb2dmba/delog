package `in`.delog.service.ssb



import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager


object Sync {
    // This method is a workaround to manually initialize the sync process instead of relying on
    // automatic initialization with Androidx Startup. It is called from the app module's
    // Application.onCreate() and should be only done once.
    fun initialize(context: Context) {
        AppInitializer.getInstance(context)
            .initializeComponent(SyncInitializer::class.java)
    }
}

// This name should not be changed otherwise the app may have concurrent sync requests running
internal const val SyncWorkName = "SyncWorkName"

internal const val PeriodicSyncWorkName = "PeriodicSyncWorkName"

/**
 * Registers work to sync the data layer periodically on app startup.
 */
class SyncInitializer : Initializer<Sync> {
    override fun create(context: Context): Sync {
        WorkManager.getInstance(context).apply {
            // Run sync on app startup and ensure only one sync worker runs at any time
            enqueueUniqueWork(
                SyncWorkName,
                ExistingWorkPolicy.REPLACE,
                SyncWorker.startUpSyncWork()
            )
            // Run sync periodically and ensure only one sync worker is scheduled at any time
            enqueueUniquePeriodicWork(
                PeriodicSyncWorkName,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                SyncWorker.periodicSyncWork()
            )
        }

        return Sync
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()
}
