package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.FoodActionType
import com.portfolio.fridgerescue.core.model.FoodEventType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailSheet(state: FoodDetailUiState, onAction: (RescueAction) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onAction(RescueAction.DismissFoodActions) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(state.foodItem.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.action_sheet_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = stringResource(R.string.rescue_mark_consumed),
                    type = FoodActionType.CONSUME,
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = stringResource(R.string.action_still_here),
                    type = FoodActionType.STILL_HERE,
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                    outlined = true,
                )
            }
            ActionButton(
                label = stringResource(R.string.action_partially_used),
                type = FoodActionType.PARTIALLY_USE,
                state = state,
                onAction = onAction,
                modifier = Modifier.fillMaxWidth(),
                outlined = true,
            )
            OutlinedTextField(
                value = state.discardReason,
                onValueChange = { onAction(RescueAction.ChangeDiscardReason(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.action_discard_reason)) },
                supportingText = { Text(stringResource(R.string.action_discard_reason_optional)) },
                singleLine = true,
                enabled = !state.actionInProgress,
            )
            ActionButton(
                label = stringResource(R.string.action_discard),
                type = FoodActionType.DISCARD,
                state = state,
                onAction = onAction,
                modifier = Modifier.fillMaxWidth(),
                outlined = true,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.events.isEmpty()) {
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.events.take(10).forEach { event ->
                    Column {
                        Text(event.type.label(), fontWeight = FontWeight.SemiBold)
                        event.discardReason?.let {
                            Text(
                                stringResource(R.string.history_reason_format, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    type: FoodActionType,
    state: FoodDetailUiState,
    onAction: (RescueAction) -> Unit,
    modifier: Modifier,
    outlined: Boolean = false,
) {
    val click = { onAction(RescueAction.RecordFoodAction(state.foodItem.id, type)) }
    if (outlined) {
        OutlinedButton(onClick = click, modifier = modifier, enabled = !state.actionInProgress) { Text(label) }
    } else {
        Button(onClick = click, modifier = modifier, enabled = !state.actionInProgress) { Text(label) }
    }
}

@Composable
private fun FoodEventType.label(): String = stringResource(
    when (this) {
        FoodEventType.CREATED -> R.string.history_created
        FoodEventType.UPDATED -> R.string.history_updated
        FoodEventType.CONSUMED -> R.string.history_consumed
        FoodEventType.STILL_HERE -> R.string.history_still_here
        FoodEventType.PARTIALLY_USED -> R.string.history_partially_used
        FoodEventType.DISCARDED -> R.string.history_discarded
        FoodEventType.UNDONE -> R.string.history_undone
    },
)
