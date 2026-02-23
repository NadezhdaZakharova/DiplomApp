package com.example.diplom.data.repository

import com.example.diplom.core.DateUtils
import com.example.diplom.data.local.DailyActivityEntity
import com.example.diplom.data.local.DiplomDao
import com.example.diplom.data.local.UserSettingsEntity
import com.example.diplom.domain.GamificationEngine
import com.example.diplom.domain.model.Achievement
import com.example.diplom.domain.model.DailyStats
import com.example.diplom.domain.model.PlayerProfile
import com.example.diplom.domain.model.StoryChapter
import com.example.diplom.domain.model.WeeklyChallenge
import com.example.diplom.domain.repository.ActivityRepository
import com.example.diplom.domain.repository.GamificationRepository
import com.example.diplom.domain.repository.LeaderboardRepository
import com.example.diplom.domain.repository.SyncRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl(
    private val dao: DiplomDao
) : ActivityRepository {
    override fun observeToday(): Flow<DailyStats> {
        val todayIso = DateUtils.todayIso()
        return dao.observeDailyActivity(todayIso).map { entity ->
            entity?.toDailyStats() ?: DailyStats(
                dateIso = todayIso,
                steps = 0,
                activeMinutes = 0,
                distanceKm = 0.0
            )
        }
    }

    override fun observeRecentDays(limit: Int): Flow<List<DailyStats>> =
        dao.observeRecentActivity(limit).map { list ->
            list.map { it.toDailyStats() }
        }

    override fun observeDailyGoal(): Flow<Int> = dao.observeSettings().map { it?.dailyGoal ?: 8000 }

    override suspend fun addSteps(steps: Int) {
        val todayIso = DateUtils.todayIso()
        val existing = dao.getDailyActivity(todayIso)
        val newSteps = (existing?.steps ?: 0) + steps
        val activeMinutes = (newSteps / 100).coerceAtLeast(0)
        dao.upsertDailyActivity(
            DailyActivityEntity(
                dateIso = todayIso,
                steps = newSteps,
                activeMinutes = activeMinutes
            )
        )
    }

    override suspend fun setDailyGoal(steps: Int) {
        val safeGoal = steps.coerceIn(2000, 30000)
        dao.upsertSettings(UserSettingsEntity(id = 0, dailyGoal = safeGoal))
    }
}

class GamificationRepositoryImpl(
    private val dao: DiplomDao
) : GamificationRepository {
    override fun observeProfile(): Flow<PlayerProfile> =
        combine(dao.observeSettings(), dao.observeRecentActivity(365)) { settings, activity ->
            val dailyGoal = settings?.dailyGoal ?: 8000
            val progress = GamificationEngine.calculatePlayerProgress(activity, dailyGoal)
            PlayerProfile(
                xp = progress.xp,
                level = progress.level,
                streakDays = progress.streakDays,
                bestStreakDays = progress.bestStreakDays
            )
        }

    override fun observeAchievements(): Flow<List<Achievement>> =
        dao.observeAchievements().map { list ->
            list.map {
                Achievement(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    unlocked = it.unlocked,
                    unlockedAtIso = it.unlockedAtIso
                )
            }
        }

    override fun observeChapters(): Flow<List<StoryChapter>> =
        dao.observeChapters().map { list ->
            list.map {
                StoryChapter(
                    chapterNumber = it.chapterNumber,
                    title = it.title,
                    requiredDistanceKm = it.requiredDistanceKm,
                    questSteps = it.questSteps,
                    unlocked = it.unlocked
                )
            }
        }

    override fun observeWeeklyChallenge(): Flow<WeeklyChallenge> =
        dao.observeWeeklyChallenge().map { item ->
            val fallback = item ?: GamificationEngine.buildWeeklyChallenge(
                existing = null,
                weekStartIso = DateUtils.isoWeekStart(),
                weekSteps = 0
            )
            WeeklyChallenge(
                weekStartIso = fallback.weekStartIso,
                targetSteps = fallback.targetSteps,
                progressSteps = fallback.progressSteps,
                completed = fallback.completed
            )
        }

    override suspend fun seedIfEmpty() {
        if (dao.getSettings() == null) {
            dao.upsertSettings(UserSettingsEntity())
        }
        if (dao.getAchievements().isEmpty()) {
            dao.upsertAchievements(GamificationEngine.initialAchievements())
        }
        if (dao.getChapters().isEmpty()) {
            dao.upsertChapters(GamificationEngine.initialChapters())
        }
        if (dao.getWeeklyChallenge() == null) {
            dao.upsertWeeklyChallenge(
                GamificationEngine.buildWeeklyChallenge(
                    existing = null,
                    weekStartIso = DateUtils.isoWeekStart(),
                    weekSteps = 0
                )
            )
        }
    }

    override suspend fun recalculate() {
        val allActivity = dao.getAllActivity()
        val settings = dao.getSettings() ?: UserSettingsEntity()
        val dailyGoal = settings.dailyGoal
        val todayIso = DateUtils.todayIso()
        val todaySteps = allActivity.firstOrNull { it.dateIso == todayIso }?.steps ?: 0
        val totalDistance = GamificationEngine.toDistanceKm(allActivity.sumOf { it.steps })
        val chapters = dao.getChapters()
        val updatedChapters = GamificationEngine.updateChapters(chapters, totalDistance, todaySteps)
        dao.upsertChapters(updatedChapters)

        val achievements = dao.getAchievements()
        val updatedAchievements = GamificationEngine.updateAchievements(
            existing = achievements,
            activity = allActivity,
            dailyGoal = dailyGoal,
            chapters = updatedChapters,
            todayIso = todayIso
        )
        dao.upsertAchievements(updatedAchievements)

        val weekStart = DateUtils.isoWeekStart()
        val weekStartDate = LocalDate.parse(weekStart)
        val weekSteps = allActivity.filter { LocalDate.parse(it.dateIso) >= weekStartDate }.sumOf { it.steps }
        val weekly = GamificationEngine.buildWeeklyChallenge(
            existing = dao.getWeeklyChallenge(),
            weekStartIso = weekStart,
            weekSteps = weekSteps
        )
        dao.upsertWeeklyChallenge(weekly)
    }
}

class OfflineLeaderboardRepository : LeaderboardRepository {
    override suspend fun topPlayers(): List<String> =
        listOf("You", "RangerBot", "MageWalker", "KnightSteps")
}

class OfflineSyncRepository : SyncRepository {
    override suspend fun syncNow(): Boolean = true
}

private fun DailyActivityEntity.toDailyStats(): DailyStats = DailyStats(
    dateIso = dateIso,
    steps = steps,
    activeMinutes = activeMinutes,
    distanceKm = GamificationEngine.toDistanceKm(steps)
)
