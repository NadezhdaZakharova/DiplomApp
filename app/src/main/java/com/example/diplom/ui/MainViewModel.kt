package com.example.diplom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.diplom.domain.repository.TrainingRepository
import com.example.diplom.domain.usecase.AddStepsUseCase
import com.example.diplom.domain.usecase.BootstrapGameUseCase
import com.example.diplom.domain.usecase.SetDailyGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val today: DailyStats = DailyStats("", 0, 0, 0.0),
    val recentDays: List<DailyStats> = emptyList(),
    val dailyGoal: Int = 8000,
    val profile: PlayerProfile = PlayerProfile(0, 1, 0, 0),
    val weeklyChallenge: WeeklyChallenge = WeeklyChallenge("", 55000, 0, false),
    val achievements: List<Achievement> = emptyList(),
    val chapters: List<StoryChapter> = emptyList(),
    val userMode: AppUserMode = AppUserMode.STUDENT,
    val exerciseBank: List<Exercise> = emptyList(),
    val todayWorkout: List<WorkoutExercise> = emptyList(),
    val exportedJson: String = "",
    val importStatus: String? = null
) {
    val goalProgressFraction: Float
        get() = (today.steps / dailyGoal.toFloat()).coerceIn(0f, 1f)

    val levelProgressFraction: Float
        get() = GamificationEngine.levelProgressFraction(profile.xp)
}

class MainViewModel(
    activityRepository: ActivityRepository,
    gamificationRepository: GamificationRepository,
    private val trainingRepository: TrainingRepository,
    private val addStepsUseCase: AddStepsUseCase,
    private val setDailyGoalUseCase: SetDailyGoalUseCase,
    private val bootstrapGameUseCase: BootstrapGameUseCase
) : ViewModel() {
    private val transferState = MutableStateFlow(TransferState())

    private val activityState = combine(
        activityRepository.observeToday(),
        activityRepository.observeRecentDays(),
        activityRepository.observeDailyGoal(),
        gamificationRepository.observeProfile()
    ) { today, recent, goal, profile ->
        ActivityState(today, recent, goal, profile)
    }

    private val gameState = combine(
        gamificationRepository.observeWeeklyChallenge(),
        gamificationRepository.observeAchievements(),
        gamificationRepository.observeChapters()
    ) { weekly, achievements, chapters ->
        GameState(weekly, achievements, chapters)
    }

    private val trainingState = combine(
        trainingRepository.observeUserMode(),
        trainingRepository.observeExerciseBank(),
        trainingRepository.observeTodayWorkout()
    ) { mode, bank, workout ->
        TrainingState(mode, bank, workout)
    }

    val uiState: StateFlow<MainUiState> = combine(
        activityState,
        gameState,
        trainingState,
        transferState
    ) { activity, game, training, transfer ->
        MainUiState(
            today = activity.today,
            recentDays = activity.recent,
            dailyGoal = activity.goal,
            profile = activity.profile,
            weeklyChallenge = game.weekly,
            achievements = game.achievements,
            chapters = game.chapters,
            userMode = training.mode,
            exerciseBank = training.bank,
            todayWorkout = training.workout,
            exportedJson = transfer.exportedJson,
            importStatus = transfer.importStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        viewModelScope.launch {
            bootstrapGameUseCase()
        }
    }

    fun addSteps(steps: Int) {
        viewModelScope.launch {
            addStepsUseCase(steps)
        }
    }

    fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            setDailyGoalUseCase(goal)
        }
    }

    fun setUserMode(mode: AppUserMode) {
        viewModelScope.launch {
            trainingRepository.setUserMode(mode)
        }
    }

    fun addExercise(title: String, description: String, reps: Int) {
        viewModelScope.launch {
            trainingRepository.addExercise(title, description, reps)
        }
    }

    fun addToWorkout(exercise: Exercise) {
        viewModelScope.launch {
            trainingRepository.addExerciseToTodayWorkout(exercise)
        }
    }

    fun removeWorkoutItem(id: Long) {
        viewModelScope.launch {
            trainingRepository.removeWorkoutItem(id)
        }
    }

    fun exportProgress() {
        viewModelScope.launch {
            val json = trainingRepository.exportProgressAsJson()
            transferState.update { it.copy(exportedJson = json, importStatus = "Экспорт готов") }
        }
    }

    fun importProgress(json: String) {
        viewModelScope.launch {
            val result = trainingRepository.importProgressFromJson(json)
            val message = if (result.isSuccess) {
                "Импорт завершен"
            } else {
                "Ошибка импорта: ${result.exceptionOrNull()?.message ?: "неизвестно"}"
            }
            transferState.update { it.copy(importStatus = message) }
        }
    }

    companion object {
        fun factory(
            activityRepository: ActivityRepository,
            gamificationRepository: GamificationRepository,
            trainingRepository: TrainingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    activityRepository = activityRepository,
                    gamificationRepository = gamificationRepository,
                    trainingRepository = trainingRepository,
                    addStepsUseCase = AddStepsUseCase(activityRepository, gamificationRepository),
                    setDailyGoalUseCase = SetDailyGoalUseCase(activityRepository, gamificationRepository),
                    bootstrapGameUseCase = BootstrapGameUseCase(gamificationRepository)
                ) as T
            }
        }
    }
}

private data class ActivityState(
    val today: DailyStats,
    val recent: List<DailyStats>,
    val goal: Int,
    val profile: PlayerProfile
)

private data class GameState(
    val weekly: WeeklyChallenge,
    val achievements: List<Achievement>,
    val chapters: List<StoryChapter>
)

private data class TrainingState(
    val mode: AppUserMode,
    val bank: List<Exercise>,
    val workout: List<WorkoutExercise>
)

private data class TransferState(
    val exportedJson: String = "",
    val importStatus: String? = null
)
