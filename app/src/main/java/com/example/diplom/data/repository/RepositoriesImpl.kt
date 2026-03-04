package com.example.diplom.data.repository

import com.example.diplom.core.DateUtils
import com.example.diplom.data.local.DailyActivityEntity
import com.example.diplom.data.local.DiplomDao
import com.example.diplom.data.local.UserSettingsEntity
import com.example.diplom.domain.GamificationEngine
import com.example.diplom.domain.model.Achievement
import com.example.diplom.domain.model.AppUserMode
import com.example.diplom.domain.model.DailyStats
import com.example.diplom.domain.model.Exercise
import com.example.diplom.domain.model.PlayerProfile
import com.example.diplom.domain.model.StoryChapter
import com.example.diplom.domain.model.WeeklyChallenge
import com.example.diplom.domain.model.WorkoutExercise
import com.example.diplom.domain.repository.ActivityRepository
import com.example.diplom.domain.repository.GamificationRepository
import com.example.diplom.domain.repository.LeaderboardRepository
import com.example.diplom.domain.repository.SyncRepository
import com.example.diplom.domain.repository.TrainingRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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

class TrainingRepositoryImpl(
    private val dao: DiplomDao
) : TrainingRepository {
    override fun observeUserMode(): Flow<AppUserMode> =
        dao.observeSettings().map { settings ->
            runCatching { AppUserMode.valueOf(settings?.mode ?: AppUserMode.STUDENT.name) }
                .getOrDefault(AppUserMode.STUDENT)
        }

    override suspend fun setUserMode(mode: AppUserMode) {
        val current = dao.getSettings() ?: UserSettingsEntity()
        dao.upsertSettings(current.copy(mode = mode.name))
    }

    override fun observeExerciseBank(): Flow<List<Exercise>> =
        dao.observeExercises().map { items ->
            items.map { Exercise(it.id, it.title, it.description, it.defaultReps) }
        }

    override suspend fun addExercise(title: String, description: String, defaultReps: Int) {
        val safeTitle = title.trim()
        if (safeTitle.isEmpty()) return
        dao.upsertExercise(
            com.example.diplom.data.local.ExerciseEntity(
                title = safeTitle,
                description = description.trim(),
                defaultReps = defaultReps.coerceIn(1, 500)
            )
        )
    }

    override fun observeTodayWorkout(): Flow<List<WorkoutExercise>> {
        val todayIso = DateUtils.todayIso()
        return dao.observePlannedWorkout(todayIso).map { items ->
            items.map {
                WorkoutExercise(
                    id = it.id,
                    dateIso = it.dateIso,
                    exerciseId = it.exerciseId,
                    title = it.title,
                    plannedReps = it.plannedReps,
                    sortOrder = it.sortOrder
                )
            }
        }
    }

    override suspend fun addExerciseToTodayWorkout(exercise: Exercise) {
        val todayIso = DateUtils.todayIso()
        val nextOrder = dao.getPlannedWorkoutAll()
            .filter { it.dateIso == todayIso }
            .maxOfOrNull { it.sortOrder + 1 } ?: 0
        dao.upsertPlannedWorkout(
            com.example.diplom.data.local.PlannedWorkoutEntity(
                dateIso = todayIso,
                exerciseId = exercise.id,
                title = exercise.title,
                plannedReps = exercise.defaultReps,
                sortOrder = nextOrder
            )
        )
    }

    override suspend fun removeWorkoutItem(id: Long) {
        dao.deletePlannedWorkoutItem(id)
    }

    override suspend fun exportProgressAsJson(): String {
        val root = JSONObject()
        val settings = dao.getSettings() ?: UserSettingsEntity()
        root.put(
            "settings",
            JSONObject()
                .put("dailyGoal", settings.dailyGoal)
                .put("mode", settings.mode)
        )
        root.put(
            "dailyActivity",
            JSONArray().apply {
                dao.getAllActivity().forEach {
                    put(
                        JSONObject()
                            .put("dateIso", it.dateIso)
                            .put("steps", it.steps)
                            .put("activeMinutes", it.activeMinutes)
                    )
                }
            }
        )
        root.put(
            "achievements",
            JSONArray().apply {
                dao.getAchievements().forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("title", it.title)
                            .put("description", it.description)
                            .put("unlocked", it.unlocked)
                            .put("unlockedAtIso", it.unlockedAtIso)
                    )
                }
            }
        )
        root.put(
            "chapters",
            JSONArray().apply {
                dao.getChapters().forEach {
                    put(
                        JSONObject()
                            .put("chapterNumber", it.chapterNumber)
                            .put("title", it.title)
                            .put("requiredDistanceKm", it.requiredDistanceKm)
                            .put("questSteps", it.questSteps)
                            .put("unlocked", it.unlocked)
                    )
                }
            }
        )
        dao.getWeeklyChallenge()?.let { weekly ->
            root.put(
                "weeklyChallenge",
                JSONObject()
                    .put("weekStartIso", weekly.weekStartIso)
                    .put("targetSteps", weekly.targetSteps)
                    .put("progressSteps", weekly.progressSteps)
                    .put("completed", weekly.completed)
            )
        }
        root.put(
            "exerciseBank",
            JSONArray().apply {
                dao.getExercises().forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("title", it.title)
                            .put("description", it.description)
                            .put("defaultReps", it.defaultReps)
                    )
                }
            }
        )
        root.put(
            "plannedWorkout",
            JSONArray().apply {
                dao.getPlannedWorkoutAll().forEach {
                    put(
                        JSONObject()
                            .put("id", it.id)
                            .put("dateIso", it.dateIso)
                            .put("exerciseId", it.exerciseId)
                            .put("title", it.title)
                            .put("plannedReps", it.plannedReps)
                            .put("sortOrder", it.sortOrder)
                    )
                }
            }
        )
        return root.toString(2)
    }

    override suspend fun importProgressFromJson(json: String): Result<Unit> = runCatching {
        val root = JSONObject(json)

        dao.clearPlannedWorkout()
        dao.clearExercises()
        dao.clearWeeklyChallenge()
        dao.clearAchievements()
        dao.clearChapters()
        dao.clearDailyActivity()

        val settingsObj = root.optJSONObject("settings")
        val goal = settingsObj?.optInt("dailyGoal", 8000) ?: 8000
        val mode = settingsObj?.optString("mode", AppUserMode.STUDENT.name) ?: AppUserMode.STUDENT.name
        dao.upsertSettings(UserSettingsEntity(id = 0, dailyGoal = goal, mode = mode))

        val activityList = mutableListOf<DailyActivityEntity>()
        val activityArray = root.optJSONArray("dailyActivity") ?: JSONArray()
        repeat(activityArray.length()) { index ->
            val item = activityArray.getJSONObject(index)
            activityList += DailyActivityEntity(
                dateIso = item.getString("dateIso"),
                steps = item.optInt("steps", 0),
                activeMinutes = item.optInt("activeMinutes", 0)
            )
        }
        activityList.forEach { dao.upsertDailyActivity(it) }

        val achievementsList = mutableListOf<com.example.diplom.data.local.AchievementEntity>()
        val achievementsArray = root.optJSONArray("achievements") ?: JSONArray()
        repeat(achievementsArray.length()) { index ->
            val item = achievementsArray.getJSONObject(index)
            achievementsList += com.example.diplom.data.local.AchievementEntity(
                id = item.getString("id"),
                title = item.getString("title"),
                description = item.getString("description"),
                unlocked = item.optBoolean("unlocked", false),
                unlockedAtIso = if (item.has("unlockedAtIso") && !item.isNull("unlockedAtIso")) {
                    item.getString("unlockedAtIso")
                } else {
                    null
                }
            )
        }
        if (achievementsList.isNotEmpty()) {
            dao.upsertAchievements(achievementsList)
        }

        val chaptersList = mutableListOf<com.example.diplom.data.local.StoryChapterEntity>()
        val chaptersArray = root.optJSONArray("chapters") ?: JSONArray()
        repeat(chaptersArray.length()) { index ->
            val item = chaptersArray.getJSONObject(index)
            chaptersList += com.example.diplom.data.local.StoryChapterEntity(
                chapterNumber = item.getInt("chapterNumber"),
                title = item.getString("title"),
                requiredDistanceKm = item.optDouble("requiredDistanceKm", 0.0),
                questSteps = item.optInt("questSteps", 0),
                unlocked = item.optBoolean("unlocked", false)
            )
        }
        if (chaptersList.isNotEmpty()) {
            dao.upsertChapters(chaptersList)
        }

        root.optJSONObject("weeklyChallenge")?.let { item ->
            dao.upsertWeeklyChallenge(
                com.example.diplom.data.local.WeeklyChallengeEntity(
                    weekStartIso = item.getString("weekStartIso"),
                    targetSteps = item.optInt("targetSteps", 55_000),
                    progressSteps = item.optInt("progressSteps", 0),
                    completed = item.optBoolean("completed", false)
                )
            )
        }

        val exercisesList = mutableListOf<com.example.diplom.data.local.ExerciseEntity>()
        val exercisesArray = root.optJSONArray("exerciseBank") ?: JSONArray()
        repeat(exercisesArray.length()) { index ->
            val item = exercisesArray.getJSONObject(index)
            exercisesList += com.example.diplom.data.local.ExerciseEntity(
                id = item.optLong("id", 0L),
                title = item.getString("title"),
                description = item.optString("description", ""),
                defaultReps = item.optInt("defaultReps", 10)
            )
        }
        if (exercisesList.isNotEmpty()) {
            dao.upsertExercises(exercisesList)
        }

        val plannedList = mutableListOf<com.example.diplom.data.local.PlannedWorkoutEntity>()
        val plannedArray = root.optJSONArray("plannedWorkout") ?: JSONArray()
        repeat(plannedArray.length()) { index ->
            val item = plannedArray.getJSONObject(index)
            plannedList += com.example.diplom.data.local.PlannedWorkoutEntity(
                id = item.optLong("id", 0L),
                dateIso = item.getString("dateIso"),
                exerciseId = item.optLong("exerciseId", 0L),
                title = item.getString("title"),
                plannedReps = item.optInt("plannedReps", 10),
                sortOrder = item.optInt("sortOrder", index)
            )
        }
        if (plannedList.isNotEmpty()) {
            dao.upsertPlannedWorkouts(plannedList)
        }
    }
}

private fun DailyActivityEntity.toDailyStats(): DailyStats = DailyStats(
    dateIso = dateIso,
    steps = steps,
    activeMinutes = activeMinutes,
    distanceKm = GamificationEngine.toDistanceKm(steps)
)
