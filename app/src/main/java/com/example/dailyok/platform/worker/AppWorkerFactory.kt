package com.example.dailyok.platform.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.dailyok.domain.useCase.SyncLastActivity

class AppWorkerFactory(
    private val syncLastActivity: SyncLastActivity,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == ActivitySyncWorker::class.java.name)
            ActivitySyncWorker(appContext, workerParameters, syncLastActivity)
        else null
}