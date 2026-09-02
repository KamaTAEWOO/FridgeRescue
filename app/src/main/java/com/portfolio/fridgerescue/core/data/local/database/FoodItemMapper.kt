package com.portfolio.fridgerescue.core.data.local.database

import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodEvent
import com.portfolio.fridgerescue.core.domain.model.FoodEventType
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate

fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = FoodItemId(id),
    name = name,
    quantity = quantity,
    storageLocation = storageLocation.toStorageLocation(),
    dates = buildList {
        manufacturerDisplayedDate.toLocalDateOrNull()?.let {
            add(FoodDate(it, FoodDateSource.MANUFACTURER_DISPLAYED))
        }
        appEstimatedDate.toLocalDateOrNull()?.let {
            add(FoodDate(it, FoodDateSource.APP_ESTIMATED))
        }
        userConfirmedDate.toLocalDateOrNull()?.let {
            add(FoodDate(it, FoodDateSource.USER_CONFIRMED))
        }
    },
    isOpened = isOpened,
    isPinned = isPinned,
    status = status.toFoodStatus(),
)

fun FoodItem.toEntity(updatedAtEpochMillis: Long): FoodItemEntity = FoodItemEntity(
    id = id.value,
    name = name,
    quantity = quantity,
    storageLocation = storageLocation.name,
    manufacturerDisplayedDate = dates
        .firstOrNull { it.source == FoodDateSource.MANUFACTURER_DISPLAYED }
        ?.value
        ?.toString(),
    appEstimatedDate = dates
        .firstOrNull { it.source == FoodDateSource.APP_ESTIMATED }
        ?.value
        ?.toString(),
    userConfirmedDate = dates
        .firstOrNull { it.source == FoodDateSource.USER_CONFIRMED }
        ?.value
        ?.toString(),
    isOpened = isOpened,
    isPinned = isPinned,
    status = status.name,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun FoodEventEntity.toDomain(): FoodEvent = FoodEvent(
    id = eventId,
    operationId = operationId,
    foodItemId = FoodItemId(foodItemId),
    type = enumValues<FoodEventType>().firstOrNull { it.name == type } ?: FoodEventType.UPDATED,
    previousStatus = previousStatus?.toFoodStatus(),
    newStatus = newStatus?.toFoodStatus(),
    discardReason = discardReason,
    occurredAt = java.time.Instant.ofEpochMilli(occurredAtEpochMillis),
    revertsEventId = revertsEventId,
)

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun String.toStorageLocation(): StorageLocation =
    enumValues<StorageLocation>().firstOrNull { it.name == this }
        ?: StorageLocation.REFRIGERATED

private fun String.toFoodStatus(): FoodStatus =
    enumValues<FoodStatus>().firstOrNull { it.name == this }
        ?: FoodStatus.NEEDS_REVIEW
