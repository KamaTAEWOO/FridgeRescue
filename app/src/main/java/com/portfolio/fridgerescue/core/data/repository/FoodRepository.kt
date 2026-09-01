package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    val foodItems: Flow<List<FoodItem>>

    suspend fun findById(id: FoodItemId): FoodItem?

    suspend fun upsert(foodItem: FoodItem)
}
