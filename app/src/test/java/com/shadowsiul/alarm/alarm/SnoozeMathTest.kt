package com.shadowsiul.alarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SnoozeMathTest {

    @Test
    fun additionAndSubtractionStaySimple() {
        repeat(20) { index ->
            val problem = SnoozeMath.generate(Random(index.toLong()))
            val parts = problem.prompt.split(" ")
            assertEquals(3, parts.size)
            val left = parts[0].toInt()
            val right = parts[2].toInt()
            val expected = if (parts[1] == "+") left + right else left - right
            assertEquals(expected, problem.answer)
            assertTrue(problem.answer >= 0)
            assertTrue(left in 1..12)
            assertTrue(right in 1..12)
        }
    }
}
