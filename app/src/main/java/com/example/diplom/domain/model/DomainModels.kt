package com.example.diplom.domain.model

data class DailyStats(
    val dateIso: String,
    val steps: Int,
    val activeMinutes: Int,
    val distanceKm: Double
)

data class PlayerProfile(
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val bestStreakDays: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val unlockedAtIso: String?
)

data class StoryChapter(
    val chapterNumber: Int,
    val title: String,
    val requiredDistanceKm: Double,
    val questSteps: Int,
    val unlocked: Boolean
)

data class WeeklyChallenge(
    val weekStartIso: String,
    val targetSteps: Int,
    val progressSteps: Int,
    val completed: Boolean
)
