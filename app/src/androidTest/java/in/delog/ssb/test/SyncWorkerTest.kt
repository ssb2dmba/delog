package `in`.delog.ssb.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import androidx.work.workDataOf
import `in`.delog.db.repository.IdentRepository
import `in`.delog.service.ssb.SsbService
import `in`.delog.service.ssb.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.startKoin
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MyWorkerFactory(private val ssbService: SsbService) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        return SyncWorker(appContext, workerParameters, Dispatchers.IO, ssbService)
    }
}

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private lateinit var context: Context
    private lateinit var executor: Executor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
    }
/*
    @Test
    fun tesSyncWorker() {
        startKoin { }
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(MyWorkerFactory(SsbService(IdentRepository(context)
            .setExecutor(executor)
            .setInputData(
                workDataOf("data" to false)
            )
            .build()
        runBlocking {
            val result = worker.doWork()
            Assert.assertEquals(result, androidx.work.ListenableWorker.Result.success())
        }
    }
    */

}