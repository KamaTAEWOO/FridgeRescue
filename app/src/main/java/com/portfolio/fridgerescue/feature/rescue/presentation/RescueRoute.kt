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

@Composable
fun RescueRoute(
    viewModel: RescueViewModel = viewModel(factory = RescueViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val consumedMessage = stringResource(R.string.rescue_consumed_message)
    val undoLabel = stringResource(R.string.rescue_undo)
    val addedMessage = stringResource(R.string.food_added_message)
    val updatedMessage = stringResource(R.string.food_updated_message)

    LaunchedEffect(viewModel, consumedMessage, undoLabel, addedMessage, updatedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                is RescueEvent.ShowConsumedUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = consumedMessage.format(event.foodName),
                        actionLabel = undoLabel,
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(RescueAction.UndoConsumed(event.foodItemId))
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
            }
        }
    }

    RescueScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
    )
}
