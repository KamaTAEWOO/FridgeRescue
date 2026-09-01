package com.portfolio.fridgerescue.feature.intake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.IntakeCandidate
import com.portfolio.fridgerescue.core.model.IntakeCandidateGroup
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.IntakeErrorCode
import com.portfolio.fridgerescue.feature.rescue.presentation.IntakeReviewUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeDraftSheet(
    state: IntakeReviewUiState,
    onDismiss: () -> Unit,
    onCandidateSelected: (String, Boolean) -> Unit,
    onCandidateUpdated: (String, String, String) -> Unit,
    onSave: () -> Unit,
    onManualEntry: () -> Unit,
) {
    val draft = state.draft
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = when (draft.status) {
                    IntakeDraftStatus.PROCESSING -> stringResource(R.string.intake_processing_title)
                    IntakeDraftStatus.ERROR -> stringResource(R.string.intake_error_title)
                    else -> stringResource(R.string.intake_received_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = when (draft.status) {
                    IntakeDraftStatus.PROCESSING -> stringResource(R.string.intake_processing_description)
                    IntakeDraftStatus.ERROR -> draft.errorMessage()
                    else -> stringResource(R.string.intake_received_description, draft.typeLabel())
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (draft.status == IntakeDraftStatus.PROCESSING) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            if (draft.status == IntakeDraftStatus.READY &&
                draft.errorCode == IntakeErrorCode.OCR_PARTIAL
            ) {
                Text(
                    text = stringResource(R.string.intake_ocr_partial),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (draft.status == IntakeDraftStatus.READY && state.candidates.isNotEmpty()) {
                CandidateGroup(
                    title = stringResource(R.string.intake_group_manage),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.MANAGE },
                    duplicateCandidateIds = state.duplicateCandidateIds,
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
                    onCandidateUpdated = onCandidateUpdated,
                )
                CandidateGroup(
                    title = stringResource(R.string.intake_group_review),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.REVIEW },
                    duplicateCandidateIds = state.duplicateCandidateIds,
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
                    onCandidateUpdated = onCandidateUpdated,
                )
                CandidateGroup(
                    title = stringResource(R.string.intake_group_excluded),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.EXCLUDED },
                    duplicateCandidateIds = state.duplicateCandidateIds,
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
                    onCandidateUpdated = onCandidateUpdated,
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.selectedCount > 0 && !state.isSaving,
                ) {
                    Text(
                        if (state.isSaving) {
                            stringResource(R.string.intake_saving)
                        } else {
                            stringResource(R.string.intake_save_selected, state.selectedCount)
                        },
                    )
                }
            } else if (draft.status == IntakeDraftStatus.READY) {
                draft.textContent?.let { SharedTextPreview(it) }
                Text(
                    text = stringResource(R.string.intake_no_candidates),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (draft.status == IntakeDraftStatus.ERROR ||
                (draft.status == IntakeDraftStatus.READY && state.candidates.isEmpty())
            ) {
                Button(
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) { Text(stringResource(R.string.import_manual)) }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(
                    if (draft.status == IntakeDraftStatus.ERROR) {
                        stringResource(R.string.intake_close)
                    } else {
                        stringResource(R.string.intake_review_later)
                    },
                )
            }
        }
    }
}

@Composable
private fun CandidateGroup(
    title: String,
    candidates: List<IntakeCandidate>,
    duplicateCandidateIds: Set<String>,
    enabled: Boolean,
    onCandidateSelected: (String, Boolean) -> Unit,
    onCandidateUpdated: (String, String, String) -> Unit,
) {
    if (candidates.isEmpty()) return
    Text(
        text = stringResource(R.string.intake_group_count, title, candidates.size),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    candidates.forEach { candidate ->
        EditableCandidate(
            candidate = candidate,
            isDuplicate = candidate.id in duplicateCandidateIds,
            enabled = enabled,
            onCandidateSelected = onCandidateSelected,
            onCandidateUpdated = onCandidateUpdated,
        )
    }
}

object IntakeCandidateTestTags {
    fun edit(id: String) = "intake-candidate-edit-$id"
    fun name(id: String) = "intake-candidate-name-$id"
    fun quantity(id: String) = "intake-candidate-quantity-$id"
    fun save(id: String) = "intake-candidate-save-$id"
}

@Composable
private fun EditableCandidate(
    candidate: IntakeCandidate,
    isDuplicate: Boolean,
    enabled: Boolean,
    onCandidateSelected: (String, Boolean) -> Unit,
    onCandidateUpdated: (String, String, String) -> Unit,
) {
    var editing by rememberSaveable(candidate.id) { mutableStateOf(false) }
    var name by rememberSaveable(candidate.id) { mutableStateOf(candidate.normalizedName) }
    var quantity by rememberSaveable(candidate.id) {
        mutableStateOf(candidate.quantity?.toString().orEmpty())
    }
    val validName = name.isNotBlank()
    val validQuantity = quantity.isBlank() || quantity.toIntOrNull()?.let { it > 0 } == true

    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = candidate.isSelected,
                        onCheckedChange = { onCandidateSelected(candidate.id, it) },
                        enabled = enabled,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.normalizedName, fontWeight = FontWeight.SemiBold)
                        val metadata = listOfNotNull(
                            candidate.quantity?.let {
                                stringResource(R.string.food_quantity_format, it)
                            },
                            candidate.reason,
                            if (isDuplicate) stringResource(R.string.intake_duplicate_existing) else null,
                        ).joinToString(" · ")
                        if (metadata.isNotEmpty()) {
                            Text(
                                text = metadata,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(
                        onClick = { editing = !editing },
                        modifier = Modifier.testTag(IntakeCandidateTestTags.edit(candidate.id)),
                        enabled = enabled,
                    ) { Text(stringResource(if (editing) R.string.intake_edit_cancel else R.string.intake_edit)) }
                }
                if (editing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(IntakeCandidateTestTags.name(candidate.id)),
                        label = { Text(stringResource(R.string.intake_edit_name)) },
                        isError = !validName,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter(Char::isDigit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(IntakeCandidateTestTags.quantity(candidate.id)),
                        label = { Text(stringResource(R.string.intake_edit_quantity)) },
                        supportingText = if (validQuantity) null else {
                            { Text(stringResource(R.string.intake_edit_quantity_error)) }
                        },
                        isError = !validQuantity,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            onCandidateUpdated(candidate.id, name, quantity)
                            editing = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(IntakeCandidateTestTags.save(candidate.id)),
                        enabled = enabled && validName && validQuantity,
                    ) {
                        Text(stringResource(R.string.intake_edit_save))
                    }
                }
            }
        }
}

@Composable
private fun SharedTextPreview(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IntakeDraft.typeLabel(): String = stringResource(
    when (contentType) {
        IntakeContentType.TEXT -> R.string.intake_type_text
        IntakeContentType.IMAGE -> R.string.intake_type_image
        IntakeContentType.PDF -> R.string.intake_type_pdf
        null -> R.string.intake_type_unknown
    },
)

@Composable
private fun IntakeDraft.errorMessage(): String = stringResource(
    when (errorCode) {
        IntakeErrorCode.SHARED_URI_UNAVAILABLE -> R.string.intake_error_uri
        IntakeErrorCode.SHARED_FILE_TOO_LARGE -> R.string.intake_error_large
        IntakeErrorCode.SHARED_FILE_SIGNATURE_INVALID -> R.string.intake_error_signature
        IntakeErrorCode.SHARED_TEXT_EMPTY -> R.string.intake_error_empty_text
        IntakeErrorCode.OCR_NO_ITEMS -> R.string.intake_error_ocr_no_items
        IntakeErrorCode.OCR_PROCESSING_FAILED -> R.string.intake_error_ocr_failed
        else -> R.string.intake_error_unsupported
    },
)
