package com.example.dailyok.platform.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dailyok.domain.useCase.SyncLastActivity
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class ActivitySyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncLastActivity: SyncLastActivity,
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val maxAttempts = 3
        return try {
            syncLastActivity()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure()
        } catch (e: IOException) {
            if (runAttemptCount < maxAttempts) Result.retry() else Result.failure()
        } catch (e: Exception) {
            Result.failure()
        }
    }


}