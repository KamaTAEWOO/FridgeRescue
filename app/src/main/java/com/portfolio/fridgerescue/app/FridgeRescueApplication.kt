package com.portfolio.fridgerescue.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.portfolio.fridgerescue.feature.notification.worker.ExpiryNotificationWorker
import com.portfolio.fridgerescue.feature.intake.data.SharedIntakeCacheCleaner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 앱 전역 Hilt 그래프의 시작점이다.
 *
 * WorkManager도 Hilt가 만든 WorkerFactory를 사용해야 Worker 생성자에 Repository와
 * UseCase를 안전하게 전달할 수 있다.
 */
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
