package com.example.dailyok.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.dailyok.data.db.ActivityEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ActivityEventDao {
    @Insert
    abstract suspend fun insert(event: ActivityEventEntity)

    @Query(
        "DELETE FROM activity_events WHERE id NOT IN " +
                "(SELECT id FROM activity_events ORDER BY timestamp DESC, id DESC LIMIT :keep)"
    )
    abstract suspend fun trim(keep: Int)

    @Transaction
    open suspend fun insertAndTrim(event: ActivityEventEntity, keep: Int = 10) {
        insert(event)
        trim(keep)
    }

    @Query("SELECT * FROM activity_events ORDER BY timestamp DESC LIMIT 5")
    abstract fun observeLatest(): Flow<List<ActivityEventEntity>>
}