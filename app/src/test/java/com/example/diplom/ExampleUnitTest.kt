package com.example.diplom

import com.example.diplom.data.local.DailyActivityEntity
import com.example.diplom.data.local.StoryChapterEntity
import com.example.diplom.domain.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun xpAndLevelAreCalculated() {
        val history = listOf(
            DailyActivityEntity("2026-02-20", 9000, 90),
            DailyActivityEntity("2026-02-21", 8500, 85),
            DailyActivityEntity("2026-02-22", 4000, 40)
        )
        val progress = GamificationEngine.calculatePlayerProgress(history, dailyGoal = 8000)
        assertTrue(progress.xp > 0)
        assertTrue(progress.level >= 1)
    }

    @Test
    fun chapterUnlockDependsOnDistanceAndQuest() {
        val chapters = listOf(
            StoryChapterEntity(1, "A", 0.2, 1000, true),
            StoryChapterEntity(2, "B", 2.0, 6000, false)
        )
        val updated = GamificationEngine.updateChapters(
            chapters = chapters,
            totalDistanceKm = 2.5,
            todaySteps = 6200
        )
        assertTrue(updated[1].unlocked)
    }

    @Test
    fun levelProgressFractionWithinRange() {
        val fraction = GamificationEngine.levelProgressFraction(450)
        assertTrue(fraction in 0f..1f)
        assertEquals(2, GamificationEngine.xpToLevel(450))
    }
}