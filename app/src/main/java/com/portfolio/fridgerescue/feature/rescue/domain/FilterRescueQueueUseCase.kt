package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation

enum class PantryStatusFilter { ALL, ACTIVE, NEEDS_REVIEW }

data class PantryFilter(
    val query: String = "",
    val storageLocation: StorageLocation? = null,
    val status: PantryStatusFilter = PantryStatusFilter.ALL,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || storageLocation != null || status != PantryStatusFilter.ALL
}

class FilterRescueQueueUseCase {
    operator fun invoke(
        items: List<RescueQueueItem>,
        filter: PantryFilter,
    ): List<RescueQueueItem> {
        val query = filter.query.trim().lowercase()
        return items.filter { item ->
            (query.isEmpty() || item.foodItem.name.lowercase().contains(query)) &&
                (filter.storageLocation == null ||
                    item.foodItem.storageLocation == filter.storageLocation) &&
                when (filter.status) {
                    PantryStatusFilter.ALL -> true
                    PantryStatusFilter.ACTIVE -> item.foodItem.status == FoodStatus.ACTIVE
                    PantryStatusFilter.NEEDS_REVIEW ->
                        item.foodItem.status == FoodStatus.NEEDS_REVIEW ||
                            item.urgency == RescueUrgency.NEEDS_DATE
                }
        }
    }
}
