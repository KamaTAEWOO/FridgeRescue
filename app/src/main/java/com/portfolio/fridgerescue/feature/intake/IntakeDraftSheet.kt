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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                text = if (draft.status == IntakeDraftStatus.ERROR) {
                    stringResource(R.string.intake_error_title)
                } else {
                    stringResource(R.string.intake_received_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (draft.status == IntakeDraftStatus.ERROR) {
                    draft.errorMessage()
                } else {
                    stringResource(R.string.intake_received_description, draft.typeLabel())
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (draft.status == IntakeDraftStatus.READY && state.candidates.isNotEmpty()) {
                CandidateGroup(
                    title = stringResource(R.string.intake_group_manage),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.MANAGE },
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
                )
                CandidateGroup(
                    title = stringResource(R.string.intake_group_review),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.REVIEW },
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
                )
                CandidateGroup(
                    title = stringResource(R.string.intake_group_excluded),
                    candidates = state.candidates.filter { it.group == IntakeCandidateGroup.EXCLUDED },
                    enabled = !state.isSaving,
                    onCandidateSelected = onCandidateSelected,
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

            if (draft.status == IntakeDraftStatus.ERROR || state.candidates.isEmpty()) {
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
    enabled: Boolean,
    onCandidateSelected: (String, Boolean) -> Unit,
) {
    if (candidates.isEmpty()) return
    Text(
        text = stringResource(R.string.intake_group_count, title, candidates.size),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    candidates.forEach { candidate ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = candidate.isSelected,
                    onCheckedChange = { onCandidateSelected(candidate.id, it) },
                    enabled = enabled,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(candidate.normalizedName, fontWeight = FontWeight.SemiBold)
                    val metadata = listOfNotNull(
                        candidate.quantity?.let { stringResource(R.string.food_quantity_format, it) },
                        candidate.reason,
                    ).joinToString(" · ")
                    if (metadata.isNotEmpty()) {
                        Text(
                            text = metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
        else -> R.string.intake_error_unsupported
    },
)
