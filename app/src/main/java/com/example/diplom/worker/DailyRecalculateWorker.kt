package com.example.diplom.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.diplom.data.local.DiplomDatabase
import com.example.diplom.data.repository.GamificationRepositoryImpl

class DailyRecalculateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return runCatching {
            val dao = DiplomDatabase.getInstance(applicationContext).dao()
            val repository = GamificationRepositoryImpl(dao)
            repository.seedIfEmpty()
            repository.recalculate()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_game_recalculate"
    }
}
