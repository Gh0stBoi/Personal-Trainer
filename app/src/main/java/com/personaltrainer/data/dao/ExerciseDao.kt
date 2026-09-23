package com.personaltrainer.data.dao

import androidx.room.*
import com.personaltrainer.data.entities.Exercise
import com.personaltrainer.data.entities.MuscleGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exercise: Exercise): Long

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise ORDER BY name ASC")
    suspend fun getAll(): List<Exercise>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    @Query("SELECT * FROM exercise WHERE primaryMuscle = :muscle ORDER BY isCompound DESC, name ASC")
    suspend fun getByMuscle(muscle: MuscleGroup): List<Exercise>

    @Query("SELECT * FROM exercise WHERE isCustom = 1 ORDER BY name ASC")
    suspend fun getCustom(): List<Exercise>

    @Query("SELECT * FROM exercise WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<Exercise>

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int
}
