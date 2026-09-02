package com.portfolio.fridgerescue.debug

import com.portfolio.fridgerescue.core.data.database.FoodEventEntity
import com.portfolio.fridgerescue.core.data.database.FoodItemEntity
import com.portfolio.fridgerescue.core.model.FoodEventType
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
import java.time.LocalDate
import java.time.ZoneOffset

data class DemoDataSet(
    val foodItems: List<FoodItemEntity>,
    val events: List<FoodEventEntity>,
)

object DemoDataFixtures {
    const val ID_PREFIX = "demo-"

    fun create(today: LocalDate = LocalDate.now()): DemoDataSet {
        val updatedAt = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val activeItems = listOf(
            item("active-spinach", "시금치", 1, StorageLocation.REFRIGERATED, today.minusDays(1), updatedAt),
            item("active-mushroom", "양송이버섯", 1, StorageLocation.REFRIGERATED, today, updatedAt, isOpened = true),
            item("active-milk", "우유", 1, StorageLocation.REFRIGERATED, today.plusDays(1), updatedAt, isOpened = true, isPinned = true),
            item("active-tofu", "두부", 2, StorageLocation.REFRIGERATED, today.plusDays(2), updatedAt, confirmed = true),
            item("active-yogurt", "플레인 요거트", 4, StorageLocation.REFRIGERATED, today.plusDays(3), updatedAt),
            item("active-tomato", "토마토", 4, StorageLocation.ROOM_TEMPERATURE, today.plusDays(5), updatedAt, estimated = true),
            item("active-eggs", "달걀", 10, StorageLocation.REFRIGERATED, today.plusDays(7), updatedAt, confirmed = true, isPinned = true),
            item("active-cheese", "슬라이스 치즈", 1, StorageLocation.REFRIGERATED, today.plusDays(14), updatedAt),
            item("active-chicken", "닭가슴살", 3, StorageLocation.FROZEN, today.plusDays(45), updatedAt),
            item("active-apple", "사과", 3, StorageLocation.ROOM_TEMPERATURE, null, updatedAt),
            item(
                suffix = "review-side-dish",
                name = "수제 반찬",
                quantity = 1,
                location = StorageLocation.REFRIGERATED,
                date = null,
                updatedAt = updatedAt,
                status = FoodStatus.NEEDS_REVIEW,
            ),
        )
        val historicalItems = listOf(
            item("history-broccoli", "브로콜리", 1, StorageLocation.REFRIGERATED, today.minusDays(7), updatedAt, status = FoodStatus.CONSUMED),
            item("history-banana", "바나나", 2, StorageLocation.ROOM_TEMPERATURE, today.minusDays(6), updatedAt, status = FoodStatus.CONSUMED),
            item("history-sprouts", "콩나물", 1, StorageLocation.REFRIGERATED, today.minusDays(4), updatedAt, status = FoodStatus.CONSUMED),
            item("history-mackerel", "고등어", 1, StorageLocation.FROZEN, today.minusDays(2), updatedAt, status = FoodStatus.CONSUMED),
            item("history-mushroom-1", "양송이버섯", 1, StorageLocation.REFRIGERATED, today.minusDays(8), updatedAt, status = FoodStatus.DISCARDED),
            item("history-mushroom-2", "양송이버섯", 1, StorageLocation.REFRIGERATED, today.minusDays(3), updatedAt, status = FoodStatus.DISCARDED),
            item("history-lettuce", "상추", 1, StorageLocation.REFRIGERATED, today.minusDays(5), updatedAt, status = FoodStatus.DISCARDED),
        )
        val events = historicalItems.mapIndexed { index, food ->
            val type = if (food.status == FoodStatus.CONSUMED.name) FoodEventType.CONSUMED else FoodEventType.DISCARDED
            FoodEventEntity(
                eventId = "$ID_PREFIX${food.id.removePrefix(ID_PREFIX)}-event",
                operationId = "$ID_PREFIX${food.id.removePrefix(ID_PREFIX)}-operation",
                foodItemId = food.id,
                type = type.name,
                previousStatus = FoodStatus.ACTIVE.name,
                newStatus = food.status,
                discardReason = if (type == FoodEventType.DISCARDED) "상해서 폐기" else null,
                occurredAtEpochMillis = updatedAt - ((historicalItems.size - index).toLong() * DAY_MILLIS),
                revertsEventId = null,
            )
        }
        return DemoDataSet(activeItems + historicalItems, events)
    }

    private fun item(
        suffix: String,
        name: String,
        quantity: Int?,
        location: StorageLocation,
        date: LocalDate?,
        updatedAt: Long,
        estimated: Boolean = false,
        confirmed: Boolean = false,
        isOpened: Boolean = false,
        isPinned: Boolean = false,
        status: FoodStatus = FoodStatus.ACTIVE,
    ) = FoodItemEntity(
        id = "$ID_PREFIX$suffix",
        name = name,
        quantity = quantity,
        storageLocation = location.name,
        manufacturerDisplayedDate = date?.takeUnless { estimated || confirmed }?.toString(),
        appEstimatedDate = date?.takeIf { estimated }?.toString(),
        userConfirmedDate = date?.takeIf { confirmed }?.toString(),
        isOpened = isOpened,
        isPinned = isPinned,
        status = status.name,
        updatedAtEpochMillis = updatedAt,
    )

    private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
}
