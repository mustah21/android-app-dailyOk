package com.example.dailyok.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MedicationEntity::class, TakenRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile private var INSTANCE: MedicationDatabase? = null

        fun get(context: Context): MedicationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicationDatabase::class.java,
                    "medication.db",
                ).build().also { INSTANCE = it }
            }
    }
}
