package com.portfolio.fridgerescue.feature.rescue.presentation

import com.portfolio.fridgerescue.core.model.FoodItemId
import com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem

sealed interface RescueUiState {
    data object Loading : RescueUiState

    data class Content(
        val items: List<RescueQueueItem>,
        val urgentCount: Int,
        val needsReviewCount: Int,
    ) : RescueUiState
}

sealed interface RescueAction {
    data class MarkConsumed(val foodItemId: FoodItemId) : RescueAction
    data class UndoConsumed(val foodItemId: FoodItemId) : RescueAction
}

sealed interface RescueEvent {
    data class ShowConsumedUndo(
        val foodItemId: FoodItemId,
        val foodName: String,
    ) : RescueEvent
}
