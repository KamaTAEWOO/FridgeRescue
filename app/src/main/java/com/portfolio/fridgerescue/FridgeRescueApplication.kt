package com.portfolio.fridgerescue

import android.app.Application
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.feature.notification.ExpiryNotificationWorker
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository
import com.portfolio.fridgerescue.feature.intake.SharedIntakeCacheCleaner
import com.portfolio.fridgerescue.feature.notification.DataStoreNotificationSettingsRepository
import com.portfolio.fridgerescue.feature.notification.NotificationSettingsRepository

class FridgeRescueApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        SharedIntakeCacheCleaner.clean(cacheDir)
        ExpiryNotificationWorker.schedule(this)
    }
}

interface AppContainer {
    val foodRepository: FoodRepository
    val intakeDraftRepository: IntakeDraftRepository
    val notificationSettingsRepository: NotificationSettingsRepository
}

private class DefaultAppContainer(
    application: Application,
) : AppContainer {
    private val database: FridgeRescueDatabase by lazy {
        FridgeRescueDatabase.create(application)
    }

    override val foodRepository: FoodRepository by lazy {
        RoomFoodRepository(
            database = database,
            foodItemDao = database.foodItemDao(),
            foodEventDao = database.foodEventDao(),
        )
    }

    override val intakeDraftRepository: IntakeDraftRepository by lazy {
        RoomIntakeDraftRepository(
            dao = database.intakeDraftDao(),
            candidateDao = database.intakeCandidateDao(),
        )
    }

    override val notificationSettingsRepository: NotificationSettingsRepository by lazy {
        DataStoreNotificationSettingsRepository(application)
    }
}
