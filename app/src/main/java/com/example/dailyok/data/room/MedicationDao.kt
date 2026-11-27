package com.example.dailyok.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications")
    fun observeMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications")
    suspend fun getMedications(): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedication(entity: MedicationEntity)

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedication(id: String)

    @Query("SELECT * FROM taken_records WHERE date >= :sinceDate")
    suspend fun getTakenSince(sinceDate: String): List<TakenRecordEntity>

    @Query("SELECT * FROM taken_records WHERE date >= :sinceDate")
    fun observeTakenSince(sinceDate: String): Flow<List<TakenRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaken(entity: TakenRecordEntity)

    @Query("DELETE FROM taken_records WHERE medicationId = :medicationId AND date = :date AND scheduledTime = :scheduledTime")
    suspend fun deleteTaken(medicationId: String, date: String, scheduledTime: String)

    @Query("DELETE FROM taken_records WHERE date < :sinceDate")
    suspend fun pruneOldTaken(sinceDate: String)
}
