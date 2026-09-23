package com.personaltrainer.data.dao

import androidx.room.*
import com.personaltrainer.data.entities.*
import kotlinx.coroutines.flow.Flow

// ─── Program DAO ──────────────────────────────────────────────────────────────

@Dao
interface ProgramDao {

    @Insert
    suspend fun insert(program: Program): Long

    @Update
    suspend fun update(program: Program)

    @Query("SELECT * FROM program WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<Program?>

    @Query("SELECT * FROM program WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): Program?

    @Query("SELECT * FROM program ORDER BY startDate DESC")
    fun observeAll(): Flow<List<Program>>

    @Query("UPDATE program SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: ProgramStatus)
}

// ─── Planned Session DAO ──────────────────────────────────────────────────────

@Dao
interface PlannedSessionDao {

    @Insert
    suspend fun insert(session: PlannedSession): Long

    @Insert
    suspend fun insertAll(sessions: List<PlannedSession>)

    @Update
    suspend fun update(session: PlannedSession)

    @Query("SELECT * FROM planned_session WHERE date = :date ORDER BY id ASC")
    fun observeByDate(date: String): Flow<List<PlannedSession>>

    @Query("SELECT * FROM planned_session WHERE date = :date ORDER BY id ASC")
    suspend fun getByDate(date: String): List<PlannedSession>

    @Query("""
        SELECT * FROM planned_session
        WHERE date BETWEEN :from AND :to
        ORDER BY date ASC
    """)
    suspend fun getBetween(from: String, to: String): List<PlannedSession>

    @Query("SELECT * FROM planned_session WHERE id = :id")
    suspend fun getById(id: Long): PlannedSession?

    @Query("SELECT * FROM planned_session WHERE status = 'PLANNED' ORDER BY date ASC")
    suspend fun getAllPlanned(): List<PlannedSession>

    @Query("""
        SELECT * FROM planned_session
        WHERE programId = :programId
        ORDER BY date ASC
    """)
    fun observeByProgram(programId: Long): Flow<List<PlannedSession>>

    @Query("UPDATE planned_session SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: SessionStatus)

    @Query("UPDATE planned_session SET date = :date, status = 'PLANNED' WHERE id = :id")
    suspend fun moveTo(id: Long, date: String)

    @Delete
    suspend fun delete(session: PlannedSession)
}

// ─── Planned Set DAO ──────────────────────────────────────────────────────────

@Dao
interface PlannedSetDao {

    @Insert
    suspend fun insertAll(sets: List<PlannedSet>)

    @Update
    suspend fun update(set: PlannedSet)

    @Query("SELECT * FROM planned_set WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun observeBySession(sessionId: Long): Flow<List<PlannedSet>>

    @Query("SELECT * FROM planned_set WHERE sessionId = :sessionId ORDER BY `order` ASC")
    suspend fun getBySession(sessionId: Long): List<PlannedSet>

    @Query("DELETE FROM planned_set WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Delete
    suspend fun delete(set: PlannedSet)
}

// ─── Set Log DAO ──────────────────────────────────────────────────────────────

@Dao
interface SetLogDao {

    @Insert
    suspend fun insert(log: SetLog): Long

    @Update
    suspend fun update(log: SetLog)

    @Delete
    suspend fun delete(log: SetLog)

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY exerciseId, setNo")
    fun observeBySession(sessionId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE sessionId = :sessionId ORDER BY exerciseId, setNo")
    suspend fun getBySession(sessionId: Long): List<SetLog>

    @Query("""
        SELECT * FROM set_log
        WHERE exerciseId = :exerciseId
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getByExercise(exerciseId: String, limit: Int = 50): List<SetLog>

    /** Last session's sets for a given exercise — used to seed next session's targets */
    @Query("""
        SELECT sl.* FROM set_log sl
        INNER JOIN planned_session ps ON sl.sessionId = ps.id
        WHERE sl.exerciseId = :exerciseId AND ps.status = 'DONE'
        ORDER BY sl.timestamp DESC
        LIMIT :sets
    """)
    suspend fun getLastSets(exerciseId: String, sets: Int = 5): List<SetLog>

    /** Best e1RM per exercise for the PR board */
    @Query("""
        SELECT exerciseId, MAX(e1rm) as e1rm, MAX(loadKg) as loadKg, MAX(reps) as reps
        FROM set_log
        GROUP BY exerciseId
    """)
    suspend fun getPRs(): List<ExercisePR>
}

/** Projection for the PR board — not a Room entity, just a POJO. */
data class ExercisePR(
    val exerciseId: String,
    val e1rm: Float,
    val loadKg: Float,
    val reps: Int
)
