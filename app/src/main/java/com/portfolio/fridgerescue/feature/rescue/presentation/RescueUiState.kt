package com.portfolio.fridgerescue.feature.rescue.presentation

import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.domain.FoodItemDraftError
import com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem

sealed interface RescueUiState {
    data object Loading : RescueUiState

    data class Content(
        val items: List<RescueQueueItem>,
        val urgentCount: Int,
        val needsReviewCount: Int,
        val editor: FoodEditorUiState? = null,
    ) : RescueUiState
}

data class FoodEditorUiState(
    val foodItemId: FoodItemId? = null,
    val name: String = "",
    val quantity: String = "",
    val date: String = "",
    val storageLocation: StorageLocation = StorageLocation.REFRIGERATED,
    val isOpened: Boolean = false,
    val isPinned: Boolean = false,
    val error: FoodItemDraftError? = null,
    val saveFailed: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = foodItemId != null
}

sealed interface RescueAction {
    data class MarkConsumed(val foodItemId: FoodItemId) : RescueAction
    data class UndoConsumed(val foodItemId: FoodItemId) : RescueAction
    data object StartAddFood : RescueAction
    data class StartEditFood(val foodItemId: FoodItemId) : RescueAction
    data object DismissEditor : RescueAction
    data class ChangeEditorName(val value: String) : RescueAction
    data class ChangeEditorQuantity(val value: String) : RescueAction
    data class ChangeEditorDate(val value: String) : RescueAction
    data class ChangeEditorStorage(val value: StorageLocation) : RescueAction
    data class ChangeEditorOpened(val value: Boolean) : RescueAction
    data class ChangeEditorPinned(val value: Boolean) : RescueAction
    data object SaveEditor : RescueAction
}

sealed interface RescueEvent {
    data class ShowConsumedUndo(
        val foodItemId: FoodItemId,
        val foodName: String,
    ) : RescueEvent

    data class ShowFoodSaved(
        val foodName: String,
        val isNew: Boolean,
    ) : RescueEvent
}
