package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.FoodActionType

@Composable
fun RescueRoute(
    viewModel: RescueViewModel = viewModel(factory = RescueViewModel.Factory),
    onPickReceipt: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val consumedMessage = stringResource(R.string.action_consumed_message)
    val stillHereMessage = stringResource(R.string.action_still_here_message)
    val partiallyUsedMessage = stringResource(R.string.action_partially_used_message)
    val discardedMessage = stringResource(R.string.action_discarded_message)
    val undoLabel = stringResource(R.string.rescue_undo)
    val addedMessage = stringResource(R.string.food_added_message)
    val updatedMessage = stringResource(R.string.food_updated_message)
    val batchSavedMessage = stringResource(R.string.intake_batch_saved)

    LaunchedEffect(
        viewModel,
        consumedMessage,
        stillHereMessage,
        partiallyUsedMessage,
        discardedMessage,
        undoLabel,
        addedMessage,
        updatedMessage,
        batchSavedMessage,
    ) {
        viewModel.events.collect { event ->
            when (event) {
                is RescueEvent.ShowMutationUndo -> {
                    val message = when (event.actionType) {
                        FoodActionType.CONSUME -> consumedMessage
                        FoodActionType.STILL_HERE -> stillHereMessage
                        FoodActionType.PARTIALLY_USE -> partiallyUsedMessage
                        FoodActionType.DISCARD -> discardedMessage
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = message.format(event.foodName),
                        actionLabel = undoLabel,
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(RescueAction.UndoMutation(event.eventId))
                    }
                }
                is RescueEvent.ShowFoodSaved -> {
                    snackbarHostState.showSnackbar(
                        message = if (event.isNew) {
                            addedMessage.format(event.foodName)
                        } else {
                            updatedMessage.format(event.foodName)
                        },
                    )
                }
                is RescueEvent.ShowBatchSaved -> {
                    snackbarHostState.showSnackbar(batchSavedMessage.format(event.count))
                }
            }
        }
    }

    RescueScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onPickReceipt = onPickReceipt,
    )
}
