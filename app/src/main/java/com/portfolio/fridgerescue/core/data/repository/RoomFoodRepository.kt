package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.data.database.FoodItemDao
import com.portfolio.fridgerescue.core.data.database.toDomain
import com.portfolio.fridgerescue.core.data.database.toEntity
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFoodRepository(
    private val foodItemDao: FoodItemDao,
    private val clock: Clock = Clock.systemUTC(),
) : FoodRepository {
    override val foodItems: Flow<List<FoodItem>> = foodItemDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    override suspend fun findById(id: FoodItemId): FoodItem? =
        foodItemDao.findById(id.value)?.toDomain()

    override suspend fun upsert(foodItem: FoodItem) {
        foodItemDao.upsert(foodItem.toEntity(clock.millis()))
    }
}
