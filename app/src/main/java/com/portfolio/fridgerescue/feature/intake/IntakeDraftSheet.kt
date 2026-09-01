package com.portfolio.fridgerescue.feature.intake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.IntakeContentType
import com.portfolio.fridgerescue.core.model.IntakeDraft
import com.portfolio.fridgerescue.core.model.IntakeDraftStatus
import com.portfolio.fridgerescue.core.model.IntakeErrorCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeDraftSheet(draft: IntakeDraft, onDismiss: () -> Unit) {
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
            draft.textContent?.let { sharedText ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = sharedText,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (draft.status == IntakeDraftStatus.READY) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.intake_review_later)) }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.intake_close)) }
            }
        }
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
