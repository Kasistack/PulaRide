package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM ride_history ORDER BY timestamp DESC")
    fun getRideHistory(): Flow<List<RideHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideHistoryEntity)

    @Query("DELETE FROM ride_history")
    suspend fun clearHistory()
}
