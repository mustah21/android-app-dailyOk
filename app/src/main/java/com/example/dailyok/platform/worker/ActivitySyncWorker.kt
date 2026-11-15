package com.example.dailyok.platform.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailyok.domain.useCase.SyncLastActivity

class ActivitySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncLastActivity: SyncLastActivity,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            syncLastActivity()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}