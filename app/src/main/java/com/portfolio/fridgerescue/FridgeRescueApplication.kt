package com.portfolio.fridgerescue

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.portfolio.fridgerescue.feature.notification.ExpiryNotificationWorker
import com.portfolio.fridgerescue.feature.intake.SharedIntakeCacheCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FridgeRescueApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SharedIntakeCacheCleaner.clean(cacheDir)
        ExpiryNotificationWorker.schedule(this)
    }
}
