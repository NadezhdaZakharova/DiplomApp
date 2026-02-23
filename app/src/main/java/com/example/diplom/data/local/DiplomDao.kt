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
}
