package com.example.diplom.domain

import com.example.diplom.data.local.AchievementEntity
import com.example.diplom.data.local.DailyActivityEntity
import com.example.diplom.data.local.StoryChapterEntity
import com.example.diplom.data.local.WeeklyChallengeEntity
import java.time.LocalDate

data class PlayerProgress(
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val bestStreakDays: Int
)

object GamificationEngine {
    private const val DAILY_XP_CAP = 120
    private const val GOAL_BONUS_XP = 25
    private const val STREAK_MULTIPLIER_MAX = 1.25

    fun toDistanceKm(steps: Int): Double = (steps * 0.00075).coerceAtLeast(0.0)

    fun calculatePlayerProgress(activity: List<DailyActivityEntity>, dailyGoal: Int): PlayerProgress {
        if (activity.isEmpty()) {
            return PlayerProgress(xp = 0, level = 1, streakDays = 0, bestStreakDays = 0)
        }

        val sorted = activity.sortedBy { it.dateIso }
        var xp = 0
        sorted.forEachIndexed { _, day ->
            val baseXp = (day.steps / 100).coerceAtMost(DAILY_XP_CAP)
            val goalBonus = if (day.steps >= dailyGoal) GOAL_BONUS_XP else 0
            xp += baseXp + goalBonus
        }

        val streak = calculateCurrentStreak(sorted, dailyGoal)
        val bestStreak = calculateBestStreak(sorted, dailyGoal)
        val streakMultiplier = (1.0 + (streak.coerceAtMost(10) * 0.025)).coerceAtMost(STREAK_MULTIPLIER_MAX)
        xp = (xp * streakMultiplier).toInt()

        return PlayerProgress(
            xp = xp,
            level = xpToLevel(xp),
            streakDays = streak,
            bestStreakDays = bestStreak
        )
    }

    fun xpToLevel(xp: Int): Int = (xp / 300) + 1

    fun levelProgressFraction(xp: Int): Float {
        val levelBase = (xp / 300) * 300
        return ((xp - levelBase) / 300f).coerceIn(0f, 1f)
    }

    fun initialAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity("first_steps", "First Steps", "Reach 1,000 steps in a day", false, null),
        AchievementEntity("goal_crusher", "Goal Crusher", "Complete daily goal 3 days", false, null),
        AchievementEntity("trail_runner", "Trail Runner", "Reach 50,000 total steps", false, null),
        AchievementEntity("streak_7", "Streak Keeper", "Maintain a 7-day streak", false, null),
        AchievementEntity("chapter_3", "Chapter Explorer", "Unlock chapter 3", false, null)
    )

    fun initialChapters(): List<StoryChapterEntity> = listOf(
        StoryChapterEntity(1, "Whispering Forest", 0.5, 3000, true),
        StoryChapterEntity(2, "Amber Hills", 2.0, 6000, false),
        StoryChapterEntity(3, "River of Glass", 5.0, 8000, false),
        StoryChapterEntity(4, "Storm Peaks", 9.0, 10000, false),
        StoryChapterEntity(5, "Citadel of Dawn", 14.0, 12000, false)
    )

    fun updateAchievements(
        existing: List<AchievementEntity>,
        activity: List<DailyActivityEntity>,
        dailyGoal: Int,
        chapters: List<StoryChapterEntity>,
        todayIso: String
    ): List<AchievementEntity> {
        val totalSteps = activity.sumOf { it.steps }
        val daysWithGoal = activity.count { it.steps >= dailyGoal }
        val has1000InDay = activity.any { it.steps >= 1000 }
        val streak = calculateCurrentStreak(activity.sortedBy { it.dateIso }, dailyGoal)
        val chapter3Unlocked = chapters.any { it.chapterNumber == 3 && it.unlocked }

        return existing.map { achievement ->
            val unlockedNow = when (achievement.id) {
                "first_steps" -> has1000InDay
                "goal_crusher" -> daysWithGoal >= 3
                "trail_runner" -> totalSteps >= 50_000
                "streak_7" -> streak >= 7
                "chapter_3" -> chapter3Unlocked
                else -> achievement.unlocked
            }
            if (achievement.unlocked || !unlockedNow) {
                achievement
            } else {
                achievement.copy(unlocked = true, unlockedAtIso = todayIso)
            }
        }
    }

    fun updateChapters(
        chapters: List<StoryChapterEntity>,
        totalDistanceKm: Double,
        todaySteps: Int
    ): List<StoryChapterEntity> = chapters.map { chapter ->
        if (chapter.unlocked) {
            chapter
        } else {
            val unlock = totalDistanceKm >= chapter.requiredDistanceKm && todaySteps >= chapter.questSteps
            chapter.copy(unlocked = unlock)
        }
    }

    fun buildWeeklyChallenge(
        existing: WeeklyChallengeEntity?,
        weekStartIso: String,
        weekSteps: Int
    ): WeeklyChallengeEntity {
        val target = 55_000
        return if (existing == null || existing.weekStartIso != weekStartIso) {
            WeeklyChallengeEntity(
                weekStartIso = weekStartIso,
                targetSteps = target,
                progressSteps = weekSteps,
                completed = weekSteps >= target
            )
        } else {
            existing.copy(
                progressSteps = weekSteps,
                completed = weekSteps >= existing.targetSteps
            )
        }
    }

    private fun calculateCurrentStreak(activity: List<DailyActivityEntity>, dailyGoal: Int): Int {
        if (activity.isEmpty()) return 0
        val byDate = activity.associateBy { LocalDate.parse(it.dateIso) }
        var streak = 0
        var cursor = LocalDate.now()
        while (true) {
            val day = byDate[cursor] ?: break
            if (day.steps < dailyGoal) break
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun calculateBestStreak(activity: List<DailyActivityEntity>, dailyGoal: Int): Int {
        var best = 0
        var current = 0
        activity.sortedBy { it.dateIso }.forEach {
            if (it.steps >= dailyGoal) {
                current++
                best = maxOf(best, current)
            } else {
                current = 0
            }
        }
        return best
    }
}
