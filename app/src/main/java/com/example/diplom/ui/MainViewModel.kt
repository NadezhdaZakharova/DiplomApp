package com.example.diplom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diplom.domain.GamificationEngine
import com.example.diplom.domain.model.Achievement
import com.example.diplom.domain.model.DailyStats
import com.example.diplom.domain.model.PlayerProfile
import com.example.diplom.domain.model.StoryChapter
import com.example.diplom.domain.model.WeeklyChallenge
import com.example.diplom.domain.repository.ActivityRepository
import com.example.diplom.domain.repository.GamificationRepository
import com.example.diplom.domain.usecase.AddStepsUseCase
import com.example.diplom.domain.usecase.BootstrapGameUseCase
import com.example.diplom.domain.usecase.SetDailyGoalUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val today: DailyStats = DailyStats("", 0, 0, 0.0),
    val recentDays: List<DailyStats> = emptyList(),
    val dailyGoal: Int = 8000,
    val profile: PlayerProfile = PlayerProfile(0, 1, 0, 0),
    val weeklyChallenge: WeeklyChallenge = WeeklyChallenge("", 55000, 0, false),
    val achievements: List<Achievement> = emptyList(),
    val chapters: List<StoryChapter> = emptyList()
) {
    val goalProgressFraction: Float
        get() = (today.steps / dailyGoal.toFloat()).coerceIn(0f, 1f)

    val levelProgressFraction: Float
        get() = GamificationEngine.levelProgressFraction(profile.xp)
}

class MainViewModel(
    activityRepository: ActivityRepository,
    gamificationRepository: GamificationRepository,
    private val addStepsUseCase: AddStepsUseCase,
    private val setDailyGoalUseCase: SetDailyGoalUseCase,
    private val bootstrapGameUseCase: BootstrapGameUseCase
) : ViewModel() {

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

    val uiState: StateFlow<MainUiState> = combine(activityState, gameState) { activity, game ->
        MainUiState(
            today = activity.today,
            recentDays = activity.recent,
            dailyGoal = activity.goal,
            profile = activity.profile,
            weeklyChallenge = game.weekly,
            achievements = game.achievements,
            chapters = game.chapters
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

    companion object {
        fun factory(
            activityRepository: ActivityRepository,
            gamificationRepository: GamificationRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    activityRepository = activityRepository,
                    gamificationRepository = gamificationRepository,
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
