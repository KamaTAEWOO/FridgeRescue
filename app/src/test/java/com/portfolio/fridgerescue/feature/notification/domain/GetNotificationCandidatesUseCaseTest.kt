package com.portfolio.fridgerescue.feature.notification.domain

import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNotificationCandidatesUseCaseTest {
    private val today = LocalDate.of(2026, 9, 1)
    private val useCase = GetNotificationCandidatesUseCase()

    @Test
    fun `TC-NOTIFY-001 overdue D3 D1 and today are selected`() {
        val items = listOf(3L, 2L, 1L, 0L, -1L).map { days -> food("food-$days", days) }

        val result = useCase(items, today)

        assertEquals(listOf("food--1", "food-0", "food-1", "food-3"), result.map { it.name })
    }

    @Test
    fun `TC-ACTION-006 finalized items are excluded from alerts`() {
        val items = listOf(
            food("active", 1, FoodStatus.ACTIVE),
            food("consumed", 1, FoodStatus.CONSUMED),
            food("discarded", 1, FoodStatus.DISCARDED),
            food("archived", 1, FoodStatus.ARCHIVED),
        )

        assertEquals(listOf("active"), useCase(items, today).map { it.name })
    }

    @Test
    fun `TC-DATE-003 item without date never creates a notification candidate`() {
        val item = FoodItem(
            id = FoodItemId("no-date"),
            name = "날짜 미정",
            storageLocation = StorageLocation.REFRIGERATED,
            status = FoodStatus.NEEDS_REVIEW,
        )

        assertTrue(useCase(listOf(item), today).isEmpty())
    }

    private fun food(name: String, days: Long, status: FoodStatus = FoodStatus.ACTIVE) = FoodItem(
        id = FoodItemId(name),
        name = name,
        storageLocation = StorageLocation.REFRIGERATED,
        dates = listOf(FoodDate(today.plusDays(days), FoodDateSource.APP_ESTIMATED)),
        status = status,
    )
}
