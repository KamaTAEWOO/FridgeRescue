package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodEvent
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    val foodItems: Flow<List<FoodItem>>

    suspend fun findById(id: FoodItemId): FoodItem?

    suspend fun upsert(foodItem: FoodItem)

    suspend fun save(foodItem: FoodItem, operationId: String): FoodMutationResult

    suspend fun saveAll(foodItems: List<FoodItem>, operationId: String): Int

    suspend fun performAction(request: FoodActionRequest): FoodMutationResult

    suspend fun undo(eventId: String, operationId: String): FoodMutationResult

    fun observeEvents(foodItemId: FoodItemId): Flow<List<FoodEvent>>
}
