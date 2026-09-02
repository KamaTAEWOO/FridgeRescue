package com.portfolio.fridgerescue.feature.rescue.presentation

import com.portfolio.fridgerescue.core.domain.model.FoodActionType
import com.portfolio.fridgerescue.core.domain.model.FoodEvent
import com.portfolio.fridgerescue.core.domain.model.FoodItem
import com.portfolio.fridgerescue.core.domain.model.FoodItemId
import com.portfolio.fridgerescue.core.domain.model.IntakeDraft
import com.portfolio.fridgerescue.core.domain.model.IntakeCandidate
import com.portfolio.fridgerescue.core.domain.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.domain.FoodItemDraftError
import com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem
import com.portfolio.fridgerescue.feature.rescue.domain.PantryFilter
import com.portfolio.fridgerescue.feature.rescue.domain.PantryStatusFilter

sealed interface RescueUiState {
    data object Loading : RescueUiState

    data class Content(
        val items: List<RescueQueueItem>,
        val notificationItems: List<RescueQueueItem> = emptyList(),
        val totalItemCount: Int = items.size,
        val pantryFilter: PantryFilter = PantryFilter(),
        val urgentCount: Int,
        val needsReviewCount: Int,
        val editor: FoodEditorUiState? = null,
        val detail: FoodDetailUiState? = null,
        val intakeReview: IntakeReviewUiState? = null,
        val showImportOptions: Boolean = false,
    ) : RescueUiState
}

data class IntakeReviewUiState(
    val draft: IntakeDraft,
    val candidates: List<IntakeCandidate>,
    val isSaving: Boolean = false,
    val duplicateCandidateIds: Set<String> = emptySet(),
) {
    val selectedCount: Int get() = candidates.count { it.isSelected }
}

data class FoodDetailUiState(
    val foodItem: FoodItem,
    val events: List<FoodEvent>,
    val discardReason: String = "",
    val actionInProgress: Boolean = false,
)

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
    data class OpenFoodActions(val foodItemId: FoodItemId) : RescueAction
    data object DismissFoodActions : RescueAction
    data class RecordFoodAction(
        val foodItemId: FoodItemId,
        val type: FoodActionType,
    ) : RescueAction
    data class ChangeDiscardReason(val value: String) : RescueAction
    data class UndoMutation(val eventId: String) : RescueAction
    data class DismissIntakeDraft(val draftId: String) : RescueAction
    data class ToggleIntakeCandidate(val candidateId: String, val selected: Boolean) : RescueAction
    data class UpdateIntakeCandidate(
        val candidateId: String,
        val name: String,
        val quantity: String,
    ) : RescueAction
    data class SaveIntakeCandidates(val draftId: String) : RescueAction
    data object OpenImportOptions : RescueAction
    data object DismissImportOptions : RescueAction
    data class ChangePantrySearch(val value: String) : RescueAction
    data class FilterPantryStorage(val storageLocation: StorageLocation?) : RescueAction
    data class FilterPantryStatus(val status: PantryStatusFilter) : RescueAction
    data object ClearPantryFilters : RescueAction
    data class StartManualFromIntake(val draftId: String) : RescueAction
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
    data class ShowMutationUndo(
        val eventId: String,
        val foodName: String,
        val actionType: FoodActionType,
    ) : RescueEvent

    data class ShowFoodSaved(
        val foodName: String,
        val isNew: Boolean,
    ) : RescueEvent

    data class ShowBatchSaved(val count: Int) : RescueEvent
    data object ShowDataDeleted : RescueEvent
    data object ShowDataDeletionFailed : RescueEvent
}
