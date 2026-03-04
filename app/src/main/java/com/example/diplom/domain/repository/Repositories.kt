package com.example.diplom.domain.repository

import com.example.diplom.domain.model.Achievement
import com.example.diplom.domain.model.DailyStats
import com.example.diplom.domain.model.PlayerProfile
import com.example.diplom.domain.model.StoryChapter
import com.example.diplom.domain.model.WeeklyChallenge
import com.example.diplom.domain.model.AppUserMode
import com.example.diplom.domain.model.Exercise
import com.example.diplom.domain.model.WorkoutExercise
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

interface TrainingRepository {
    fun observeUserMode(): Flow<AppUserMode>
    suspend fun setUserMode(mode: AppUserMode)
    fun observeExerciseBank(): Flow<List<Exercise>>
    suspend fun addExercise(title: String, description: String, defaultReps: Int)
    fun observeTodayWorkout(): Flow<List<WorkoutExercise>>
    suspend fun addExerciseToTodayWorkout(exercise: Exercise)
    suspend fun removeWorkoutItem(id: Long)
    suspend fun exportProgressAsJson(): String
    suspend fun importProgressFromJson(json: String): Result<Unit>
}
