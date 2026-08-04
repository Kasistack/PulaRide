package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_history")
data class RideHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pickup: String,
    val dropoff: String,
    val fare: Double,
    val paymentMethod: String,
    val timestamp: Long,
    val driverName: String,
    val driverVehicle: String,
    val status: String, // "COMPLETED", "CANCELLED"
    val rating: Float
)
