package com.example.diplom.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dateIso: String,
    val steps: Int,
    val activeMinutes: Int
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val dailyGoal: Int = 8000
)

@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockedAtIso: String?
)

@Entity(tableName = "story_chapter")
data class StoryChapterEntity(
    @PrimaryKey val chapterNumber: Int,
    val title: String,
    val requiredDistanceKm: Double,
    val questSteps: Int,
    val unlocked: Boolean
)

@Entity(tableName = "weekly_challenge")
data class WeeklyChallengeEntity(
    @PrimaryKey val id: Int = 0,
    val weekStartIso: String,
    val targetSteps: Int,
    val progressSteps: Int,
    val completed: Boolean
)
