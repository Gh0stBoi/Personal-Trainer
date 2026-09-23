package com.personaltrainer.engine.rescheduler

import com.personaltrainer.data.entities.PlannedSession
import com.personaltrainer.data.entities.SessionPriority
import com.personaltrainer.data.entities.SessionStatus
import com.personaltrainer.data.entities.SessionType

/**
 * Rescheduler — section 6.6 of the implementation plan.
 *
 * Pure Kotlin, no Android imports, fully unit-testable.
 *
 * The rescheduler is the core algorithm that decides what to do
 * when a session is missed. It returns a list of [PlanChange] actions
 * that the repository layer then applies to the database.
 */
object Rescheduler {

    private const val MIN_HOURS_REST = 48
    private const val MAX_CONSECUTIVE_TRAINING_DAYS = 4
    private const val LOOK_AHEAD_DAYS = 3
    private const val MAX_SESSION_EXTENSION_MIN = 15
    private const val VOLUME_DEBT_CAP = 0.10f   // max 10% extra volume next week

    /**
     * Handle a missed session.
     *
     * @param missed       the session that was skipped
     * @param weekSessions all sessions in the current week
     * @param availableDays list of date strings (yyyy-MM-dd) that are open in the next 3 days
     * @param todayEpochDay epoch day (for computing 48h rules)
     */
    fun handleMissed(
        missed: PlannedSession,
        weekSessions: List<PlannedSession>,
        availableDays: List<DaySlot>,
        todayEpochDay: Long
    ): List<PlanChange> {
        // Find candidate slots satisfying the 48h and consecutive-day rules
        val candidateSlots = availableDays.filter { day ->
            canTrainOn(day, missed, weekSessions)
        }

        return when {
            // 1. A free slot exists → MOVE
            candidateSlots.isNotEmpty() -> {
                val bestSlot = candidateSlots.first()
                listOf(
                    PlanChange.Move(missed.id, bestSlot.date),
                    PlanChange.ShiftFollowing(missed.id)
                )
            }

            // 2. No free slot but key session → MERGE key lifts into next compatible session
            missed.priority == SessionPriority.KEY && canMerge(missed, weekSessions) -> {
                val nextCompatible = weekSessions.firstOrNull { s ->
                    s.status == SessionStatus.PLANNED &&
                    s.id != missed.id &&
                    s.date > missed.date
                }
                if (nextCompatible != null) {
                    listOf(
                        PlanChange.MergeKeyLifts(missed.id, nextCompatible.id),
                        PlanChange.DropAccessories(missed.id)
                    )
                } else {
                    dropSession(missed)
                }
            }

            // 3. Nothing works → DROP
            else -> dropSession(missed)
        }
    }

    /**
     * Handle a long absence (e.g. illness, travel, injury).
     *
     * @param daysSinceLastSession computed from the last DONE session
     */
    fun handleGap(daysSinceLastSession: Int): GapReEntryPlan {
        return when {
            daysSinceLastSession < 3 -> GapReEntryPlan(
                loadFraction = 1.0f, setReduction = 0,
                description = "Resume as planned."
            )
            daysSinceLastSession < 7 -> GapReEntryPlan(
                loadFraction = 0.90f, setReduction = 1,
                description = "Re-entry session: 90% load, 1 set fewer."
            )
            daysSinceLastSession < 14 -> GapReEntryPlan(
                loadFraction = 0.80f, setReduction = 1,
                description = "Re-entry session: 80% load, 1 set fewer."
            )
            else -> GapReEntryPlan(
                loadFraction = 0.70f, setReduction = 2,
                description = "Ramp-up week: 70% load, 2 sets fewer. Consider regenerating the program.",
                shouldRegenerate = daysSinceLastSession >= 14
            )
        }
    }

    // ─ Private helpers ─────────────────────────────────────────────────────

    private fun canTrainOn(
        day: DaySlot,
        missed: PlannedSession,
        allSessions: List<PlannedSession>
    ): Boolean {
        // Check 48h rule (no same muscle group trained within 48h)
        val recentSameGroup = allSessions.filter { s ->
            s.type == missed.type &&
            s.status == SessionStatus.DONE &&
            s.date <= day.date
        }.maxByOrNull { it.date }

        if (recentSameGroup != null) {
            val daysBetween = daysBetween(recentSameGroup.date, day.date)
            if (daysBetween < 2) return false
        }

        // Check consecutive day cap
        val consecutiveBefore = countConsecutiveDaysBefore(day.date, allSessions)
        if (consecutiveBefore >= MAX_CONSECUTIVE_TRAINING_DAYS) return false

        return true
    }

    private fun canMerge(missed: PlannedSession, weekSessions: List<PlannedSession>): Boolean {
        // Can only merge if there's a compatible session in the same week
        return weekSessions.any { s ->
            s.status == SessionStatus.PLANNED &&
            s.id != missed.id &&
            s.date > missed.date
        }
    }

    private fun dropSession(missed: PlannedSession): List<PlanChange> {
        return if (missed.priority == SessionPriority.MINOR) {
            listOf(PlanChange.Drop(missed.id))
        } else {
            listOf(
                PlanChange.Drop(missed.id),
                PlanChange.AddVolumeDebt(missed.id, VOLUME_DEBT_CAP)
            )
        }
    }

    private fun daysBetween(from: String, to: String): Int {
        // Simple date diff using string comparison for pure Kotlin (no Android)
        // Format: "yyyy-MM-dd" — lexicographic order works
        val fromParts = from.split("-").map { it.toInt() }
        val toParts = to.split("-").map { it.toInt() }
        val fromEpoch = epochDay(fromParts[0], fromParts[1], fromParts[2])
        val toEpoch = epochDay(toParts[0], toParts[1], toParts[2])
        return (toEpoch - fromEpoch).toInt()
    }

    private fun countConsecutiveDaysBefore(date: String, sessions: List<PlannedSession>): Int {
        // Count how many consecutive days before [date] have a non-rest session
        var count = 0
        var checkDate = date
        while (true) {
            val prevDate = prevDay(checkDate)
            val hasSession = sessions.any { it.date == prevDate && it.status != SessionStatus.SKIPPED }
            if (!hasSession) break
            count++
            checkDate = prevDate
            if (count >= MAX_CONSECUTIVE_TRAINING_DAYS) break
        }
        return count
    }

    // Minimal epoch day calculation for pure Kotlin (no java.time)
    private fun epochDay(year: Int, month: Int, day: Int): Long {
        var y = year.toLong(); var m = month.toLong()
        if (m <= 2) { y--; m += 12 }
        val a = y / 100; val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toLong() +
               (30.6001 * (m + 1)).toLong() +
               day + b - 1524
    }

    private fun prevDay(date: String): String {
        val parts = date.split("-").map { it.toInt() }
        val epoch = epochDay(parts[0], parts[1], parts[2]) - 1
        return epochToDateString(epoch)
    }

    private fun epochToDateString(epochDay: Long): String {
        // Reverse of epochDay — simple approximation sufficient for scheduling
        var z = epochDay + 2440588L - 2440588L   // keep in epoch-day space
        // Use java.util.Calendar equivalent via basic algorithm
        val jd = epochDay + 2440588L
        var l = jd + 68569L
        val n = (4 * l) / 146097L
        l -= (146097L * n + 3L) / 4L
        val i = (4000L * (l + 1L)) / 1461001L
        l -= (1461L * i) / 4L - 31L
        val j = (80L * l) / 2447L
        val day = (l - (2447L * j) / 80L).toInt()
        l = j / 11L
        val month = (j + 2L - 12L * l).toInt()
        val year = (100L * (n - 49L) + i + l).toInt()
        return "%04d-%02d-%02d".format(year, month, day)
    }
}

// ─── Domain types ────────────────────────────────────────────────────────────

data class DaySlot(
    val date: String,           // "yyyy-MM-dd"
    val availableMinutes: Int   // remaining available training minutes
)

data class GapReEntryPlan(
    val loadFraction: Float,
    val setReduction: Int,
    val description: String,
    val shouldRegenerate: Boolean = false
)

/** Sealed class of actions the rescheduler can return. */
sealed class PlanChange {
    /** Move session [sessionId] to [newDate]. */
    data class Move(val sessionId: Long, val newDate: String) : PlanChange()

    /** Shift all sessions after [sessionId] by one slot in the rolling split. */
    data class ShiftFollowing(val afterSessionId: Long) : PlanChange()

    /** Merge key lifts from [fromSessionId] into [intoSessionId]. */
    data class MergeKeyLifts(val fromSessionId: Long, val intoSessionId: Long) : PlanChange()

    /** Drop accessory sets from [sessionId]. */
    data class DropAccessories(val sessionId: Long) : PlanChange()

    /** Drop [sessionId] entirely. */
    data class Drop(val sessionId: Long) : PlanChange()

    /** Record volume debt for the missed session (repay next week, capped at [fraction]). */
    data class AddVolumeDebt(val sessionId: Long, val fraction: Float) : PlanChange()
}
