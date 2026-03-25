package com.example.diplom.app //test

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.diplom.data.local.DiplomDatabase
import com.example.diplom.data.repository.ActivityRepositoryImpl
import com.example.diplom.data.repository.GamificationRepositoryImpl
import com.example.diplom.data.repository.OfflineLeaderboardRepository
import com.example.diplom.data.repository.OfflineSyncRepository
import com.example.diplom.data.repository.TrainingRepositoryImpl
import com.example.diplom.domain.repository.ActivityRepository
import com.example.diplom.domain.repository.GamificationRepository
import com.example.diplom.domain.repository.LeaderboardRepository
import com.example.diplom.domain.repository.SyncRepository
import com.example.diplom.domain.repository.TrainingRepository
import com.example.diplom.worker.DailyRecalculateWorker
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val db = DiplomDatabase.getInstance(context)

    val activityRepository: ActivityRepository = ActivityRepositoryImpl(db.dao())
    val gamificationRepository: GamificationRepository = GamificationRepositoryImpl(db.dao())
    val leaderboardRepository: LeaderboardRepository = OfflineLeaderboardRepository()
    val syncRepository: SyncRepository = OfflineSyncRepository()
    val trainingRepository: TrainingRepository = TrainingRepositoryImpl(db.dao())

    fun scheduleDailyRecalculation(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyRecalculateWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyRecalculateWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
