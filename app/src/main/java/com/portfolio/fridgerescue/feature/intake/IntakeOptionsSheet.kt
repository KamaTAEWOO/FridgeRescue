package com.portfolio.fridgerescue.feature.intake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeOptionsSheet(
    onDismiss: () -> Unit,
    onPickReceipt: () -> Unit,
    onManualEntry: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.import_share_guide),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPickReceipt, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_pick_receipt))
            }
            OutlinedButton(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.import_manual))
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.editor_cancel))
            }
        }
    }
}
