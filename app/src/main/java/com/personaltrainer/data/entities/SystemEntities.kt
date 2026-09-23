package com.personaltrainer.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A scheduled reminder entry.
 *
 * Each row corresponds to one alarm that should fire (or has fired).
 * The [AlarmScheduler] reads this table on boot and reconciles it
 * with AlarmManager.
 *
 * [kind] tells the UI what notification template to use.
 * [fireAt] is the Unix timestamp when the alarm should fire.
 * [refId] links back to the session or check-in this reminder belongs to.
 * [escalationLevel] tracks how far along the ladder we are (L0–L4).
 */
@Entity(tableName = "reminder")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: ReminderKind,
    val fireAt: Long,
    val refId: Long? = null,            // PlannedSession.id, or null for daily checks
    val state: ReminderState = ReminderState.PENDING,
    val escalationLevel: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderKind {
    MORNING_CHECKIN,
    PRE_GYM_MEAL,
    GYM_ALERT_L0,
    GYM_ALERT_L1,
    GYM_ALERT_L2,
    GYM_ALERT_L3,
    GYM_ALERT_L4,
    POST_GYM_CHECKIN,
    MEAL_PROMPT,
    EVENING_CHECKIN,
    BEDTIME_WIND_DOWN,
    WEEKLY_REVIEW,
    WRAP_UP
}

enum class ReminderState { PENDING, FIRED, DISMISSED, CANCELLED }

// ─── Append-only Event log ────────────────────────────────────────────────────

/**
 * Append-only event log.  Never update or delete rows.
 * [type] is a string like "SessionStarted", "MealLogged", etc.
 * [payload] is a JSON string with event-specific data.
 */
@Entity(tableName = "event")
data class AppEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long = System.currentTimeMillis(),
    val type: String,
    val payload: String = "{}"          // JSON
)

// ─── LLM response cache ───────────────────────────────────────────────────────

/**
 * Cache for LLM responses, keyed by SHA-256 hash of the input.
 * Prevents redundant API calls for the same meal text, etc.
 */
@Entity(tableName = "llm_cache")
data class LlmCache(
    @PrimaryKey val inputHash: String,
    val response: String,
    val createdAt: Long = System.currentTimeMillis()
)
