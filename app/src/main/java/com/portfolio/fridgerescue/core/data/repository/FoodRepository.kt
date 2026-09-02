package com.portfolio.fridgerescue.core.data.repository

import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodActionRequest
import com.portfolio.fridgerescue.core.model.FoodEvent
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodMutationResult
import kotlinx.coroutines.flow.Flow

/**
 * 식재료 현재 상태와 행동 이력을 제공하는 데이터 계층 경계다.
 *
 * 읽기는 Room 변경을 계속 전달하는 [Flow]로 노출하고, 쓰기는 완료 시점을 명확히 알 수 있도록
 * `suspend` 함수로 제공한다. `operationId`가 있는 작업은 재전달돼도 한 번만 적용되어야 한다.
 */
interface FoodRepository {
    val foodItems: Flow<List<FoodItem>>
    val events: Flow<List<FoodEvent>>

    suspend fun findById(id: FoodItemId): FoodItem?

    suspend fun upsert(foodItem: FoodItem)

    suspend fun save(foodItem: FoodItem, operationId: String): FoodMutationResult

    suspend fun saveAll(foodItems: List<FoodItem>, operationId: String): Int

    suspend fun performAction(request: FoodActionRequest): FoodMutationResult

    suspend fun undo(eventId: String, operationId: String): FoodMutationResult

    fun observeEvents(foodItemId: FoodItemId): Flow<List<FoodEvent>>
}
