package com.example.dailyok.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dose: String,
    val timesJson: String, // JSON array of "HH:mm" strings
)

@Entity(tableName = "taken_records", primaryKeys = ["medicationId", "date", "scheduledTime"])
data class TakenRecordEntity(
    val medicationId: String,
    val date: String,          // ISO date "YYYY-MM-DD"
    val scheduledTime: String, // "HH:mm"
    val takenAt: String,       // ISO datetime
)
