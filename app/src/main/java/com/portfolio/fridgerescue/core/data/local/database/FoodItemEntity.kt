package com.portfolio.fridgerescue.core.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val quantity: Int?,
    @ColumnInfo(name = "storage_location")
    val storageLocation: String,
    @ColumnInfo(name = "manufacturer_displayed_date")
    val manufacturerDisplayedDate: String?,
    @ColumnInfo(name = "app_estimated_date")
    val appEstimatedDate: String?,
    @ColumnInfo(name = "user_confirmed_date")
    val userConfirmedDate: String?,
    @ColumnInfo(name = "is_opened")
    val isOpened: Boolean,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,
    val status: String,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)
