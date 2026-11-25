package com.example.dailyok.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_events")
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String,
)