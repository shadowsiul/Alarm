package com.shadowsiul.alarm.alarm

import kotlin.random.Random

object SnoozeMath {
    data class Problem(val prompt: String, val answer: Int)

    fun generate(random: Random = Random.Default): Problem {
        val a = random.nextInt(2, 13)
        val b = random.nextInt(1, 13)
        return if (random.nextBoolean()) {
            Problem("$a + $b", a + b)
        } else {
            val left = maxOf(a, b)
            val right = minOf(a, b)
            Problem("$left - $right", left - right)
        }
    }
}
