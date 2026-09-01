package com.portfolio.fridgerescue.core.data.database

import com.portfolio.fridgerescue.core.model.FoodDate
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.FoodItem
import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.FoodStatus
import com.portfolio.fridgerescue.core.model.StorageLocation
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

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun String.toStorageLocation(): StorageLocation =
    enumValues<StorageLocation>().firstOrNull { it.name == this }
        ?: StorageLocation.REFRIGERATED

private fun String.toFoodStatus(): FoodStatus =
    enumValues<FoodStatus>().firstOrNull { it.name == this }
        ?: FoodStatus.NEEDS_REVIEW
