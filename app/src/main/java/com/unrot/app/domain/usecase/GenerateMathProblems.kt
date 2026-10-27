package com.unrot.app.domain.usecase

import com.unrot.app.domain.model.MathDifficulty
import com.unrot.app.domain.model.MathProblem
import kotlin.random.Random

import javax.inject.Inject

class GenerateMathProblems @Inject constructor() {

    operator fun invoke(count: Int, difficulty: MathDifficulty): List<MathProblem> {
        return List(count) { generateOne(difficulty) }
    }

    private fun generateOne(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> generateEasy()
            MathDifficulty.MEDIUM -> generateMedium()
            MathDifficulty.HARD -> generateHard()
        }
    }

    private fun generateEasy(): MathProblem {
        val a = Random.nextInt(1, 20)
        val b = Random.nextInt(1, 20)
        return if (Random.nextBoolean()) {
            MathProblem("$a + $b = ?", a + b)
        } else {
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min = ?", max - min)
        }
    }

    private fun generateMedium(): MathProblem {
        return when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(10, 99)
                val b = Random.nextInt(10, 99)
                MathProblem("$a + $b = ?", a + b)
            }
            1 -> {
                val a = Random.nextInt(20, 99)
                val b = Random.nextInt(10, a)
                MathProblem("$a - $b = ?", a - b)
            }
            else -> {
                val a = Random.nextInt(2, 12)
                val b = Random.nextInt(2, 12)
                MathProblem("$a × $b = ?", a * b)
            }
        }
    }

    private fun generateHard(): MathProblem {
        return when (Random.nextInt(4)) {
            0 -> {
                val a = Random.nextInt(50, 999)
                val b = Random.nextInt(50, 999)
                MathProblem("$a + $b = ?", a + b)
            }
            1 -> {
                val a = Random.nextInt(100, 999)
                val b = Random.nextInt(50, a)
                MathProblem("$a - $b = ?", a - b)
            }
            2 -> {
                val a = Random.nextInt(5, 25)
                val b = Random.nextInt(5, 25)
                MathProblem("$a × $b = ?", a * b)
            }
            else -> {
                val b = Random.nextInt(2, 12)
                val quotient = Random.nextInt(2, 12)
                val a = b * quotient
                MathProblem("$a ÷ $b = ?", quotient)
            }
        }
    }
}
