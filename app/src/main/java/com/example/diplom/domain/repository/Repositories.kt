package com.example.diplom.domain.repository

import com.example.diplom.domain.model.Achievement
import com.example.diplom.domain.model.DailyStats
import com.example.diplom.domain.model.PlayerProfile
import com.example.diplom.domain.model.StoryChapter
import com.example.diplom.domain.model.WeeklyChallenge
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeToday(): Flow<DailyStats>
    fun observeRecentDays(limit: Int = 7): Flow<List<DailyStats>>
    fun observeDailyGoal(): Flow<Int>
    suspend fun addSteps(steps: Int)
    suspend fun setDailyGoal(steps: Int)
}

interface GamificationRepository {
    fun observeProfile(): Flow<PlayerProfile>
    fun observeAchievements(): Flow<List<Achievement>>
    fun observeChapters(): Flow<List<StoryChapter>>
    fun observeWeeklyChallenge(): Flow<WeeklyChallenge>
    suspend fun seedIfEmpty()
    suspend fun recalculate()
}

interface LeaderboardRepository {
    suspend fun topPlayers(): List<String>
}

interface SyncRepository {
    suspend fun syncNow(): Boolean
}
