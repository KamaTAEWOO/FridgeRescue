package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.domain.model.FoodActionType
import com.portfolio.fridgerescue.core.domain.model.FoodEventType

object FoodDetailTestTags {
    const val DIALOG = "food-detail-dialog"
}

/** 자주 쓰지 않는 상세 행동과 변경 이력을 화면 중앙 팝업에 모아 보여준다. */
@Composable
fun FoodDetailDialog(state: FoodDetailUiState, onAction: (RescueAction) -> Unit) {
    Dialog(
        onDismissRequest = { onAction(RescueAction.DismissFoodActions) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 560.dp)
                .heightIn(max = 760.dp)
                .imePadding()
                .testTag(FoodDetailTestTags.DIALOG),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        state.foodItem.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { onAction(RescueAction.StartEditFood(state.foodItem.id)) }) {
                        Text(stringResource(R.string.rescue_edit_food_details))
                    }
                    IconButton(onClick = { onAction(RescueAction.DismissFoodActions) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_dialog_close),
                        )
                    }
                }
                Text(
                    stringResource(R.string.action_dialog_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                Text(
                    stringResource(R.string.history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (state.events.isEmpty()) {
                    Text(
                        stringResource(R.string.history_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
