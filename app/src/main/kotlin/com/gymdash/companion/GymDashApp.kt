package com.gymdash.companion

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gymdash.companion.data.local.datastore.SyncPreferences
import com.gymdash.companion.worker.HealthSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymDashApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferences: SyncPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val autoSync = preferences.autoSyncEnabled.first()
            if (autoSync) {
                val interval = preferences.syncIntervalMinutes.first()
                HealthSyncWorker.enqueue(this@GymDashApp, interval)
            }
        }
    }
}
