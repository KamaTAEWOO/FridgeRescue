package com.portfolio.fridgerescue.feature.rescue.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.core.model.FoodDateSource
import com.portfolio.fridgerescue.core.model.StorageLocation
import com.portfolio.fridgerescue.feature.rescue.domain.RescueQueueItem
import com.portfolio.fridgerescue.feature.rescue.domain.RescueUrgency

@Composable
fun FoodRescueCard(
    queueItem: RescueQueueItem,
    onOpenActions: () -> Unit,
    onMarkConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasized = queueItem.urgency == RescueUrgency.OVERDUE ||
        queueItem.urgency == RescueUrgency.TODAY
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = queueItem.foodItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = queueItem.dateDescription(),
                        modifier = Modifier.padding(top = 3.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                UrgencyBadge(queueItem)
            }

            Text(
                text = queueItem.metadataDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenActions,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.rescue_more_actions),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onMarkConsumed,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.rescue_mark_consumed),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgencyBadge(queueItem: RescueQueueItem) {
    val label = when (queueItem.urgency) {
        RescueUrgency.OVERDUE -> stringResource(
            R.string.date_days_overdue,
            -(queueItem.daysRemaining ?: 0),
        )
        RescueUrgency.TODAY -> stringResource(R.string.date_today)
        RescueUrgency.SOON,
        RescueUrgency.LATER,
        -> if (queueItem.daysRemaining == 1L) {
            stringResource(R.string.date_tomorrow)
        } else {
            stringResource(R.string.date_days_remaining, queueItem.daysRemaining ?: 0)
        }
        RescueUrgency.NEEDS_DATE -> stringResource(R.string.date_needs_review)
    }

    Surface(
        color = if (queueItem.urgency == RescueUrgency.OVERDUE || queueItem.urgency == RescueUrgency.TODAY) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RescueQueueItem.metadataDescription(): String {
    val storage = foodItem.storageLocation.label()
    val quantity = foodItem.quantity?.let { stringResource(R.string.food_quantity_format, it) }
    val opened = if (foodItem.isOpened) stringResource(R.string.food_opened) else null
    val pinned = if (foodItem.isPinned) stringResource(R.string.food_pinned) else null
    val labels = listOfNotNull(storage, quantity, opened, pinned)
    return labels.joinToString(separator = " · ")
}

@Composable
private fun RescueQueueItem.dateDescription(): String {
    val sourceLabel = when (effectiveDate?.source) {
        FoodDateSource.MANUFACTURER_DISPLAYED -> stringResource(R.string.date_source_manufacturer)
        FoodDateSource.APP_ESTIMATED -> stringResource(R.string.date_source_estimated)
        FoodDateSource.USER_CONFIRMED -> stringResource(R.string.date_source_confirmed)
        null -> return stringResource(R.string.date_needs_review)
    }
    val date = requireNotNull(effectiveDate).value
    return "$sourceLabel · ${date.monthValue}월 ${date.dayOfMonth}일"
}

@Composable
private fun StorageLocation.label(): String = when (this) {
    StorageLocation.REFRIGERATED -> stringResource(R.string.storage_refrigerated)
    StorageLocation.FROZEN -> stringResource(R.string.storage_frozen)
    StorageLocation.ROOM_TEMPERATURE -> stringResource(R.string.storage_room_temperature)
}
