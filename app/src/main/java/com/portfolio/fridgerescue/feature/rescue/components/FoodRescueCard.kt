package com.portfolio.fridgerescue.feature.rescue.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
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
    onEdit: () -> Unit,
    onOpenActions: () -> Unit,
    onMarkConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasized = queueItem.urgency == RescueUrgency.OVERDUE ||
        queueItem.urgency == RescueUrgency.TODAY
    val containerColor = if (emphasized) {
        lerp(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer,
            0.62f,
        )
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetadataBadge(queueItem.foodItem.storageLocation.label())
                queueItem.foodItem.quantity?.let {
                    MetadataBadge(stringResource(R.string.food_quantity_format, it))
                }
                if (queueItem.foodItem.isOpened) {
                    MetadataBadge(stringResource(R.string.food_opened))
                }
                if (queueItem.foodItem.isPinned) {
                    MetadataBadge(stringResource(R.string.food_pinned))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.9f),
                ) {
                    Text(
                        text = stringResource(R.string.rescue_edit_food),
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedButton(
                    onClick = onOpenActions,
                    modifier = Modifier.weight(1.2f),
                ) {
                    Text(
                        text = stringResource(R.string.rescue_more_actions),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = onMarkConsumed,
                    modifier = Modifier.weight(1.4f),
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
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
private fun MetadataBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
