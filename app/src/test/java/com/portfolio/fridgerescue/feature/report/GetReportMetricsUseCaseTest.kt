package com.portfolio.fridgerescue.feature.report

import com.portfolio.fridgerescue.core.model.FoodEvent
import com.portfolio.fridgerescue.core.model.FoodEventType
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetReportMetricsUseCaseTest {
    private val useCase = GetReportMetricsUseCase()

    @Test
    fun `TC-REPORT-001 counts consumed and discarded events`() {
        val metrics = useCase(
            listOf(food("a", "두부"), food("b", "우유")),
            listOf(event("consume", "a", FoodEventType.CONSUMED), event("discard", "b", FoodEventType.DISCARDED)),
        )

        assertEquals(1, metrics.rescuedCount)
        assertEquals(1, metrics.discardedCount)
        assertNull(metrics.shoppingHint)
    }

    @Test
    fun `TC-REPORT-002 undone terminal action is excluded`() {
        val consumed = event("consume", "a", FoodEventType.CONSUMED)
        val undo = event("undo", "a", FoodEventType.UNDONE, revertsEventId = consumed.id)

        val metrics = useCase(listOf(food("a", "두부")), listOf(consumed, undo))

        assertEquals(0, metrics.rescuedCount)
    }

    @Test
    fun `TC-REPORT-003 repeated discard produces shopping hint without money estimate`() {
        val metrics = useCase(
            listOf(food("a", "우유"), food("b", "우유")),
            listOf(event("d1", "a", FoodEventType.DISCARDED), event("d2", "b", FoodEventType.DISCARDED)),
        )

        assertTrue(metrics.shoppingHint?.contains("우유") == true)
    }

    private fun food(id: String, name: String) = FoodItem(
        id = FoodItemId(id),
        name = name,
        storageLocation = StorageLocation.REFRIGERATED,
        status = FoodStatus.DISCARDED,
    )

    private fun event(
        id: String,
        foodId: String,
        type: FoodEventType,
        revertsEventId: String? = null,
    ) = FoodEvent(
        id = id,
        operationId = "operation-$id",
        foodItemId = FoodItemId(foodId),
        type = type,
        previousStatus = FoodStatus.ACTIVE,
        newStatus = if (type == FoodEventType.CONSUMED) FoodStatus.CONSUMED else FoodStatus.DISCARDED,
        occurredAt = Instant.EPOCH,
        revertsEventId = revertsEventId,
    )
}
