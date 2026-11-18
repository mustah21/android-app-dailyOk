package com.example.dailyok.data.repository


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dailyok.domain.interfaces.ActivityRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.activityDataStore: DataStore<Preferences> by preferencesDataStore(name = "activity")

class DataStoreActivityRepository(
    private val store: DataStore<Preferences>,
) : ActivityRepository {

    private val key = longPreferencesKey("last_activity")

    override val lastActivity: Flow<Instant?> =
        store.data.map { prefs -> prefs[key]?.let(Instant::ofEpochMilli) }

    override suspend fun save(time: Instant) {
        store.edit { it[key] = time.toEpochMilli() }
    }


}