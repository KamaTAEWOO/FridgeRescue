package com.portfolio.fridgerescue.feature.intake.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeOptionsSheet(
    onDismiss: () -> Unit,
    onCaptureReceipt: () -> Unit,
    onPickReceipt: () -> Unit,
    onScanBarcode: () -> Unit,
    onManualEntry: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.import_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.import_close),
                    )
                }
            }
            Text(
                text = stringResource(R.string.import_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OptionGroup(
                title = stringResource(R.string.import_group_receipt),
                firstTitle = stringResource(R.string.import_capture_receipt),
                firstDescription = stringResource(R.string.import_capture_receipt_hint),
                onFirstClick = onCaptureReceipt,
                secondTitle = stringResource(R.string.import_pick_receipt),
                secondDescription = stringResource(R.string.import_pick_receipt_hint),
                onSecondClick = onPickReceipt,
                modifier = Modifier.padding(top = 10.dp),
            )
            OptionGroup(
                title = stringResource(R.string.import_group_single),
                firstTitle = stringResource(R.string.import_scan_barcode),
                firstDescription = stringResource(R.string.import_scan_barcode_hint),
                onFirstClick = onScanBarcode,
                secondTitle = stringResource(R.string.import_manual),
                secondDescription = stringResource(R.string.import_manual_hint),
                onSecondClick = onManualEntry,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun OptionGroup(
    title: String,
    firstTitle: String,
    firstDescription: String,
    onFirstClick: () -> Unit,
    secondTitle: String,
    secondDescription: String,
    onSecondClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AddOptionCard(
                title = firstTitle,
                description = firstDescription,
                onClick = onFirstClick,
                modifier = Modifier.weight(1f),
            )
            AddOptionCard(
                title = secondTitle,
                description = secondDescription,
                onClick = onSecondClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddOptionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
