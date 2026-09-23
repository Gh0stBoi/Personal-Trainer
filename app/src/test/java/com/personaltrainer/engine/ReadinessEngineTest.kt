package com.personaltrainer.engine

import com.personaltrainer.engine.readiness.ReadinessEngine
import com.personaltrainer.engine.readiness.ReadinessRecommendation
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [ReadinessEngine].
 */
class ReadinessEngineTest {

    @Test
    fun `Pain flag always returns REST regardless of score`() {
        val result = ReadinessEngine.score(
            sleepHours = 9f,
            sleepQuality = 5,
            sorenessLevel = 1,
            energyMood = 5,
            painFlag = true
        )
        assertEquals(0, result.score)
        assertEquals(ReadinessRecommendation.REST, result.recommendation)
        assertTrue(result.painFlag)
    }

    @Test
    fun `Perfect conditions give score above 70`() {
        val result = ReadinessEngine.score(
            sleepHours = 8f,
            sleepQuality = 5,
            sorenessLevel = 1,
            energyMood = 5
        )
        assertTrue("Score ${result.score} should be >= 70", result.score >= 70)
        assertEquals(ReadinessRecommendation.TRAIN_AS_PLANNED, result.recommendation)
    }

    @Test
    fun `Poor sleep with high soreness gives reduced volume`() {
        val result = ReadinessEngine.score(
            sleepHours = 5.5f,
            sleepQuality = 2,
            sorenessLevel = 4,
            energyMood = 2
        )
        assertTrue("Score ${result.score} should be in 40-69 range", result.score in 0..69)
        assertEquals(ReadinessRecommendation.REDUCED_VOLUME, result.recommendation)
    }

    @Test
    fun `Very poor conditions give REST recommendation`() {
        val result = ReadinessEngine.score(
            sleepHours = 3f,
            sleepQuality = 1,
            sorenessLevel = 5,
            energyMood = 1
        )
        assertTrue("Score ${result.score} should be < 40", result.score < 40)
        assertEquals(ReadinessRecommendation.REST, result.recommendation)
    }

    @Test
    fun `Elevated resting HR ratio reduces score`() {
        val baseResult = ReadinessEngine.score(7f, 3, 2, 3)
        val elevatedResult = ReadinessEngine.score(7f, 3, 2, 3, restingHrRatio = 1.25f)
        assertTrue("Elevated HR should lower score", elevatedResult.score <= baseResult.score)
    }

    @Test
    fun `Score is always in 0-100 range`() {
        val result = ReadinessEngine.score(0f, 1, 5, 1)
        assertTrue(result.score in 0..100)
    }
}
