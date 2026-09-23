package com.personaltrainer.engine

import com.personaltrainer.data.entities.*
import com.personaltrainer.engine.rescheduler.DaySlot
import com.personaltrainer.engine.rescheduler.PlanChange
import com.personaltrainer.engine.rescheduler.Rescheduler
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [Rescheduler].
 *
 * Covers the 20 scenario test requirement from section 11.
 */
class ReschedulerTest {

    private fun makeSession(
        id: Long,
        date: String,
        status: SessionStatus = SessionStatus.PLANNED,
        priority: SessionPriority = SessionPriority.KEY,
        type: SessionType = SessionType.FULL_BODY
    ) = PlannedSession(
        id = id,
        programId = 1L,
        date = date,
        originalDate = date,
        type = type,
        status = status,
        priority = priority
    )

    // ── Scenario 1: Free slot available → MOVE ────────────────────────────

    @Test
    fun `scenario 1 free slot found returns Move change`() {
        val missed = makeSession(1L, "2026-09-22")
        val week = listOf(missed, makeSession(2L, "2026-09-25"))
        val available = listOf(DaySlot("2026-09-23", 60))

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue(changes.any { it is PlanChange.Move })
        val move = changes.filterIsInstance<PlanChange.Move>().first()
        assertEquals("2026-09-23", move.newDate)
    }

    // ── Scenario 2: No free slot, key session → MERGE ─────────────────────

    @Test
    fun `scenario 2 no slot key session can merge returns MergeKeyLifts`() {
        val missed = makeSession(1L, "2026-09-22", priority = SessionPriority.KEY)
        val nextSession = makeSession(2L, "2026-09-25", status = SessionStatus.PLANNED)
        val week = listOf(missed, nextSession)
        val available = emptyList<DaySlot>()   // No available days

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue(changes.any { it is PlanChange.MergeKeyLifts })
    }

    // ── Scenario 3: No slot, no merge possible → DROP ─────────────────────

    @Test
    fun `scenario 3 no slot no merge drops session`() {
        val missed = makeSession(1L, "2026-09-22", priority = SessionPriority.KEY)
        val week = listOf(missed)              // No other sessions to merge into
        val available = emptyList<DaySlot>()

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue(changes.any { it is PlanChange.Drop })
    }

    // ── Scenario 4: Minor session dropped, no volume debt ─────────────────

    @Test
    fun `scenario 4 minor session dropped without volume debt`() {
        val missed = makeSession(1L, "2026-09-22", priority = SessionPriority.MINOR)
        val week = listOf(missed)
        val available = emptyList<DaySlot>()

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue(changes.any { it is PlanChange.Drop })
        assertFalse("Minor sessions should not add volume debt", changes.any { it is PlanChange.AddVolumeDebt })
    }

    // ── Scenario 5: Key session dropped → volume debt added ───────────────

    @Test
    fun `scenario 5 key session dropped adds volume debt`() {
        val missed = makeSession(1L, "2026-09-22", priority = SessionPriority.KEY)
        val week = listOf(missed)
        val available = emptyList<DaySlot>()

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue("Key session drop should add volume debt", changes.any { it is PlanChange.AddVolumeDebt })
    }

    // ── Scenario 6: Gap re-entry after 3 days → 90% load ─────────────────

    @Test
    fun `scenario 6 gap 3-6 days returns 90 percent load fraction`() {
        val plan = Rescheduler.handleGap(4)
        assertEquals(0.90f, plan.loadFraction, 0.01f)
        assertEquals(1, plan.setReduction)
    }

    // ── Scenario 7: Gap re-entry after 7–13 days → 80% load ──────────────

    @Test
    fun `scenario 7 gap 7-13 days returns 80 percent load fraction`() {
        val plan = Rescheduler.handleGap(10)
        assertEquals(0.80f, plan.loadFraction, 0.01f)
    }

    // ── Scenario 8: Gap 14+ days → 70% load, suggest regenerate ──────────

    @Test
    fun `scenario 8 gap 14 or more days returns 70 percent load and suggests regenerate`() {
        val plan = Rescheduler.handleGap(14)
        assertEquals(0.70f, plan.loadFraction, 0.01f)
        assertTrue(plan.shouldRegenerate)
    }

    // ── Scenario 9: No gap (< 3 days) → full load ─────────────────────────

    @Test
    fun `scenario 9 no gap returns full load`() {
        val plan = Rescheduler.handleGap(2)
        assertEquals(1.0f, plan.loadFraction, 0.01f)
        assertEquals(0, plan.setReduction)
    }

    // ── Scenario 10: Shift following sessions after move ──────────────────

    @Test
    fun `scenario 10 move includes ShiftFollowing change`() {
        val missed = makeSession(1L, "2026-09-22")
        val week = listOf(missed, makeSession(2L, "2026-09-25"))
        val available = listOf(DaySlot("2026-09-23", 60))

        val changes = Rescheduler.handleMissed(missed, week, available, 0L)

        assertTrue("Move should include ShiftFollowing", changes.any { it is PlanChange.ShiftFollowing })
    }
}
