package com.example.diplom.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiplomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyActivity(entity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activity WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getDailyActivity(dateIso: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity WHERE dateIso = :dateIso LIMIT 1")
    fun observeDailyActivity(dateIso: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity ORDER BY dateIso DESC LIMIT :limit")
    fun observeRecentActivity(limit: Int): Flow<List<DailyActivityEntity>>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM daily_activity")
    suspend fun getTotalSteps(): Int

    @Query("SELECT * FROM daily_activity")
    suspend fun getAllActivity(): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(entity: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE id = 0 LIMIT 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0 LIMIT 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(items: List<AchievementEntity>)

    @Query("SELECT * FROM achievement ORDER BY unlocked DESC, id ASC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievement")
    suspend fun getAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChapters(items: List<StoryChapterEntity>)

    @Query("SELECT * FROM story_chapter ORDER BY chapterNumber ASC")
    fun observeChapters(): Flow<List<StoryChapterEntity>>

    @Query("SELECT * FROM story_chapter ORDER BY chapterNumber ASC")
    suspend fun getChapters(): List<StoryChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyChallenge(entity: WeeklyChallengeEntity)

    @Query("SELECT * FROM weekly_challenge WHERE id = 0 LIMIT 1")
    fun observeWeeklyChallenge(): Flow<WeeklyChallengeEntity?>

    @Query("SELECT * FROM weekly_challenge WHERE id = 0 LIMIT 1")
    suspend fun getWeeklyChallenge(): WeeklyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(items: List<ExerciseEntity>)

    @Query("SELECT * FROM exercise_bank ORDER BY id ASC")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_bank ORDER BY id ASC")
    suspend fun getExercises(): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlannedWorkout(entity: PlannedWorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlannedWorkouts(items: List<PlannedWorkoutEntity>)

    @Query("SELECT * FROM planned_workout WHERE dateIso = :dateIso ORDER BY sortOrder ASC, id ASC")
    fun observePlannedWorkout(dateIso: String): Flow<List<PlannedWorkoutEntity>>

    @Query("SELECT * FROM planned_workout ORDER BY dateIso DESC, sortOrder ASC")
    suspend fun getPlannedWorkoutAll(): List<PlannedWorkoutEntity>

    @Query("DELETE FROM planned_workout WHERE id = :id")
    suspend fun deletePlannedWorkoutItem(id: Long)

    @Query("DELETE FROM planned_workout")
    suspend fun clearPlannedWorkout()

    @Query("DELETE FROM exercise_bank")
    suspend fun clearExercises()

    @Query("DELETE FROM daily_activity")
    suspend fun clearDailyActivity()

    @Query("DELETE FROM achievement")
    suspend fun clearAchievements()

    @Query("DELETE FROM story_chapter")
    suspend fun clearChapters()

    @Query("DELETE FROM weekly_challenge")
    suspend fun clearWeeklyChallenge()
}
