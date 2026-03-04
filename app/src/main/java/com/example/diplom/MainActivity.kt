package com.example.diplom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diplom.app.AppContainer
import com.example.diplom.data.sensor.StepCounterManager
import com.example.diplom.domain.model.AppUserMode
import com.example.diplom.domain.model.Exercise
import com.example.diplom.ui.MainUiState
import com.example.diplom.ui.MainViewModel
import com.example.diplom.ui.theme.DiplomTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(
            activityRepository = container.activityRepository,
            gamificationRepository = container.gamificationRepository,
            trainingRepository = container.trainingRepository
        )
    }
    private lateinit var stepCounterManager: StepCounterManager
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            stepCounterManager.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(applicationContext)
        stepCounterManager = StepCounterManager(applicationContext) { delta ->
            mainViewModel.addSteps(delta)
        }
        container.scheduleDailyRecalculation(applicationContext)
        enableEdgeToEdge()
        setContent {
            DiplomTheme {
                DiplomApp(mainViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (requiresActivityPermission() && !hasActivityPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            stepCounterManager.start()
        }
    }

    override fun onStop() {
        stepCounterManager.stop()
        super.onStop()
    }

    private fun requiresActivityPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun hasActivityPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun DiplomApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> TodayScreen(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding),
                    onAddSteps = viewModel::addSteps,
                    onSetGoal = viewModel::updateDailyGoal
                )
                AppDestinations.FAVORITES -> StoryMapScreen(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.PROFILE -> AchievementsScreen(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.STATS -> StatisticsScreen(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding)
                )
                AppDestinations.TRAINING -> TrainingScreen(
                    state = uiState,
                    modifier = Modifier.padding(innerPadding),
                    onModeSelected = viewModel::setUserMode,
                    onAddExercise = viewModel::addExercise,
                    onAddToWorkout = viewModel::addToWorkout,
                    onRemoveWorkoutItem = viewModel::removeWorkoutItem,
                    onExport = viewModel::exportProgress,
                    onImport = viewModel::importProgress
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Today", Icons.Default.Home),
    FAVORITES("Story", Icons.Default.Favorite),
    PROFILE("Rewards", Icons.Default.AccountBox),
    STATS("Stats", Icons.Default.Star),
    TRAINING("Training", Icons.Default.Build),
}

@Composable
private fun TodayScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onAddSteps: (Int) -> Unit,
    onSetGoal: (Int) -> Unit
) {
    var goalInput by remember { mutableIntStateOf(state.dailyGoal) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Today", fontWeight = FontWeight.Bold)
            Text("Steps: ${state.today.steps}")
            Text("Distance: ${"%.2f".format(state.today.distanceKm)} km")
            Text("Active minutes: ${state.today.activeMinutes}")
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Daily goal: ${state.dailyGoal} steps")
                    LinearProgressIndicator(
                        progress = { state.goalProgressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onAddSteps(500) }) { Text("+500") }
                        Button(onClick = { onAddSteps(1000) }) { Text("+1000") }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Level ${state.profile.level} | XP ${state.profile.xp}")
                    LinearProgressIndicator(
                        progress = { state.levelProgressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Streak: ${state.profile.streakDays} days (best ${state.profile.bestStreakDays})")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Weekly challenge")
                    Text("${state.weeklyChallenge.progressSteps}/${state.weeklyChallenge.targetSteps} steps")
                    LinearProgressIndicator(
                        progress = {
                            (state.weeklyChallenge.progressSteps /
                                state.weeklyChallenge.targetSteps.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(if (state.weeklyChallenge.completed) "Completed" else "In progress")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Quick goal tuning")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                goalInput = (goalInput - 1000).coerceAtLeast(2000)
                                onSetGoal(goalInput)
                            }
                        ) { Text("-1000") }
                        Button(
                            onClick = {
                                goalInput = (goalInput + 1000).coerceAtMost(30000)
                                onSetGoal(goalInput)
                            }
                        ) { Text("+1000") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryMapScreen(state: MainUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Fantasy Hike Campaign", fontWeight = FontWeight.Bold)
            Text("Walk to unlock chapters and story milestones.")
        }
        items(state.chapters) { chapter ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Chapter ${chapter.chapterNumber}: ${chapter.title}")
                    Text("Required distance: ${chapter.requiredDistanceKm} km")
                    Text("Quest: ${chapter.questSteps} steps in a day")
                    Text(if (chapter.unlocked) "Status: Unlocked" else "Status: Locked")
                }
            }
        }
    }
}

@Composable
private fun AchievementsScreen(state: MainUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Badges and rewards", fontWeight = FontWeight.Bold)
        }
        items(state.achievements) { achievement ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(achievement.title)
                    Text(achievement.description)
                    Text(
                        if (achievement.unlocked) {
                            "Unlocked at: ${achievement.unlockedAtIso ?: "unknown"}"
                        } else {
                            "Locked"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(state: MainUiState, modifier: Modifier = Modifier) {
    val totalSteps = state.recentDays.sumOf { it.steps }
    val totalKm = state.recentDays.sumOf { it.distanceKm }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("7-day trends", fontWeight = FontWeight.Bold)
            Text("Total steps (last days): $totalSteps")
            Text("Distance (last days): ${"%.2f".format(totalKm)} km")
        }
        items(state.recentDays) { day ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day.dateIso)
                    Text("${day.steps} steps")
                }
            }
        }
    }
}

@Composable
private fun TrainingScreen(
    state: MainUiState,
    modifier: Modifier = Modifier,
    onModeSelected: (AppUserMode) -> Unit,
    onAddExercise: (String, String, Int) -> Unit,
    onAddToWorkout: (Exercise) -> Unit,
    onRemoveWorkoutItem: (Long) -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit
) {
    var titleInput by rememberSaveable { mutableStateOf("") }
    var descriptionInput by rememberSaveable { mutableStateOf("") }
    var repsInput by rememberSaveable { mutableStateOf("10") }
    var importInput by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Режим использования", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onModeSelected(AppUserMode.STUDENT) }) { Text("Ученик") }
                        Button(onClick = { onModeSelected(AppUserMode.TRAINER) }) { Text("Тренер") }
                    }
                    Text("Текущий режим: ${if (state.userMode == AppUserMode.TRAINER) "Тренер" else "Ученик"}")
                }
            }
        }

        if (state.userMode == AppUserMode.TRAINER) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Добавить упражнение в банк", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Название") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = { repsInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Повторения по умолчанию") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onAddExercise(titleInput, descriptionInput, repsInput.toIntOrNull() ?: 10)
                                titleInput = ""
                                descriptionInput = ""
                                repsInput = "10"
                            }
                        ) {
                            Text("Добавить")
                        }
                    }
                }
            }
        }

        item {
            Text("Банк упражнений", fontWeight = FontWeight.Bold)
        }
        items(state.exerciseBank) { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.title, fontWeight = FontWeight.Medium)
                    if (exercise.description.isNotBlank()) {
                        Text(exercise.description)
                    }
                    Text("Повторения: ${exercise.defaultReps}")
                    if (state.userMode == AppUserMode.STUDENT) {
                        Button(onClick = { onAddToWorkout(exercise) }) { Text("В тренировку") }
                    }
                }
            }
        }

        item {
            Text("Тренировка на сегодня", fontWeight = FontWeight.Bold)
        }
        items(state.todayWorkout) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.sortOrder + 1}. ${item.title} x ${item.plannedReps}")
                    TextButton(onClick = { onRemoveWorkoutItem(item.id) }) { Text("Удалить") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Экспорт / импорт прогресса", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onExport) { Text("Сформировать JSON") }
                        TextButton(
                            onClick = {
                                if (state.exportedJson.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(state.exportedJson))
                                }
                            }
                        ) {
                            Text("Копировать")
                        }
                    }
                    if (state.exportedJson.isNotBlank()) {
                        Text("JSON готов. Можно отправить через мессенджер.")
                    }
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        label = { Text("Вставьте JSON для импорта") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onImport(importInput) }) { Text("Импортировать") }
                    state.importStatus?.let { Text(it) }
                }
            }
        }
    }
}