package com.portfolio.fridgerescue.core.data.datasource.local

import com.portfolio.fridgerescue.core.data.repository.FoodRepository
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryFoodRepository(
    initialItems: List<FoodItem> = emptyList(),
) : FoodRepository {
    private val items = MutableStateFlow(initialItems)

    override val foodItems: Flow<List<FoodItem>> = items.asStateFlow()

    override suspend fun findById(id: FoodItemId): FoodItem? =
        items.value.firstOrNull { it.id == id }

    override suspend fun upsert(foodItem: FoodItem) {
        items.update { currentItems ->
            val currentIndex = currentItems.indexOfFirst { it.id == foodItem.id }
            if (currentIndex == -1) {
                currentItems + foodItem
            } else {
                currentItems.toMutableList().apply {
                    set(currentIndex, foodItem)
                }
            }
        }
    }
}
