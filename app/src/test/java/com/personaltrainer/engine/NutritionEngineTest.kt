package com.personaltrainer.engine

import com.personaltrainer.data.entities.Goal
import com.personaltrainer.data.entities.Sex
import com.personaltrainer.engine.nutrition.NutritionEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [NutritionEngine] — section 11 of the implementation plan.
 *
 * These are pure Kotlin tests that run on the JVM (no Android emulator needed).
 */
class NutritionEngineTest {

    // ── BMR tests ────────────────────────────────────────────────────────

    @Test
    fun `BMR Mifflin-St Jeor male 30 years 80kg 180cm`() {
        val bmr = NutritionEngine.bmr(80f, 180f, 30, Sex.MALE)
        // Expected: 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        assertEquals(1780f, bmr, 1f)
    }

    @Test
    fun `BMR Mifflin-St Jeor female 25 years 60kg 165cm`() {
        val bmr = NutritionEngine.bmr(60f, 165f, 25, Sex.FEMALE)
        // Expected: 10*60 + 6.25*165 - 5*25 - 161 = 600 + 1031.25 - 125 - 161 = 1345.25
        assertEquals(1345f, bmr, 2f)
    }

    // ── TDEE tests ───────────────────────────────────────────────────────

    @Test
    fun `TDEE sedentary (0 sessions) uses 1_2 multiplier`() {
        val bmr = 1800f
        val tdee = NutritionEngine.tdee(bmr, 0)
        assertEquals(bmr * 1.2f, tdee, 1f)
    }

    @Test
    fun `TDEE very active (5 sessions) uses 1_725 multiplier`() {
        val bmr = 1800f
        val tdee = NutritionEngine.tdee(bmr, 5)
        assertEquals(bmr * 1.725f, tdee, 1f)
    }

    // ── Target calories ───────────────────────────────────────────────────

    @Test
    fun `Fat loss target is capped at max 500 kcal deficit`() {
        val tdee = 3000f
        val target = NutritionEngine.targetCalories(tdee, Goal.FAT_LOSS, Sex.MALE, 30)
        // 20% of 3000 = 600, capped at 500 → 3000 - 500 = 2500
        assertEquals(2500f, target, 5f)
    }

    @Test
    fun `Target calories never go below male calorie floor`() {
        val tdee = 1600f   // Very low TDEE
        val target = NutritionEngine.targetCalories(tdee, Goal.FAT_LOSS, Sex.MALE, 30)
        assertTrue("Should not go below floor", target >= NutritionEngine.CALORIE_FLOOR_MALE)
    }

    @Test
    fun `Under-18 users get no deficit`() {
        val tdee = 2500f
        val target = NutritionEngine.targetCalories(tdee, Goal.FAT_LOSS, Sex.MALE, 17)
        assertEquals(tdee, target, 1f)
    }

    @Test
    fun `Muscle gain adds 7_5 percent surplus`() {
        val tdee = 2500f
        val target = NutritionEngine.targetCalories(tdee, Goal.MUSCLE_GAIN, Sex.MALE, 25)
        assertEquals(tdee * 1.075f, target, 5f)
    }

    // ── Macro split ───────────────────────────────────────────────────────

    @Test
    fun `Macros sum to approximately target calories`() {
        val macros = NutritionEngine.macros(2500f, 80f)
        val totalFromMacros = macros.proteinG * 4 + macros.carbsG * 4 + macros.fatG * 9
        assertEquals(2500f, totalFromMacros.toFloat(), 50f)  // within 50 kcal
    }

    @Test
    fun `Protein is at least 1_6g per kg of body weight`() {
        val macros = NutritionEngine.macros(2500f, 80f)
        val minProtein = 80 * NutritionEngine.PROTEIN_G_PER_KG_MIN
        assertTrue("Protein ${macros.proteinG}g should be >= $minProtein g", macros.proteinG >= minProtein)
    }

    // ── Adaptive TDEE ────────────────────────────────────────────────────

    @Test
    fun `Adaptive TDEE returns null with less than 14 days of data`() {
        val result = NutritionEngine.adaptiveTdee(2000f, -0.5f, 10)
        assertNull(result)
    }

    @Test
    fun `Adaptive TDEE estimates real expenditure correctly`() {
        // Lost 0.5 kg in 14 days eating 2000 kcal/day
        // kcal from weight loss = 0.5 * 7700 / 14 = 275
        // Estimated TDEE = 2000 + 275 = 2275
        val result = NutritionEngine.adaptiveTdee(2000f, -0.5f, 14)
        assertNotNull(result)
        assertEquals(2275f, result!!, 10f)
    }

    // ── Off-plan budget ───────────────────────────────────────────────────

    @Test
    fun `Off-plan reduction never drops below floor`() {
        val reduced = NutritionEngine.offPlanDailyTarget(
            normalDailyTarget = 1600f,
            excessKcal = 5000f,   // extreme excess
            floorKcal = NutritionEngine.CALORIE_FLOOR_MALE
        )
        assertTrue(reduced >= NutritionEngine.CALORIE_FLOOR_MALE)
    }

    @Test
    fun `Off-plan reduction is capped at 15 percent per day`() {
        val normalTarget = 2500f
        val reduced = NutritionEngine.offPlanDailyTarget(
            normalDailyTarget = normalTarget,
            excessKcal = 3000f,
            spreadDays = 3,
            maxReductionPerDay = 0.15f,
            floorKcal = NutritionEngine.CALORIE_FLOOR_MALE
        )
        val maxReduction = normalTarget * 0.15f
        assertTrue("Reduction should not exceed 15%", reduced >= normalTarget - maxReduction)
    }
}
