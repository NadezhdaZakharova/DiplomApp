package com.example.diplom.domain.usecase

import com.example.diplom.domain.repository.ActivityRepository
import com.example.diplom.domain.repository.GamificationRepository

class AddStepsUseCase(
    private val activityRepository: ActivityRepository,
    private val gamificationRepository: GamificationRepository
) {
    suspend operator fun invoke(steps: Int) {
        activityRepository.addSteps(steps)
        gamificationRepository.recalculate()
    }
}

class SetDailyGoalUseCase(
    private val activityRepository: ActivityRepository,
    private val gamificationRepository: GamificationRepository
) {
    suspend operator fun invoke(goal: Int) {
        activityRepository.setDailyGoal(goal)
        gamificationRepository.recalculate()
    }
}

class BootstrapGameUseCase(
    private val gamificationRepository: GamificationRepository
) {
    suspend operator fun invoke() {
        gamificationRepository.seedIfEmpty()
        gamificationRepository.recalculate()
    }
}
