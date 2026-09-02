package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterRescueQueueUseCaseTest {
    private val today = LocalDate.of(2026, 9, 2)
    private val filter = FilterRescueQueueUseCase()

    @Test
    fun TC_PANTRY_002_query_location_and_status_are_combined() {
        val queue = GetRescueQueueUseCase()(
            listOf(
                food("냉동 두부", StorageLocation.FROZEN, FoodStatus.ACTIVE),
                food("냉장 두부", StorageLocation.REFRIGERATED, FoodStatus.ACTIVE),
                food("냉동 만두", StorageLocation.FROZEN, FoodStatus.NEEDS_REVIEW),
            ),
            today,
        )

        val result = filter(
            queue,
            PantryFilter(
                query = "두부",
                storageLocation = StorageLocation.FROZEN,
                status = PantryStatusFilter.ACTIVE,
            ),
        )

        assertEquals(listOf("냉동 두부"), result.map { it.foodItem.name })
    }

    @Test
    fun TC_PANTRY_002_needs_review_includes_missing_date() {
        val queue = GetRescueQueueUseCase()(
            listOf(food("날짜 없는 우유", StorageLocation.REFRIGERATED, FoodStatus.ACTIVE)),
            today,
        )

        assertEquals(
            listOf("날짜 없는 우유"),
            filter(queue, PantryFilter(status = PantryStatusFilter.NEEDS_REVIEW))
                .map { it.foodItem.name },
        )
    }

    private fun food(name: String, location: StorageLocation, status: FoodStatus) = FoodItem(
        id = FoodItemId(name),
        name = name,
        storageLocation = location,
        status = status,
    )
}
