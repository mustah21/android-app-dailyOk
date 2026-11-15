package com.example.dailyok

import android.content.Context
import com.example.dailyok.data.repository.DataStoreActivityRepository
import com.example.dailyok.data.repository.activityDataStore
import com.example.dailyok.domain.interfaces.ActivityRepository
import com.example.dailyok.domain.interfaces.ActivitySource
import com.example.dailyok.domain.useCase.SyncLastActivity
import com.example.dailyok.platform.usage.UsageActivitySource

class AppContainer(context: Context) {
    val activitySource: ActivitySource = UsageActivitySource(context)
    val activityRepository: ActivityRepository =
        DataStoreActivityRepository(context.activityDataStore)
    val syncLastActivity = SyncLastActivity(activitySource, activityRepository)
}