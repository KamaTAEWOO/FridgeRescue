package com.portfolio.fridgerescue.feature.rescue.domain

import com.portfolio.fridgerescue.core.domain.repository.FoodRepository
import com.portfolio.fridgerescue.core.domain.model.FoodDate
import com.portfolio.fridgerescue.core.domain.model.FoodDateSource
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.FoodStatus
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

data class FoodItemDraft(
    val foodItemId: FoodItemId?,
    val nameInput: String,
    val quantityInput: String,
    val dateInput: String,
    val storageLocation: StorageLocation,
    val isOpened: Boolean,
    val isPinned: Boolean,
)

enum class FoodItemDraftError {
    NAME_REQUIRED,
    INVALID_QUANTITY,
    INVALID_DATE,
}

sealed interface SaveFoodItemResult {
    data class Saved(
        val foodItem: FoodItem,
        val isNew: Boolean,
    ) : SaveFoodItemResult

    data class Invalid(val error: FoodItemDraftError) : SaveFoodItemResult
}

class SaveFoodItemUseCase(
    private val repository: FoodRepository,
    private val operationIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val idFactory: () -> FoodItemId = { FoodItemId(UUID.randomUUID().toString()) },
) {
    suspend operator fun invoke(draft: FoodItemDraft): SaveFoodItemResult {
        val name = draft.nameInput.trim()
        if (name.isEmpty()) {
            return SaveFoodItemResult.Invalid(FoodItemDraftError.NAME_REQUIRED)
        }

        val quantity = draft.quantityInput.trim().takeIf(String::isNotEmpty)?.toIntOrNull()
        if (draft.quantityInput.isNotBlank() && (quantity == null || quantity <= 0)) {
            return SaveFoodItemResult.Invalid(FoodItemDraftError.INVALID_QUANTITY)
        }

        val date = try {
            draft.dateInput.trim().takeIf(String::isNotEmpty)?.let(LocalDate::parse)
        } catch (_: DateTimeParseException) {
            return SaveFoodItemResult.Invalid(FoodItemDraftError.INVALID_DATE)
        }

        val existingItem = draft.foodItemId?.let { repository.findById(it) }
        val dates = if (date == null) {
            emptyList()
        } else {
            existingItem
                ?.dates
                .orEmpty()
                .filterNot { it.source == FoodDateSource.USER_CONFIRMED } +
                FoodDate(date, FoodDateSource.USER_CONFIRMED)
        }
        val status = when {
            existingItem?.isFinalized == true -> existingItem.status
            date == null -> FoodStatus.NEEDS_REVIEW
            else -> FoodStatus.ACTIVE
        }
        val foodItem = FoodItem(
            id = existingItem?.id ?: idFactory(),
            name = name,
            quantity = quantity,
            storageLocation = draft.storageLocation,
            dates = dates,
            isOpened = draft.isOpened,
            isPinned = draft.isPinned,
            status = status,
        )

        repository.save(foodItem, operationIdFactory())
        return SaveFoodItemResult.Saved(
            foodItem = foodItem,
            isNew = existingItem == null,
        )
    }
}
