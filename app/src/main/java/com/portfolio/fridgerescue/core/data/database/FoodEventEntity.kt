package com.portfolio.fridgerescue.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_events",
    indices = [
        Index(value = ["operation_id"], unique = true),
        Index(value = ["food_item_id"]),
        Index(value = ["reverts_event_id"]),
    ],
)
data class FoodEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "operation_id")
    val operationId: String,
    @ColumnInfo(name = "food_item_id")
    val foodItemId: String,
    val type: String,
    @ColumnInfo(name = "previous_status")
    val previousStatus: String?,
    @ColumnInfo(name = "new_status")
    val newStatus: String?,
    @ColumnInfo(name = "discard_reason")
    val discardReason: String?,
    @ColumnInfo(name = "occurred_at_epoch_millis")
    val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "reverts_event_id")
    val revertsEventId: String?,
)
