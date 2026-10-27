package com.unrot.app.domain.usecase

import com.unrot.app.domain.model.MathDifficulty
import com.unrot.app.domain.model.UnlockDifficulty

import javax.inject.Inject

class CalculateUnlockDifficulty @Inject constructor() {

    operator fun invoke(
        unlockCountToday: Int,
        baseProblemsCount: Int,
        extraMinutesPerUnlock: Int,
        difficulty: MathDifficulty
    ): UnlockDifficulty {
        val problemsRequired = when (unlockCountToday) {
            0 -> baseProblemsCount
            1 -> baseProblemsCount + 3
            2 -> baseProblemsCount + 7
            else -> baseProblemsCount + 7 + (unlockCountToday - 2) * 4
        }
        return UnlockDifficulty(
            problemsRequired = problemsRequired,
            extraMinutesGranted = extraMinutesPerUnlock,
            difficulty = difficulty
        )
    }
}
