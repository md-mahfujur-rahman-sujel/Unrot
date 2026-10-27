package com.unrot.app.ui.block

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unrot.app.data.repository.LimitRepository
import com.unrot.app.domain.model.MathProblem
import com.unrot.app.domain.usecase.CalculateUnlockDifficulty
import com.unrot.app.domain.usecase.GenerateMathProblems
import com.unrot.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockState(
    val showChallenge: Boolean = false,
    val problemsRequired: Int = 5,
    val extraMinutes: Int = 5,
    val correctCount: Int = 0,
    val currentProblem: MathProblem? = null,
    val userAnswer: String = "",
    val feedback: String? = null,
    val unlocked: Boolean = false,
    val packageName: String = "",
    val scheduleEndLabel: String? = null
) {
    val progress: Float
        get() = if (problemsRequired > 0) correctCount.toFloat() / problemsRequired else 0f
}

@HiltViewModel
class BlockViewModel @Inject constructor(
    private val limitRepository: LimitRepository,
    private val generateMathProblems: GenerateMathProblems,
    private val calculateUnlockDifficulty: CalculateUnlockDifficulty
) : ViewModel() {

    private val _state = MutableStateFlow(BlockState())
    val state: StateFlow<BlockState> = _state.asStateFlow()

    private var problemQueue: List<MathProblem> = emptyList()
    private var problemIndex = 0

    /** For schedule-blocked apps: shows the time the quiet-hours window ends. */
    fun loadScheduleInfo(packageName: String) {
        viewModelScope.launch {
            val app = limitRepository.getLimitedApp(packageName) ?: return@launch
            val label = app.scheduleEndMinutes?.let { DateUtils.formatMinuteOfDay(it) }
            _state.update { it.copy(scheduleEndLabel = label) }
        }
    }

    fun startChallenge(packageName: String) {
        viewModelScope.launch {
            val app = limitRepository.getLimitedApp(packageName) ?: return@launch
            val settings = limitRepository.getSettings()
            val difficulty = limitRepository.parseDifficulty(settings.mathDifficulty)
            val unlockDiff = calculateUnlockDifficulty(
                unlockCountToday = app.unlockCountToday,
                baseProblemsCount = settings.baseProblemsCount,
                extraMinutesPerUnlock = settings.extraMinutesPerUnlock,
                difficulty = difficulty
            )
            problemQueue = generateMathProblems(unlockDiff.problemsRequired, difficulty)
            problemIndex = 0
            _state.update {
                it.copy(
                    showChallenge = true,
                    problemsRequired = unlockDiff.problemsRequired,
                    extraMinutes = unlockDiff.extraMinutesGranted,
                    correctCount = 0,
                    currentProblem = problemQueue.firstOrNull(),
                    userAnswer = "",
                    feedback = null,
                    packageName = packageName
                )
            }
        }
    }

    fun onAnswerChanged(answer: String) {
        _state.update { it.copy(userAnswer = answer.filter { c -> c.isDigit() || c == '-' }, feedback = null) }
    }

    fun submitAnswer() {
        val current = _state.value
        val problem = current.currentProblem ?: return
        val answer = current.userAnswer.toIntOrNull()

        if (answer == problem.correctAnswer) {
            val newCount = current.correctCount + 1
            if (newCount >= current.problemsRequired) {
                completeUnlock(current)
            } else {
                problemIndex++
                _state.update {
                    it.copy(
                        correctCount = newCount,
                        currentProblem = problemQueue.getOrNull(problemIndex),
                        userAnswer = "",
                        feedback = "Correct!"
                    )
                }
            }
        } else {
            _state.update { it.copy(feedback = "Wrong answer — try again", userAnswer = "") }
        }
    }

    private fun completeUnlock(state: BlockState) {
        viewModelScope.launch {
            limitRepository.grantExtraTime(
                packageName = state.packageName,
                extraMs = state.extraMinutes * 60_000L,
                problemsSolved = state.problemsRequired,
                problemsRequired = state.problemsRequired
            )
            _state.update { it.copy(unlocked = true) }
        }
    }
}
