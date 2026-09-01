package com.portfolio.fridgerescue

import android.app.Application
import com.portfolio.fridgerescue.core.data.database.FridgeRescueDatabase
import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.data.repository.RoomFoodRepository

class FridgeRescueApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}

interface AppContainer {
    val foodRepository: FoodRepository
}

private class DefaultAppContainer(
    application: Application,
) : AppContainer {
    private val database: FridgeRescueDatabase by lazy {
        FridgeRescueDatabase.create(application)
    }

    override val foodRepository: FoodRepository by lazy {
        RoomFoodRepository(database.foodItemDao())
    }
}
