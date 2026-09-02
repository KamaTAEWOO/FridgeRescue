package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRescueQueueUseCaseTest {
    private val today = LocalDate.of(2026, 9, 1)
    private val getRescueQueue = GetRescueQueueUseCase()

    @Test
    fun `TC-QUEUE-001 same urgency sorts opened then pinned items first`() {
        val regular = foodItem(id = "regular", name = "일반", date = today)
        val pinned = foodItem(id = "pinned", name = "고정", date = today, isPinned = true)
        val opened = foodItem(id = "opened", name = "개봉", date = today, isOpened = true)

        val result = getRescueQueue(listOf(regular, pinned, opened), today)

        assertEquals(listOf("opened", "pinned", "regular"), result.map { it.foodItem.id.value })
    }

    @Test
    fun `TC-QUEUE-002 finalized items are excluded`() {
        val items = listOf(
            foodItem(id = "active", name = "활성", date = today),
            foodItem(id = "consumed", name = "먹음", date = today, status = FoodStatus.CONSUMED),
            foodItem(id = "discarded", name = "버림", date = today, status = FoodStatus.DISCARDED),
            foodItem(id = "archived", name = "보관", date = today, status = FoodStatus.ARCHIVED),
        )

        val result = getRescueQueue(items, today)

        assertEquals(listOf("active"), result.map { it.foodItem.id.value })
    }

    @Test
    fun `TC-DATE-003 item without date needs review and has no D-day`() {
        val dated = foodItem(id = "dated", name = "날짜 있음", date = today.plusDays(7))
        val noDate = foodItem(id = "no-date", name = "날짜 없음", date = null)

        val result = getRescueQueue(listOf(noDate, dated), today)

        assertEquals("no-date", result.last().foodItem.id.value)
        assertEquals(RescueUrgency.NEEDS_DATE, result.last().urgency)
        assertNull(result.last().daysRemaining)
    }

    @Test
    fun `TC-DATE-005 past date remains actionable and is marked overdue`() {
        val foodItem = foodItem(
            id = "past",
            name = "지난 두부",
            date = today.minusDays(2),
        )

        val result = getRescueQueue(listOf(foodItem), today).single()

        assertEquals(RescueUrgency.OVERDUE, result.urgency)
        assertEquals(-2L, result.daysRemaining)
    }

    private fun foodItem(
        id: String,
        name: String,
        date: LocalDate?,
        status: FoodStatus = FoodStatus.ACTIVE,
        isOpened: Boolean = false,
        isPinned: Boolean = false,
    ) = FoodItem(
        id = FoodItemId(id),
        name = name,
        storageLocation = StorageLocation.REFRIGERATED,
        dates = date?.let {
            listOf(FoodDate(it, FoodDateSource.MANUFACTURER_DISPLAYED))
        }.orEmpty(),
        isOpened = isOpened,
        isPinned = isPinned,
        status = status,
    )
}
