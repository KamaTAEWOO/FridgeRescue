package com.portfolio.fridgerescue

import android.app.Application
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.IntakeDraftRepository
import com.portfolio.fridgerescue.core.data.repository.RoomIntakeDraftRepository
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository

class FridgeRescueApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}

interface AppContainer {
    val foodRepository: FoodRepository
    val intakeDraftRepository: IntakeDraftRepository
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
}
