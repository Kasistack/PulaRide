package com.example.data

import kotlinx.coroutines.flow.Flow

class RideRepository(private val rideDao: RideDao) {
    val rideHistory: Flow<List<RideHistoryEntity>> = rideDao.getRideHistory()

    suspend fun addRide(ride: RideHistoryEntity) {
        rideDao.insertRide(ride)
    }

    suspend fun clearHistory() {
        rideDao.clearHistory()
    }
}
