package com.personaltrainer.data.dao

import androidx.room.*
import com.personaltrainer.data.entities.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfile)

    @Query("UPDATE user_profile SET onboardingComplete = :complete WHERE id = 1")
    suspend fun setOnboardingComplete(complete: Boolean)

    @Query("UPDATE user_profile SET weightKg = :weightKg WHERE id = 1")
    suspend fun updateWeight(weightKg: Float)
}
