package com.portfolio.fridgerescue.feature.notification.domain

import com.portfolio.fridgerescue.core.domain.model.FoodEvent
import com.portfolio.fridgerescue.core.domain.model.FoodEventType
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetStaleFoodCandidatesUseCaseTest {
    private val now = Instant.parse("2026-09-02T12:00:00Z")
    private val useCase = GetStaleFoodCandidatesUseCase()

    @Test
    fun TC_ACTION_007_active_item_without_response_for_over_seven_days_needs_review() {
        val food = food("old", FoodStatus.ACTIVE)
        val event = event(food.id, Instant.parse("2026-08-25T11:59:59Z"))

        assertEquals(listOf(food), useCase(listOf(food), listOf(event), now))
    }

    @Test
    fun TC_ACTION_007_recent_or_already_reviewing_items_are_unchanged() {
        val recent = food("recent", FoodStatus.ACTIVE)
        val reviewing = food("reviewing", FoodStatus.NEEDS_REVIEW)
        val events = listOf(
            event(recent.id, Instant.parse("2026-09-01T12:00:00Z")),
            event(reviewing.id, Instant.parse("2026-08-01T12:00:00Z")),
        )

        assertTrue(useCase(listOf(recent, reviewing), events, now).isEmpty())
    }

    private fun food(id: String, status: FoodStatus) = FoodItem(
        id = FoodItemId(id),
        name = id,
        storageLocation = StorageLocation.REFRIGERATED,
        status = status,
    )

    private fun event(foodItemId: FoodItemId, occurredAt: Instant) = FoodEvent(
        id = "event-${foodItemId.value}",
        operationId = "operation-${foodItemId.value}",
        foodItemId = foodItemId,
        type = FoodEventType.CREATED,
        previousStatus = null,
        newStatus = FoodStatus.ACTIVE,
        occurredAt = occurredAt,
    )
}
