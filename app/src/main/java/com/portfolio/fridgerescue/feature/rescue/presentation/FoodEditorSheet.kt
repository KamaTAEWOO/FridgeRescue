package com.portfolio.fridgerescue.feature.rescue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.domain.FoodItemDraftError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorSheet(
    state: FoodEditorUiState,
    onAction: (RescueAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(RescueAction.DismissEditor) },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.isEditing) R.string.editor_edit_title else R.string.editor_add_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(RescueAction.ChangeEditorName(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FoodEditorTestTags.NAME),
                label = { Text(stringResource(R.string.editor_name_label)) },
                placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
                singleLine = true,
                enabled = !state.isSaving,
            )

            OutlinedTextField(
                value = state.quantity,
                onValueChange = { onAction(RescueAction.ChangeEditorQuantity(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FoodEditorTestTags.QUANTITY),
                label = { Text(stringResource(R.string.editor_quantity_label)) },
                singleLine = true,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            OutlinedTextField(
                value = state.date,
                onValueChange = { onAction(RescueAction.ChangeEditorDate(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(FoodEditorTestTags.DATE),
                label = { Text(stringResource(R.string.editor_date_label)) },
                placeholder = { Text(stringResource(R.string.editor_date_placeholder)) },
                supportingText = { Text(stringResource(R.string.editor_date_supporting)) },
                singleLine = true,
                enabled = !state.isSaving,
            )

            Text(
                text = stringResource(R.string.editor_storage_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StorageLocation.entries.forEach { location ->
                    FilterChip(
                        selected = state.storageLocation == location,
                        onClick = { onAction(RescueAction.ChangeEditorStorage(location)) },
                        label = { Text(location.label()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(FoodEditorTestTags.storage(location)),
                        enabled = !state.isSaving,
                    )
                }
            }

            ToggleRow(
                label = stringResource(R.string.editor_opened_label),
                checked = state.isOpened,
                enabled = !state.isSaving,
                onCheckedChange = { onAction(RescueAction.ChangeEditorOpened(it)) },
            )
            ToggleRow(
                label = stringResource(R.string.editor_pinned_label),
                checked = state.isPinned,
                enabled = !state.isSaving,
                onCheckedChange = { onAction(RescueAction.ChangeEditorPinned(it)) },
            )

            state.error?.let { error ->
                Text(
                    text = error.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.editor_error_save),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onAction(RescueAction.DismissEditor) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.editor_cancel))
                }
                Button(
                    onClick = { onAction(RescueAction.SaveEditor) },
                    modifier = Modifier
                        .weight(2f)
                        .testTag(FoodEditorTestTags.SAVE),
                    enabled = !state.isSaving,
                ) {
                    Text(
                        stringResource(
                            if (state.isSaving) R.string.editor_saving else R.string.editor_save,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun StorageLocation.label(): String = when (this) {
    StorageLocation.REFRIGERATED -> stringResource(R.string.storage_refrigerated)
    StorageLocation.FROZEN -> stringResource(R.string.storage_frozen)
    StorageLocation.ROOM_TEMPERATURE -> stringResource(R.string.storage_room_temperature)
}

@Composable
private fun FoodItemDraftError.message(): String = when (this) {
    FoodItemDraftError.NAME_REQUIRED -> stringResource(R.string.editor_error_name)
    FoodItemDraftError.INVALID_QUANTITY -> stringResource(R.string.editor_error_quantity)
    FoodItemDraftError.INVALID_DATE -> stringResource(R.string.editor_error_date)
}

object FoodEditorTestTags {
    const val NAME = "food_editor_name"
    const val QUANTITY = "food_editor_quantity"
    const val DATE = "food_editor_date"
    const val SAVE = "food_editor_save"

    fun storage(location: StorageLocation): String = "food_editor_storage_${location.name}"
}
