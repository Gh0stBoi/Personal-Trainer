package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A meal log entry for one slot on one day.
 *
 * [rawText] is what the user typed before LLM parsing.
 * [itemsJson] is the validated JSON array of parsed food items.
 * [confidence] is the LLM confidence (0.0–1.0); null = manually entered.
 * [offPlan] = true if the user flagged it as off-plan.
 */
@Entity(tableName = "meal_log")
data class MealLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                   // "yyyy-MM-dd"
    val slot: MealSlot,
    val rawText: String? = null,
    val itemsJson: String? = null,      // JSON array of food items
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val confidence: Float? = null,      // null = manually entered exactly
    val offPlan: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MealSlot {
    BREAKFAST, MORNING_SNACK, LUNCH, AFTERNOON_SNACK, DINNER, EVENING_SNACK, EXTRA
}

// ─── Sleep Log ────────────────────────────────────────────────────────────────

/**
 * Sleep record — one per day.  Populated from morning check-in or Health Connect.
 */
@Entity(tableName = "sleep_log")
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                   // "yyyy-MM-dd"
    val hours: Float,
    /** 1 = terrible, 5 = excellent */
    val quality: Int,
    val source: String = "manual"       // "manual" | "health_connect"
)
